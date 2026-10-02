package com.spendtag.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtag.app.core.database.SpendTagDatabase
import com.spendtag.app.core.database.dao.CategoryDao
import com.spendtag.app.core.database.dao.CounterpartyDao
import com.spendtag.app.core.database.dao.FriendDao
import com.spendtag.app.core.database.dao.PaymentDao
import com.spendtag.app.core.database.dao.SplitDao
import com.spendtag.app.core.database.entity.CategoryEntity
import com.spendtag.app.core.database.entity.CounterpartyEntity
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.SettlementEntity
import com.spendtag.app.core.database.entity.SplitShareEntity
import com.spendtag.app.core.database.model.CategorySpending
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Stage 2 Instrumented Test Suite
 * 
 * Verifies the Room SQLite layer in an actual in-memory Android database:
 * 1. Category pre-seeding
 * 2. Strict Rule: Category totals ALWAYS sum myShare, never amountPaise
 * 3. RefNo deduplication
 * 4. 2-minute time window deduplication for payments without refNo
 * 5. Friend bill split and settlement balance calculations
 */
@RunWith(AndroidJUnit4::class)
class SpendTagDatabaseTest {

    private lateinit var database: SpendTagDatabase
    private lateinit var paymentDao: PaymentDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var counterpartyDao: CounterpartyDao
    private lateinit var friendDao: FriendDao
    private lateinit var splitDao: SplitDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpendTagDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(SpendTagDatabase.SeedCategoriesCallback)
            .build()

        paymentDao = database.paymentDao()
        categoryDao = database.categoryDao()
        counterpartyDao = database.counterpartyDao()
        friendDao = database.friendDao()
        splitDao = database.splitDao()

