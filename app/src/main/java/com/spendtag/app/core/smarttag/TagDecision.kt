package com.spendtag.app.core.smarttag

/**
 * Result of the Smart Tagging Engine's evaluation of a payment.
 */
sealed class TagDecision {

    /**
     * Confidence threshold (>= 3 consecutive identical tags) has been met.
     * Transaction is silently categorized without bothering the user.
     */
    data class SilentTag(
        val categoryId: Long,
        val confidenceScore: Int,
        val reason: String = "High confidence learned rule (streak >= 3)"
    ) : TagDecision()

    /**
     * Confidence is insufficient (< 3) or it is a cold-start payment.
     * A Quick-Tag Notification is shown with ranked category suggestions.
     */
    data class AskUser(
        val bestGuessCategoryId: Long,
        val secondGuessCategoryId: Long,
        val confidenceScore: Int,
        val isLearned: Boolean
    ) : TagDecision()
}
