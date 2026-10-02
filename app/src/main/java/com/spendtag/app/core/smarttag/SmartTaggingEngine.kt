package com.spendtag.app.core.smarttag

import com.spendtag.app.core.database.entity.CounterpartyEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.model.PaymentDirection
import java.util.Locale

/**
 * Smart Tagging and Heuristic Confidence Engine.
 * 
 * Rules:
 * 1. Confidence threshold >= 3 consecutive identical tags: Silently tags the transaction.
 * 2. Confidence 1 or 2: Suggests the learned category as the primary one-tap button in notifications.
 * 3. Cold-start (0 tags): Employs merchant/payee keyword heuristics to select the most likely category.
 * 4. Incoming transactions (direction == IN): Defaults to Income / Transfer.
 * 5. Reclassifying an expense immediately resets/adjusts the streak.
 */
object SmartTaggingEngine {

    const val CONFIDENCE_THRESHOLD = 3

    // Curated Category IDs matching CategoryPalette
    const val CATEGORY_FOOD = 1L
    const val CATEGORY_GROCERIES = 2L
    const val CATEGORY_TRAVEL = 3L
    const val CATEGORY_SHOPPING = 4L
    const val CATEGORY_ENTERTAINMENT = 5L
    const val CATEGORY_BILLS = 6L
    const val CATEGORY_HEALTH = 7L
    const val CATEGORY_PERSONAL_CARE = 8L
    const val CATEGORY_EDUCATION_WORK = 9L
    const val CATEGORY_RENT = 10L
    const val CATEGORY_INCOME = 11L
    const val CATEGORY_OTHER = 12L

    /**
     * Evaluates a payment against existing counterparty and tag history records.
     */
    fun evaluateWithHistory(
        payment: PaymentEntity,
        counterparty: CounterpartyEntity?,
        recentHistory: List<TagHistoryEntity>
    ): TagDecision {
        // 1. Incoming money logic
        if (payment.direction == PaymentDirection.IN) {
            return evaluateColdStartGuess(payment)
        }

        // 2. Identify target learned category
        val targetCategoryId = counterparty?.defaultCategoryId ?: recentHistory.firstOrNull()?.categoryId

        if (targetCategoryId != null && recentHistory.isNotEmpty()) {
            // Count consecutive tags matching the target category starting from the most recent
            var streak = 0
            for (entry in recentHistory) {
                if (entry.categoryId == targetCategoryId) {
                    streak++
                } else {
                    break // Streak interrupted by a different category choice
                }
            }

            if (streak >= CONFIDENCE_THRESHOLD) {
                return TagDecision.SilentTag(
                    categoryId = targetCategoryId,
                    confidenceScore = streak,
                    reason = "Learned rule confirmed $streak times consecutively"
                )
            } else {
                val secondGuess = if (targetCategoryId == CATEGORY_FOOD) CATEGORY_GROCERIES else CATEGORY_FOOD
                return TagDecision.AskUser(
                    bestGuessCategoryId = targetCategoryId,
                    secondGuessCategoryId = secondGuess,
                    confidenceScore = streak,
                    isLearned = true
                )
            }
        }

        // 3. Fallback to cold-start keyword heuristics
        return evaluateColdStartGuess(payment)
    }

