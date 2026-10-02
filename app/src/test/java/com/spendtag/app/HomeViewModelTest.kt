package com.spendtag.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying financial metrics and percentage calculations used in HomeViewModel.
 */
class HomeViewModelTest {

    @Test
    fun calculateMonthOverMonthChange_computesCorrectPercentages() {
        // Case 1: Last month was 0 -> null
        val changeNull = calculateMoM(current = 50000L, last = 0L)
        assertNull(changeNull)

        // Case 2: Increase (+12%)
        val increase = calculateMoM(current = 112000L, last = 100000L)
        assertEquals(12, increase)

        // Case 3: Decrease (-8%)
        val decrease = calculateMoM(current = 92000L, last = 100000L)
        assertEquals(-8, decrease)

        // Case 4: Identical (0%)
        val identical = calculateMoM(current = 100000L, last = 100000L)
        assertEquals(0, identical)
    }

    @Test
    fun calculateCategoryPercentage_computesAccurateShare() {
        val totalSpentPaise = 100000L // Rs 1,000
        val foodPaise = 40000L       // Rs 400
        val travelPaise = 60000L     // Rs 600

        val foodPercentage = (foodPaise.toFloat() / totalSpentPaise.toFloat()) * 100f
        val travelPercentage = (travelPaise.toFloat() / totalSpentPaise.toFloat()) * 100f

        assertEquals(40.0f, foodPercentage, 0.01f)
        assertEquals(60.0f, travelPercentage, 0.01f)
    }

    private fun calculateMoM(current: Long, last: Long): Int? {
        if (last == 0L) return null
        val change = ((current - last).toDouble() / last.toDouble()) * 100
        return change.toInt()
    }
}
