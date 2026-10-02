package com.spendtag.app.core.database.model

/**
 * Historical spending data point for a single month.
 */
data class MonthlyTrendItem(
    val yearMonthString: String,
    val monthLabel: String,
    val totalSpentPaise: Long
)
