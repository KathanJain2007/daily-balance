package com.spendtag.app.core.model

import com.spendtag.app.ui.theme.CategoryDefinition
import com.spendtag.app.ui.theme.CategoryPalette

/**
 * UI presentation model for a payment transaction.
 */
data class PaymentUiModel(
    val id: Long,
    val amountPaise: Long,
    val mySharePaise: Long,
    val direction: PaymentDirection,
    val upiId: String?,
    val refNo: String?,
    val source: PaymentSource,
    val createdAt: Long,
    val categoryId: Long?,
    val category: CategoryDefinition,
    val status: PaymentStatus,
    val payeeName: String,
    val note: String?,
    val isSplit: Boolean = mySharePaise != amountPaise
)
