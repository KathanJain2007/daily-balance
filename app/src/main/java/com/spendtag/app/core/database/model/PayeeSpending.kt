package com.spendtag.app.core.database.model

/**
 * Spending aggregate for a top payee or merchant.
 */
data class PayeeSpending(
    val payeeName: String,
    val totalSpentPaise: Long,
    val paymentCount: Int
)
