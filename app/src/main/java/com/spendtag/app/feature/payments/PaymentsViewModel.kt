package com.spendtag.app.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.data.repository.PaymentRepository
import com.spendtag.app.ui.theme.CategoryPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaymentsViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _directionFilter = MutableStateFlow<PaymentDirection?>(null)
    val directionFilter: StateFlow<PaymentDirection?> = _directionFilter.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val groupedPayments: StateFlow<Map<String, List<PaymentUiModel>>> = combine(
        paymentRepository.getAllPayments(),
        _searchQuery,
        _directionFilter,
        _selectedCategoryId
    ) { payments, query, direction, catId ->
        val filtered = payments.filter { payment ->
            val matchesQuery = query.isBlank() ||
                    (payment.note?.contains(query, ignoreCase = true) == true) ||
                    (payment.upiId?.contains(query, ignoreCase = true) == true) ||
                    (payment.refNo?.contains(query, ignoreCase = true) == true) ||
                    CurrencyFormatter.formatPaise(payment.amountPaise).contains(query)

            val matchesDirection = direction == null || payment.direction == direction
            val matchesCategory = catId == null || payment.categoryId == catId

            matchesQuery && matchesDirection && matchesCategory
        }.map { entity ->
            PaymentUiModel(
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
        }

        // Group by Sticky Day Header (e.g. "Today", "Yesterday", "12 October 2026")
        filtered.groupBy { DateTimeUtils.formatDayHeader(it.createdAt) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDirectionFilter(direction: PaymentDirection?) {
        _directionFilter.value = if (_directionFilter.value == direction) null else direction
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }
}
