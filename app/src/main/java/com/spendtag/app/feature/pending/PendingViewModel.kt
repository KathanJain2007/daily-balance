package com.spendtag.app.feature.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.database.SpendTagDatabase
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.data.repository.PaymentRepository
import com.spendtag.app.ui.theme.CategoryDefinition
import com.spendtag.app.ui.theme.CategoryPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingCardUiModel(
    val payment: PaymentUiModel,
    val bestGuessCategory: CategoryDefinition
)

class PendingViewModel(
    private val paymentRepository: PaymentRepository,
    private val database: SpendTagDatabase
) : ViewModel() {

    val pendingCards: StateFlow<List<PendingCardUiModel>> = paymentRepository.getPendingPayments()
        .map { list ->
            list.map { entity ->
                // Check if counterparty has a learned default category
                val defaultCatId = if (entity.upiId != null) {
                    database.counterpartyDao().getByUpiId(entity.upiId)?.defaultCategoryId
                } else null

                val resolvedCatId = defaultCatId ?: com.spendtag.app.core.smarttag.SmartTaggingEngine.evaluateColdStartGuess(entity).bestGuessCategoryId
                val bestGuess = CategoryPalette.getCategory(resolvedCatId)

                val uiModel = PaymentUiModel(
                    id = entity.id,
                    amountPaise = entity.amountPaise,
                    mySharePaise = entity.myShare,
                    direction = entity.direction,
                    upiId = entity.upiId,
                    refNo = entity.refNo,
                    source = entity.source,
                    createdAt = entity.createdAt,
                    categoryId = entity.categoryId,
                    category = CategoryPalette.getCategory(entity.categoryId),
                    status = entity.status,
                    payeeName = entity.note ?: entity.upiId ?: "Payment",
                    note = entity.note
                )
                PendingCardUiModel(payment = uiModel, bestGuessCategory = bestGuess)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tagPayment(paymentId: Long, categoryId: Long, upiId: String?) {
        viewModelScope.launch {
            paymentRepository.tagPayment(paymentId, categoryId)
            val payment = paymentRepository.getPaymentById(paymentId)
            if (upiId != null) {
                database.counterpartyDao().recordCategoryChoice(
                    upiId = upiId,
                    categoryId = categoryId,
                    fallbackAlias = payment?.note ?: upiId
                )
            }
            database.tagHistoryDao().insertTagHistory(
                TagHistoryEntity(
                    paymentId = paymentId,
                    upiId = upiId,
                    categoryId = categoryId,
                    isSilent = false
                )
            )
        }
    }

    fun acceptGuess(paymentId: Long, bestGuessCategory: CategoryDefinition, upiId: String?) {
        tagPayment(paymentId, bestGuessCategory.id, upiId)
    }

    fun skipPayment(paymentId: Long) {
        viewModelScope.launch {
            paymentRepository.skipPayment(paymentId)
        }
    }
}