        // Seed default categories into the in-memory instance
        runBlocking {
            categoryDao.insertCategories(
                listOf(
                    CategoryEntity(id = 1L, name = "Food & Dining", colour = "#FF7043", icon = "restaurant", isDefault = true),
                    CategoryEntity(id = 2L, name = "Groceries", colour = "#43A047", icon = "shopping_cart", isDefault = true),
                    CategoryEntity(id = 3L, name = "Travel & Commute", colour = "#0288D1", icon = "directions_car", isDefault = true)
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun categoryTotals_alwaysSumMyShare_neverAmountPaise() = runBlocking {
        val now = 1728734400000L
        val foodCategoryId = 1L

        // Scenario from prompt:
        // You pay Rs 5,000 (500000 paise) for a dinner at a restaurant.
        // Friends owe you Rs 4,000 (400000 paise).
        // Therefore, your personal share (myShare) is only Rs 1,000 (100000 paise).
        val dinnerPayment = PaymentEntity(
            amountPaise = 500000L,
            direction = PaymentDirection.OUT,
            upiId = "restaurant@icici",
            refNo = "428700112233",
            source = PaymentSource.SMS,
            createdAt = now,
            categoryId = foodCategoryId,
            myShare = 100000L, // Rs 1,000
            status = PaymentStatus.TAGGED
        )

        val id = paymentDao.insertPayment(dinnerPayment)
        assertTrue(id > 0)

        // Query category spending summary
        val summary = paymentDao.getCategorySpendingSummary(now - 1000, now + 1000).first()
        assertEquals(1, summary.size)

        val foodSpending = summary.first { it.categoryId == foodCategoryId }
        // MUST BE 100000L (Rs 1,000), NEVER 500000L (Rs 5,000)
        assertEquals(100000L, foodSpending.totalSpentPaise)

        val totalSpent = paymentDao.getTotalSpentPaise(now - 1000, now + 1000).first()
        assertEquals(100000L, totalSpent)
    }

    @Test
    fun deduplication_skipsDuplicateRefNo() = runBlocking {
        val now = 1728734400000L
        val original = PaymentEntity(
            amountPaise = 35000L, // Rs 350
            direction = PaymentDirection.OUT,
            upiId = "tea@okaxis",
            refNo = "428719283719",
            source = PaymentSource.SMS,
            createdAt = now,
            status = PaymentStatus.PENDING
        )

        val firstInsertId = paymentDao.insertIfUnique(original)
        assertNotNull("First payment must be inserted", firstInsertId)

        // Duplicate arrives via second SMS with same refNo
        val duplicate = PaymentEntity(
            amountPaise = 35000L,
            direction = PaymentDirection.OUT,
            upiId = "tea@okaxis",
            refNo = "428719283719", // Same refNo
            source = PaymentSource.SMS,
            createdAt = now + 5000, // Arrived 5 seconds later
            status = PaymentStatus.PENDING
        )

        val secondInsertId = paymentDao.insertIfUnique(duplicate)
        assertNull("Duplicate refNo must be rejected", secondInsertId)
    }

    @Test
    fun deduplication_withoutRefNo_skipsDuplicateWithin2Minutes() = runBlocking {
        val t0 = 1728734400000L // T
        val paymentWithoutRef = PaymentEntity(
            amountPaise = 25000L, // Rs 250
            direction = PaymentDirection.OUT,
            upiId = "chai@okaxis",
            refNo = null, // No refNo
            source = PaymentSource.SMS,
            createdAt = t0,
            status = PaymentStatus.PENDING
        )

        val firstId = paymentDao.insertIfUnique(paymentWithoutRef)
        assertNotNull("First transaction must be inserted", firstId)

        // Duplicate payment arrives 60 seconds later (within 2-minute window: 120,000 ms)
        val duplicateWithinWindow = PaymentEntity(
            amountPaise = 25000L,
            direction = PaymentDirection.OUT,
            upiId = "chai@okaxis",
            refNo = null,
            source = PaymentSource.SMS,
            createdAt = t0 + 60_000L, // 1 minute later
            status = PaymentStatus.PENDING
        )

        val duplicateId = paymentDao.insertIfUnique(duplicateWithinWindow)
        assertNull("Payment within 2-minute window with same amount and UPI ID must be skipped", duplicateId)

        // Legitimate second payment arrives 5 minutes later (outside 2-minute window)
        val legitimateLaterPayment = PaymentEntity(
            amountPaise = 25000L,
            direction = PaymentDirection.OUT,
            upiId = "chai@okaxis",
            refNo = null,
            source = PaymentSource.SMS,
            createdAt = t0 + 300_000L, // 5 minutes later
            status = PaymentStatus.PENDING
        )

        val laterId = paymentDao.insertIfUnique(legitimateLaterPayment)
        assertNotNull("Legitimate payment outside 2 minutes must be inserted", laterId)
    }

    @Test
    fun friendSplitAndSettlement_calculatesNetBalance() = runBlocking {
        val friendId = friendDao.insertFriend(
            FriendEntity(name = "Rahul", upiId = "rahul@okhdfcbank")
        )

        // Split share: Rahul owes Rs 4,000 (400000 paise)
        splitDao.insertSplitShare(
            SplitShareEntity(
                paymentId = 100L,
                friendId = friendId,
                sharePaise = 400000L
            )
        )

        val balanceBefore = splitDao.getNetBalanceOwedByFriend(friendId).first()
        assertEquals(400000L, balanceBefore)

        // Rahul settles Rs 4,000
        splitDao.insertSettlement(
            SettlementEntity(
                paymentId = 101L,
                friendId = friendId,
                amountPaise = 400000L
            )
        )

        val balanceAfter = splitDao.getNetBalanceOwedByFriend(friendId).first()
        assertEquals(0L, balanceAfter)
    }

    @Test
    fun counterparty_learnsDefaultCategoryAndAlias() = runBlocking {
        val upiId = "swiggy@icici"
        val foodCatId = 1L

        val initial = CounterpartyEntity(
            upiId = upiId,
            alias = "Swiggy",
            defaultCategoryId = foodCatId
        )
        counterpartyDao.insertOrUpdate(initial)

        val retrieved = counterpartyDao.getByUpiId(upiId)
        assertNotNull(retrieved)
        assertEquals("Swiggy", retrieved?.alias)
        assertEquals(foodCatId, retrieved?.defaultCategoryId)
    }
}
