package com.spendtag.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.database.dao.SplitDao
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.data.repository.PaymentRepository
import com.spendtag.app.feature.home.model.CategorySpendingUiModel
import com.spendtag.app.ui.theme.CategoryPalette
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val paymentRepository: PaymentRepository,
    private val splitDao: SplitDao
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(YearMonth.now(DateTimeUtils.IndianZoneId))
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    // 1. Total spent this month (Strictly sums myShare)
    val totalSpentThisMonth: StateFlow<Long> = _currentYearMonth.flatMapLatest { ym ->
        val startMillis = DateTimeUtils.getStartOfMonthMillis(ym)
        val endMillis = DateTimeUtils.getEndOfMonthMillis(ym)
        paymentRepository.getTotalSpentPaise(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // 2. Total spent last month
    val totalSpentLastMonth: StateFlow<Long> = _currentYearMonth.flatMapLatest { ym ->
        val lastYm = ym.minusMonths(1)
        val startMillis = DateTimeUtils.getStartOfMonthMillis(lastYm)
        val endMillis = DateTimeUtils.getEndOfMonthMillis(lastYm)
        paymentRepository.getTotalSpentPaise(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // 3. Month-over-Month percentage change
    val monthOverMonthPercentChange: StateFlow<Int?> = combine(
        totalSpentThisMonth,
        totalSpentLastMonth
    ) { current, last ->
        if (last == 0L) null
        else {
            val change = ((current - last).toDouble() / last.toDouble()) * 100
            change.toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 4. Category Spending Breakdown for Donut Chart
    val categoryBreakdown: StateFlow<List<CategorySpendingUiModel>> = combine(
        _currentYearMonth.flatMapLatest { ym ->
            val startMillis = DateTimeUtils.getStartOfMonthMillis(ym)
            val endMillis = DateTimeUtils.getEndOfMonthMillis(ym)
            paymentRepository.getCategorySpendingSummary(startMillis, endMillis)
        },
        totalSpentThisMonth
    ) { summaries, totalSpent ->
        if (totalSpent == 0L) emptyList()
        else {
            summaries.map { summary ->
                val category = CategoryPalette.getCategory(summary.categoryId)
                val percentage = (summary.totalSpentPaise.toFloat() / totalSpent.toFloat()) * 100f
                CategorySpendingUiModel(
                    category = category,
                    totalSpentPaise = summary.totalSpentPaise,
                    paymentCount = summary.paymentCount,
                    percentage = percentage
                )
            }.sortedByDescending { it.totalSpentPaise }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Pending Payments Count
    val pendingCount: StateFlow<Int> = paymentRepository.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 6. Last 5 Payments
    val recentPayments: StateFlow<List<PaymentUiModel>> = paymentRepository.getRecentPayments(5)
        .map { entities ->
            entities.map { entity ->
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Total Owed across all friends
    val totalOwedAcrossFriends: StateFlow<Long> = splitDao.getTotalOwedAcrossAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun previousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }
}
