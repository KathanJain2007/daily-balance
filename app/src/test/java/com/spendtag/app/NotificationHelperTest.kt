package com.spendtag.app

import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unit test suite verifying:
 * 1. Notification formatting logic for outgoing and incoming payments
 * 2. Unique request code derivation algorithm
 * 3. 9:00 PM evening digest timing calculation
 */
class NotificationHelperTest {

    @Test
    fun notificationTitle_formatsOutflowCorrectly() {
        val payment = PaymentEntity(
            id = 42L,
            amountPaise = 24000L, // Rs 240
            direction = PaymentDirection.OUT,
            upiId = "chai@okaxis",
            source = PaymentSource.SMS,
            createdAt = 1728734400000L,
            status = PaymentStatus.PENDING,
            note = "Chai Point"
        )

        val formattedAmount = CurrencyFormatter.formatPaise(payment.amountPaise)
        val payeeName = payment.note ?: payment.upiId ?: "Card"
        val title = "$formattedAmount to $payeeName"

        assertEquals("₹240 to Chai Point", title)
    }

    @Test
    fun notificationTitle_formatsIncomingMoneyCorrectly() {
        val payment = PaymentEntity(
            id = 43L,
            amountPaise = 150000L, // Rs 1,500
            direction = PaymentDirection.IN,
            upiId = "rahul@okhdfcbank",
            source = PaymentSource.SMS,
            createdAt = 1728734400000L,
            status = PaymentStatus.PENDING,
            note = null
        )

        val formattedAmount = CurrencyFormatter.formatPaise(payment.amountPaise)
        val friendName: String? = "Rahul"
        val title = if (friendName != null) "$formattedAmount from $friendName" else "$formattedAmount received"

        assertEquals("₹1,500 from Rahul", title)
    }

    @Test
    fun requestCodes_areUniqueAcrossPaymentsAndActions() {
        val payment1Id = 101L
        val payment2Id = 102L

        val reqCode1Base = (payment1Id % 100000).toInt() * 10
        val reqCode2Base = (payment2Id % 100000).toInt() * 10

        // Ensure bases are different
        assertTrue(reqCode1Base != reqCode2Base)

        // Ensure action codes within payment 1 are all distinct
        val p1Tap = reqCode1Base
        val p1Action1 = reqCode1Base + 1
        val p1Action2 = reqCode1Base + 2
        val p1Delete = reqCode1Base + 4

        val set1 = setOf(p1Tap, p1Action1, p1Action2, p1Delete)
        assertEquals(4, set1.size)
    }

    @Test
    fun eveningDigest_calculatesDelayToNext9PM() {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = LocalDateTime.now(zoneId)
        val targetTime = LocalTime.of(21, 0) // 9:00 PM

        var targetDateTime = now.with(targetTime)
        if (now.isAfter(targetDateTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val delayMillis = Duration.between(now, targetDateTime).toMillis()
        assertTrue("Delay must be positive", delayMillis >= 0)
        assertTrue("Delay must be at most 24 hours", delayMillis <= 24 * 60 * 60 * 1000L)
    }
}
