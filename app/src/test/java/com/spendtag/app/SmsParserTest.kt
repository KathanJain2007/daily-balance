package com.spendtag.app

import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stage 1 Test Suite: SMS Parser Engine
 * 
 * Tests written FIRST to specify the exact behavior for:
 * 1. Indian Bank UPI debits (HDFC, SBI, Kotak, PNB, etc.)
 * 2. Card spends and POS debits (ICICI, HDFC)
 * 3. Incoming credits (Friend transfers, refunds, salary)
 * 4. UTR and Reference number extraction
 * 5. Rejection of OTPs, promotional spam, and balance queries
 * 6. Edge cases: formatted numbers (commas, decimals, rupee symbols)
 */
class SmsParserTest {

    private val fixedTimestamp = 1728734400000L // 12 Oct 2024 17:20:00 GMT+5:30

    @Test
    fun parse_hdfcUpiDebit_extractsCorrectDetails() {
        val sms = "Sent Rs. 350.00 from HDFC Bank A/C **1234 to tea@okaxis on 12-10-24. Ref 428719283719. Not you? Call 18002586161."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "VM-HDFCBK")

        assertNotNull(result)
        result!!
        assertEquals(35000L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertEquals("tea@okaxis", result.upiId)
        assertEquals("428719283719", result.refNo)
    }

    @Test
    fun parse_sbiUpiDebit_extractsCorrectDetails() {
        val sms = "Dear UPI user A/C 9876 debited by 120.00 on 12Oct24 transfer to swiggy@icici Ref 428712345678. If not done by you, forward this SMS to 9223008333 to block UPI -SBI"
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "AX-SBIINB")

        assertNotNull(result)
        result!!
        assertEquals(12000L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertEquals("swiggy@icici", result.upiId)
        assertEquals("428712345678", result.refNo)
    }

    @Test
    fun parse_iciciCardDebit_extractsPayeeAndAmountWithComma() {
        val sms = "Dear Customer, your A/C ending 4567 has been debited for Rs 1,499.00 on 14-Oct-24 towards ZOMATO. Ref No 428812345678. Avl Bal is INR 24,500.00."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "BW-ICICIB")

        assertNotNull(result)
        result!!
        assertEquals(149900L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertNull(result.upiId)
        assertEquals("428812345678", result.refNo)
        assertEquals("ZOMATO", result.payeeName)
    }

    @Test
    fun parse_axisUpiCredit_extractsCreditDirectionAndSender() {
        val sms = "INR 1,500.00 credited to A/C ending 5678 on 15-10-24 by rahul@okhdfcbank UPI/CR/428912345678/Payment. Bal INR 12,300.00."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "VK-AXISBK")

        assertNotNull(result)
        result!!
        assertEquals(150000L, result.amountPaise)
        assertEquals(PaymentDirection.IN, result.direction)
        assertEquals("rahul@okhdfcbank", result.upiId)
        assertEquals("428912345678", result.refNo)
    }

    @Test
    fun parse_kotakUpiDebit_extractsCorrectFields() {
        val sms = "Sent Rs.45.00 from Kotak Bank AC X1234 to chaiwala@paytm on 12-10-2024. UPI Ref 428799887766. If not you, SMS BLOCK to 9971056767."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "JM-KOTAKB")

        assertNotNull(result)
        result!!
        assertEquals(4500L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertEquals("chaiwala@paytm", result.upiId)
        assertEquals("428799887766", result.refNo)
    }

    @Test
    fun parse_sbiCreditTransfer_extractsIncomingTransfer() {
        val sms = "Dear SBI User, A/C 9876 credited by Rs 500.00 on 15-Oct-24 by transfer from priya@oksbi Ref 428933445566. Bal Rs 15,200.00."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "AX-SBIINB")

        assertNotNull(result)
        result!!
        assertEquals(50000L, result.amountPaise)
        assertEquals(PaymentDirection.IN, result.direction)
        assertEquals("priya@oksbi", result.upiId)
        assertEquals("428933445566", result.refNo)
    }

    @Test
    fun parse_hdfcCreditCardSpend_extractsMerchantAndAmount() {
        val sms = "Alert: You've spent INR 3,250.00 on HDFC Bank Credit Card ending 9012 at RELIANCE RETAIL on 16-Oct-24. Avl Lmt: INR 1,45,000.00."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "VM-HDFCBK")

        assertNotNull(result)
        result!!
        assertEquals(325000L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertNull(result.upiId)
        assertEquals("RELIANCE RETAIL", result.payeeName)
    }

    @Test
    fun parse_paytmUpiAlert_withRupeeSymbolAndMerchantName() {
        val sms = "Paid ₹240 to Chai Point (chaipoint@icici) on 12-10-2024. UPI Ref: 428719283719."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "BP-PAYTMB")

        assertNotNull(result)
        result!!
        assertEquals(24000L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertEquals("chaipoint@icici", result.upiId)
        assertEquals("428719283719", result.refNo)
        assertEquals("Chai Point", result.payeeName)
    }

    @Test
    fun parse_pnbDebitWithDecimalPaise() {
        val sms = "Your A/C ending 0123 debited by INR 850.50 on 18-Oct-24 by UPI/DR/429012345678/blinkit@axl."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "CP-PNBSMS")

        assertNotNull(result)
        result!!
        assertEquals(85050L, result.amountPaise)
        assertEquals(PaymentDirection.OUT, result.direction)
        assertEquals("blinkit@axl", result.upiId)
        assertEquals("429012345678", result.refNo)
    }

    // =================================================================
    // REJECTION TESTS (Must return null)
    // =================================================================

    @Test
    fun parse_ignoresOtpMessages() {
        val sms = "Your OTP for transaction of INR 2,500.00 on HDFC Bank Card ending 1234 is 482910. Do not share OTP with anyone."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "VM-HDFCBK")
        assertNull("OTP message must be ignored", result)
    }

    @Test
    fun parse_ignoresOneTimePasswordVariations() {
        val sms = "492813 is your secret One Time Password to authorize payment of Rs 400.00 on Swiggy. Valid for 5 mins."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "TX-SWIGGY")
        assertNull("One Time Password must be ignored", result)
    }

    @Test
    fun parse_ignoresPromotionalAndLoanOffers() {
        val sms = "Congratulations! You are pre-approved for personal loan of Rs 5,00,000 with instant disbursal. Click here http://hdfc.co/loan to apply now."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "VM-HDFCBK")
        assertNull("Promotional pre-approved loan message must be ignored", result)
    }

    @Test
    fun parse_ignoresBalanceEnquiries() {
        val sms = "Your A/C balance for account ending 1234 is Rs 4,520.50 as on 12-Oct-24."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "AX-SBIINB")
        assertNull("Balance inquiry must be ignored", result)
    }

    @Test
    fun parse_ignoresKycAndServiceNotifications() {
        val sms = "Dear Customer, your KYC is due. Please visit nearest branch with Aadhaar to avoid account suspension."
        val result = SmsParser.parse(sms, fixedTimestamp, sender = "BW-ICICIB")
        assertNull("Non-transactional KYC message must be ignored", result)
    }

    @Test
    fun parse_ignoresBlankOrJunkMessages() {
        assertNull(SmsParser.parse("", fixedTimestamp))
        assertNull(SmsParser.parse("   ", fixedTimestamp))
        assertNull(SmsParser.parse("Hi, how are you doing today?", fixedTimestamp))
    }
}
