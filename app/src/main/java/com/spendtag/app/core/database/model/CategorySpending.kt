package com.spendtag.app.core.database.model

/**
 * Data class representing category spending aggregation.
 * 
 * Strict Data Rule:
 * totalSpentPaise ALWAYS sums myShare, never amountPaise.
 */
data class CategorySpending(
    val categoryId: Long,
    val totalSpentPaise: Long,
    val paymentCount: Int
)
