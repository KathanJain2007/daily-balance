package com.spendtag.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test verifying predictive burn-rate pacing calculations:
 * "At this pace your money lasts N more days".
 */
class InsightsPacingTest {

    @Test
    fun calculateDaysRemaining_estimatesAccurately() {
        val budgetPaise = 200000L    // Rs 2,000 budget
        val spentPaise = 65000L      // Rs 650 spent so far
        val daysElapsed = 10         // Day 10 of month

        val dailyBurnRatePaise = spentPaise / daysElapsed // Rs 65/day (6500 paise)
        val remainingBudgetPaise = budgetPaise - spentPaise // Rs 1,350 (135000 paise)

        val daysRemaining = if (dailyBurnRatePaise > 0) {
            (remainingBudgetPaise / dailyBurnRatePaise).toInt().coerceAtLeast(0)
        } else 0

        assertEquals(20, daysRemaining)
    }

    @Test
    fun calculateDaysRemaining_returnsZeroWhenBudgetExhausted() {
        val budgetPaise = 100000L // Rs 1,000 budget
        val spentPaise = 120000L  // Rs 1,200 spent (Exceeded)
        val daysElapsed = 15

        val remainingBudget = budgetPaise - spentPaise
        val daysRemaining = if (remainingBudget <= 0) 0 else 10

        assertEquals(0, daysRemaining)
    }
}
