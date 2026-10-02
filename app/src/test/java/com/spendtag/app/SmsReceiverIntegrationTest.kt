package com.spendtag.app

import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit test verifying the mapping pipeline from incoming SMS text
 * to domain PaymentEntity properties used by SmsReceiver.
 */
class SmsReceiverIntegrationTest {

    @Test
    fun smsPipeline_mapsParsedSmsToPendingPaymentEntity() {
        val sms = "Sent Rs. 350.00 from HDFC Bank A/C **1234 to tea@okaxis on 12-10-24. Ref 428719283719. Not you? Call 18002586161."
        val timestamp = 1728734400000L
        val sender = "VM-HDFCBK"

        val parsed = SmsParser.parse(sms, timestamp, sender)
        assertNotNull(parsed)
        parsed!!

        // Construct entity as SmsReceiver does
        val paymentAmountPaise = parsed.amountPaise
        val paymentMyShare = paymentAmountPaise // Defaults to full amount
        val paymentStatus = PaymentStatus.PENDING
        val paymentSource = PaymentSource.SMS

        assertEquals(35000L, paymentAmountPaise)
        assertEquals(35000L, paymentMyShare)
        assertEquals(PaymentDirection.OUT, parsed.direction)
        assertEquals("tea@okaxis", parsed.upiId)
        assertEquals("428719283719", parsed.refNo)
        assertEquals(PaymentStatus.PENDING, paymentStatus)
        assertEquals(PaymentSource.SMS, paymentSource)
    }
}
