package com.spendtag.app

import com.spendtag.app.core.common.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Strict Long paise math without precision loss.
 * 2. Indian numbering system formatting (e.g. ₹1,00,000).
 * 3. Tabular edge cases: zero, negative amounts, decimals.
 */
class CurrencyFormatterTest {

    @Test
    fun formatIndianGrouping_formatsStandardAmountsCorrectly() {
        assertEquals("0", CurrencyFormatter.formatIndianGrouping(0L))
        assertEquals("50", CurrencyFormatter.formatIndianGrouping(50L))
        assertEquals("500", CurrencyFormatter.formatIndianGrouping(500L))
        assertEquals("1,000", CurrencyFormatter.formatIndianGrouping(1000L))
        assertEquals("10,000", CurrencyFormatter.formatIndianGrouping(10000L))
        assertEquals("1,00,000", CurrencyFormatter.formatIndianGrouping(100000L))
        assertEquals("10,00,000", CurrencyFormatter.formatIndianGrouping(1000000L))
        assertEquals("1,00,00,000", CurrencyFormatter.formatIndianGrouping(10000000L))
    }

    @Test
    fun formatPaise_formatsPaiseToIndianRupees() {
        // ₹0
        assertEquals("₹0", CurrencyFormatter.formatPaise(0L))

        // ₹5
        assertEquals("₹5", CurrencyFormatter.formatPaise(500L))

        // ₹5.50
        assertEquals("₹5.50", CurrencyFormatter.formatPaise(550L))

        // ₹1,00,000 (10,000,000 paise)
        assertEquals("₹1,00,000", CurrencyFormatter.formatPaise(10000000L))

        // Decimals forced: ₹1,00,000.00
        assertEquals("₹1,00,000.00", CurrencyFormatter.formatPaise(10000000L, showDecimalsAlways = true))

        // Negative values: -₹240
        assertEquals("-₹240", CurrencyFormatter.formatPaise(-24000L))
    }

    @Test
    fun parseToPaise_handlesVariedInputFormats() {
        assertEquals(50000L, CurrencyFormatter.parseToPaise("500"))
        assertEquals(50050L, CurrencyFormatter.parseToPaise("500.50"))
        assertEquals(10000000L, CurrencyFormatter.parseToPaise("₹1,00,000"))
        assertEquals(25000L, CurrencyFormatter.parseToPaise("Rs 250"))
        assertEquals(0L, CurrencyFormatter.parseToPaise(""))
    }
}
