package com.spendtag.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.model.CategorySpending
import com.spendtag.app.core.database.model.PayeeSpending
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT COUNT(*) FROM payments WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM payments ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentPayments(limit: Int = 5): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getPaymentsBetween(startTime: Long, endTime: Long): Flow<List<PaymentEntity>>

    // =================================================================
    // DEDUPLICATION QUERIES
    // =================================================================

    @Query("SELECT * FROM payments WHERE refNo IS NOT NULL AND refNo = :refNo LIMIT 1")
    suspend fun getByRefNo(refNo: String): PaymentEntity?

    @Query("""
        SELECT * FROM payments 
        WHERE amountPaise = :amountPaise 
          AND direction = :direction 
          AND ((upiId IS NULL AND :upiId IS NULL) OR upiId = :upiId)
          AND createdAt >= :minTime 
          AND createdAt <= :maxTime 
        LIMIT 1
    """)
    suspend fun findDuplicateWithoutRef(
        amountPaise: Long,
        direction: PaymentDirection,
        upiId: String?,
        minTime: Long,
        maxTime: Long
    ): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: PaymentEntity): Long

    /**
     * Deduplicated Insertion Logic:
     * 1. If refNo is present and matches an existing record -> skip (returns null).
     * 2. If refNo is missing, checks for any payment with identical amountPaise, direction,
     *    and upiId within a 2-minute (120,000 ms) window -> skip (returns null).
     * 3. Otherwise, inserts into Room and returns the new payment ID.
     */
    @Transaction
    suspend fun insertIfUnique(payment: PaymentEntity): Long? {
        val ref = payment.refNo?.trim()
        if (!ref.isNullOrBlank()) {
            val existingByRef = getByRefNo(ref)
            if (existingByRef != null) {
                return null // Duplicate detected by unique reference number
            }
        } else {
            // Deduplicate across a 2-minute (120,000 ms) window
            val windowMillis = 120_000L
            val minTime = payment.createdAt - windowMillis
            val maxTime = payment.createdAt + windowMillis
            val existingInWindow = findDuplicateWithoutRef(
                amountPaise = payment.amountPaise,
                direction = payment.direction,
                upiId = payment.upiId,
                minTime = minTime,
                maxTime = maxTime
            )
            if (existingInWindow != null) {
                return null // Duplicate detected within 2-minute window
            }
        }

        return insertPayment(payment)
    }

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET categoryId = :categoryId, status = :status WHERE id = :paymentId")
    suspend fun updateCategory(paymentId: Long, categoryId: Long?, status: PaymentStatus)

    @Query("UPDATE payments SET myShare = :myShare WHERE id = :paymentId")
    suspend fun updateMyShare(paymentId: Long, myShare: Long)

    // =================================================================
    // STRICT DATA RULE: CATEGORY TOTALS ALWAYS SUM myShare
    // =================================================================

    @Query("""
        SELECT 
            categoryId, 
            SUM(myShare) AS totalSpentPaise, 
            COUNT(id) AS paymentCount 
        FROM payments 
        WHERE direction = 'OUT' 
          AND createdAt >= :startTime 
          AND createdAt <= :endTime 
          AND categoryId IS NOT NULL 
        GROUP BY categoryId
    """)
    fun getCategorySpendingSummary(startTime: Long, endTime: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT COALESCE(SUM(myShare), 0) 
        FROM payments 
        WHERE direction = 'OUT' 
          AND createdAt >= :startTime 
          AND createdAt <= :endTime
    """)
    fun getTotalSpentPaise(startTime: Long, endTime: Long): Flow<Long>

    @Query("""
        SELECT 
            COALESCE(note, upiId, 'Other') AS payeeName, 
            SUM(myShare) AS totalSpentPaise, 
            COUNT(id) AS paymentCount 
        FROM payments 
        WHERE direction = 'OUT' 
          AND createdAt >= :startTime 
          AND createdAt <= :endTime 
        GROUP BY payeeName 
        ORDER BY totalSpentPaise DESC 
        LIMIT :limit
    """)
    fun getTopPayees(startTime: Long, endTime: Long, limit: Int = 5): Flow<List<PayeeSpending>>
}
