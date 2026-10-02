package com.spendtag.app

import com.spendtag.app.core.database.entity.CounterpartyEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.smarttag.SmartTaggingEngine
import com.spendtag.app.core.smarttag.TagDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying smart learning, confidence heuristics, and silent auto-tagging.
 */
class SmartTaggingEngineTest {

    private fun createTestPayment(
        upiId: String?,
        note: String?,
        direction: PaymentDirection = PaymentDirection.OUT
    ): PaymentEntity {
        return PaymentEntity(
            id = 100L,
            amountPaise = 25000L, // Rs 250
            direction = direction,
            upiId = upiId,
            refNo = "REF123456",
            source = PaymentSource.SMS,
            createdAt = System.currentTimeMillis(),
            categoryId = null,
            myShare = 25000L,
            status = PaymentStatus.PENDING,
            note = note
        )
    }

    @Test
    fun coldStart_guessesCategoryFromMerchantKeywords() {
        val swiggyPayment = createTestPayment("swiggy@icici", "Swiggy Order")
        val swiggyDecision = SmartTaggingEngine.evaluateColdStartGuess(swiggyPayment)
        assertEquals(1L, swiggyDecision.bestGuessCategoryId) // Food & Dining

        val uberPayment = createTestPayment("uber@hdfc", "Uber Trip")
        val uberDecision = SmartTaggingEngine.evaluateColdStartGuess(uberPayment)
        assertEquals(3L, uberDecision.bestGuessCategoryId) // Transport

        val zeptoPayment = createTestPayment("zepto@axis", "Zepto Grocery")
        val zeptoDecision = SmartTaggingEngine.evaluateColdStartGuess(zeptoPayment)
        assertEquals(2L, zeptoDecision.bestGuessCategoryId) // Groceries

        val netflixPayment = createTestPayment("netflix@kotak", "Netflix Subscription")
        val netflixDecision = SmartTaggingEngine.evaluateColdStartGuess(netflixPayment)
        assertEquals(5L, netflixDecision.bestGuessCategoryId) // Entertainment
    }

    @Test
    fun confidenceBelowThree_asksUserWithLearnedFirstChoice() {
        val upiId = "chaiwala@okaxis"
        val payment = createTestPayment(upiId, "Chai Corner")
        val counterparty = CounterpartyEntity(
            upiId = upiId,
            alias = "Chai Corner",
            defaultCategoryId = 1L
        )

        // Only 2 consecutive tags
        val history = listOf(
            TagHistoryEntity(id = 1L, paymentId = 1L, upiId = upiId, categoryId = 1L, taggedAt = 1000L),
            TagHistoryEntity(id = 2L, paymentId = 2L, upiId = upiId, categoryId = 1L, taggedAt = 2000L)
        )

        val decision = SmartTaggingEngine.evaluateWithHistory(payment, counterparty, history)

        assertTrue(decision is TagDecision.AskUser)
        val askUser = decision as TagDecision.AskUser
        assertEquals(1L, askUser.bestGuessCategoryId) // Learns Food
        assertEquals(2, askUser.confidenceScore)
        assertTrue(askUser.isLearned)
    }

    @Test
    fun confidenceThreeOrMore_triggersSilentTag() {
        val upiId = "metro@dmrc"
        val payment = createTestPayment(upiId, "DMRC Metro")
        val counterparty = CounterpartyEntity(
            upiId = upiId,
            alias = "DMRC Metro",
            defaultCategoryId = 3L // Transport
        )

        // 3 consecutive tags for Transport
        val history = listOf(
            TagHistoryEntity(id = 3L, paymentId = 3L, upiId = upiId, categoryId = 3L, taggedAt = 3000L),
            TagHistoryEntity(id = 2L, paymentId = 2L, upiId = upiId, categoryId = 3L, taggedAt = 2000L),
            TagHistoryEntity(id = 1L, paymentId = 1L, upiId = upiId, categoryId = 3L, taggedAt = 1000L)
        )

        val decision = SmartTaggingEngine.evaluateWithHistory(payment, counterparty, history)

        assertTrue(decision is TagDecision.SilentTag)
        val silentTag = decision as TagDecision.SilentTag
        assertEquals(3L, silentTag.categoryId)
        assertEquals(3, silentTag.confidenceScore)
    }

    @Test
    fun categoryCorrection_breaksConsecutiveStreak() {
        val upiId = "store@upi"
        val payment = createTestPayment(upiId, "General Store")
        val counterparty = CounterpartyEntity(
            upiId = upiId,
            alias = "General Store",
            defaultCategoryId = 4L // Switched to Shopping
        )

        // History: was Food (1L) 3 times, but most recently tagged as Shopping (4L)
        val history = listOf(
            TagHistoryEntity(id = 4L, paymentId = 4L, upiId = upiId, categoryId = 4L, taggedAt = 4000L), // Recent
            TagHistoryEntity(id = 3L, paymentId = 3L, upiId = upiId, categoryId = 1L, taggedAt = 3000L),
            TagHistoryEntity(id = 2L, paymentId = 2L, upiId = upiId, categoryId = 1L, taggedAt = 2000L),
            TagHistoryEntity(id = 1L, paymentId = 1L, upiId = upiId, categoryId = 1L, taggedAt = 1000L)
        )

        val decision = SmartTaggingEngine.evaluateWithHistory(payment, counterparty, history)

        // Streak for category 4L is only 1! Should ask user, NOT silently tag.
        assertTrue(decision is TagDecision.AskUser)
        val askUser = decision as TagDecision.AskUser
        assertEquals(4L, askUser.bestGuessCategoryId)
        assertEquals(1, askUser.confidenceScore)
    }

    @Test
    fun incomingPayment_routesToIncomeTransfer() {
        val salaryPayment = createTestPayment(null, "Salary Credit", direction = PaymentDirection.IN)
        val decision = SmartTaggingEngine.evaluateColdStartGuess(salaryPayment)

        assertEquals(11L, decision.bestGuessCategoryId) // Income / Transfer
    }
}