    /**
     * Cold-start heuristic evaluation based on merchant name and UPI handle patterns.
     */
    fun evaluateColdStartGuess(payment: PaymentEntity): TagDecision.AskUser {
        if (payment.direction == PaymentDirection.IN) {
            return TagDecision.AskUser(
                bestGuessCategoryId = CATEGORY_INCOME,
                secondGuessCategoryId = CATEGORY_OTHER,
                confidenceScore = 0,
                isLearned = false
            )
        }

        val textToSearch = listOfNotNull(payment.note, payment.upiId)
            .joinToString(" ")
            .lowercase(Locale.ENGLISH)

        val bestGuess = when {
            containsAny(textToSearch, FOOD_KEYWORDS) -> CATEGORY_FOOD
            containsAny(textToSearch, GROCERY_KEYWORDS) -> CATEGORY_GROCERIES
            containsAny(textToSearch, TRAVEL_KEYWORDS) -> CATEGORY_TRAVEL
            containsAny(textToSearch, SHOPPING_KEYWORDS) -> CATEGORY_SHOPPING
            containsAny(textToSearch, ENTERTAINMENT_KEYWORDS) -> CATEGORY_ENTERTAINMENT
            containsAny(textToSearch, BILLS_KEYWORDS) -> CATEGORY_BILLS
            containsAny(textToSearch, HEALTH_KEYWORDS) -> CATEGORY_HEALTH
            containsAny(textToSearch, PERSONAL_CARE_KEYWORDS) -> CATEGORY_PERSONAL_CARE
            containsAny(textToSearch, RENT_KEYWORDS) -> CATEGORY_RENT
            else -> CATEGORY_FOOD
        }

        val secondGuess = when (bestGuess) {
            CATEGORY_FOOD -> CATEGORY_GROCERIES
            CATEGORY_GROCERIES -> CATEGORY_FOOD
            CATEGORY_TRAVEL -> CATEGORY_BILLS
            CATEGORY_SHOPPING -> CATEGORY_FOOD
            else -> CATEGORY_OTHER
        }

        return TagDecision.AskUser(
            bestGuessCategoryId = bestGuess,
            secondGuessCategoryId = secondGuess,
            confidenceScore = 0,
            isLearned = false
        )
    }

    private fun containsAny(text: String, keywords: Array<String>): Boolean {
        for (kw in keywords) {
            if (text.contains(kw)) return true
        }
        return false
    }

    private val FOOD_KEYWORDS = arrayOf(
        "swiggy", "zomato", "mcdonald", "starbucks", "chai", "cafe", "restaurant",
        "burger", "pizza", "subway", "kfc", "dominos", "biryani", "food", "eats",
        "kitchen", "bakery", "tea", "coffee", "dhaba", "canteen"
    )

    private val GROCERY_KEYWORDS = arrayOf(
        "zepto", "blinkit", "instamart", "bigbasket", "dmart", "spencer",
        "nature basket", "supermarket", "kirana", "provision", "milk", "dairy",
        "fruit", "vegetable", "grofer"
    )

    private val TRAVEL_KEYWORDS = arrayOf(
        "uber", "ola", "rapido", "metro", "dmrc", "bmrc", "irctc", "redbus",
        "yulu", "petrol", "fuel", "shell", "hpcl", "bpcl", "iocl", "toll",
        "fastag", "auto", "cab"
    )

    private val SHOPPING_KEYWORDS = arrayOf(
        "amazon", "flipkart", "myntra", "ajio", "meesho", "zara", "h&m",
        "decathlon", "nykaa", "uniqlo", "croma", "reliancedigital", "retail", "mall"
    )

    private val ENTERTAINMENT_KEYWORDS = arrayOf(
        "bookmyshow", "pvr", "inox", "netflix", "spotify", "hotstar",
        "prime video", "cinema", "movie", "gaming", "youtube", "steam"
    )

    private val BILLS_KEYWORDS = arrayOf(
        "bescom", "electricity", "airtel", "jio", "vi", "vodafone",
        "broadband", "wifi", "water", "gas", "indane", "bill", "recharge", "postpaid", "actcorp"
    )

    private val HEALTH_KEYWORDS = arrayOf(
        "apollo", "1mg", "pharmeasy", "medplus", "netmeds", "doctor", "clinic",
        "dental", "hospital", "pharmacy", "diagnostic", "lab", "medicine"
    )

    private val PERSONAL_CARE_KEYWORDS = arrayOf(
        "salon", "spa", "cult", "gym", "fitness", "haircut", "parlour"
    )

    private val RENT_KEYWORDS = arrayOf(
        "rent", "society", "maintenance", "nobroker", "nestaway"
    )
}
