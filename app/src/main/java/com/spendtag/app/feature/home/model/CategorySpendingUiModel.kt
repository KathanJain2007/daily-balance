package com.spendtag.app.feature.home.model

import com.spendtag.app.ui.theme.CategoryDefinition

/**
 * UI presentation model for category spending segments in donut charts and breakdowns.
 */
data class CategorySpendingUiModel(
    val category: CategoryDefinition,
    val totalSpentPaise: Long,
    val paymentCount: Int,
    val percentage: Float
)
