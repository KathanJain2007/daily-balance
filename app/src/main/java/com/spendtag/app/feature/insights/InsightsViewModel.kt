package com.spendtag.app.feature.insights

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.database.model.MonthlyTrendItem
import com.spendtag.app.core.database.model.PayeeSpending
import com.spendtag.app.core.export.CsvExporter
import com.spendtag.app.data.repository.PaymentRepository
import com.spendtag.app.feature.home.model.CategorySpendingUiModel
import com.spendtag.app.ui.theme.CategoryPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class InsightsViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val currentMonth = YearMonth.now(DateTimeUtils.IndianZoneId)
    private val startOfMonth = DateTimeUtils.getStartOfMonthMillis(currentMonth)
    private val endOfMonth = DateTimeUtils.getEndOfMonthMillis(currentMonth)

    // User's monthly spending budget (defaults to ₹30,000 = 3,000,000 paise)
    private val _monthlyBudgetPaise = MutableStateFlow(3000000L)
    val monthlyBudgetPaise: StateFlow<Long> = _monthlyBudgetPaise.asStateFlow()

    // 1. Total spent this month
    val totalSpentThisMonth: StateFlow<Long> = paymentRepository.getTotalSpentPaise(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // 2. Category totals with percentages for horizontal progress bars
    val categoryTotals: StateFlow<List<CategorySpendingUiModel>> = combine(
        paymentRepository.getCategorySpendingSummary(startOfMonth, endOfMonth),
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

    // 3. Top 5 Payees / Merchants
    val topPayees: StateFlow<List<PayeeSpending>> = paymentRepository.getTopPayees(startOfMonth, endOfMonth, 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Six-Month Trend Line Data
    val sixMonthTrend: StateFlow<List<MonthlyTrendItem>> = paymentRepository.getAllPayments().map { allPayments ->
        val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
        val trendList = mutableListOf<MonthlyTrendItem>()

        // Generate past 6 months
        for (i in 5 downTo 0) {
            val ym = currentMonth.minusMonths(i.toLong())
            val start = DateTimeUtils.getStartOfMonthMillis(ym)
            val end = DateTimeUtils.getEndOfMonthMillis(ym)

            val monthSpend = allPayments
                .filter { it.direction.name == "OUT" && it.createdAt in start..end }
                .sumOf { it.myShare }

            val label = ym.format(monthLabelFormatter)
            trendList.add(MonthlyTrendItem(ym.toString(), label, monthSpend))
        }
        trendList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Predictive Burn-Rate Pacing: "At this pace your money lasts N more days"
    data class PacingStatus(
        val dailyBurnRatePaise: Long,
        val daysRemaining: Int,
        val isExceeded: Boolean
    )

    val pacingStatus: StateFlow<PacingStatus> = combine(
        totalSpentThisMonth,
        _monthlyBudgetPaise
    ) { spent, budget ->
        val today = LocalDate.now(DateTimeUtils.IndianZoneId)
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)

        val dailyBurnRate = spent / daysElapsed
        val remainingBudget = budget - spent
        val isExceeded = remainingBudget <= 0L

        val daysRemaining = if (!isExceeded && dailyBurnRate > 0) {
            (remainingBudget / dailyBurnRate).toInt()
        } else 0

        PacingStatus(
            dailyBurnRatePaise = dailyBurnRate,
            daysRemaining = daysRemaining,
            isExceeded = isExceeded
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PacingStatus(0L, 0, false)
    )

    /**
     * Exports all transactions to CSV and shares via FileProvider.
     */
    fun exportCsv(context: Context, onReadyToShare: (Intent) -> Unit) {
        viewModelScope.launch {
            val allPayments = paymentRepository.getAllPayments().first()
            val csvContent = CsvExporter.generateCsvString(
                payments = allPayments,
                categoryNameProvider = { id -> CategoryPalette.getCategory(id).name }
            )

            val filename = "SpendTag_Expenses_${currentMonth.toString().replace("-", "_")}.csv"
            val file = CsvExporter.writeCsvToFile(context, filename, csvContent)
            val shareIntent = CsvExporter.createShareIntent(context, file)
            onReadyToShare(shareIntent)
        }
    }
}
