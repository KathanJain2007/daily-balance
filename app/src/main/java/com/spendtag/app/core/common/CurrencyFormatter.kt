package com.spendtag.app.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * SpendTag Currency Formatter
 * 
 * Strict Data Rule:
 * All monetary amounts are stored as Long paise (1 INR = 100 paise).
 * Never uses Double or Float for monetary representation to avoid IEEE-754 precision drift.
 * 
 * Formats numbers following the Indian numbering system:
 * e.g., 10000000 paise -> ₹1,00,000
 * 500 paise -> ₹5
 * 550 paise -> ₹5.50
 */
object CurrencyFormatter {

    private const val RUPEE_SYMBOL = "₹"

    /**
     * Formats Long paise into an Indian Rupee string (e.g. ₹1,23,456 or ₹500).
     *
     * @param amountPaise The amount in paise.
     * @param includeSymbol Whether to prefix with "₹".
     * @param showDecimalsAlways If true, always shows two decimal places (e.g. ₹500.00).
     *                           If false, omits ".00" when paise % 100 == 0.
     */
    fun formatPaise(
        amountPaise: Long,
        includeSymbol: Boolean = true,
        showDecimalsAlways: Boolean = false
    ): String {
        val isNegative = amountPaise < 0
        val absPaise = abs(amountPaise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val formattedRupees = formatIndianGrouping(rupees)

        val amountString = if (remainingPaise == 0L && !showDecimalsAlways) {
            formattedRupees
        } else {
            val paiseString = remainingPaise.toString().padStart(2, '0')
            "$formattedRupees.$paiseString"
        }

        val prefix = if (includeSymbol) RUPEE_SYMBOL else ""
        return if (isNegative) {
            "-$prefix$amountString"
        } else {
            "$prefix$amountString"
        }
    }

    /**
     * Formats an integer into the Indian grouping pattern:
     * e.g.:
     * 1000 -> 1,000
     * 10000 -> 10,000
     * 100000 -> 1,00,000
     * 10000000 -> 1,00,00,000
     */
    fun formatIndianGrouping(rupees: Long): String {
        val str = rupees.toString()
        val length = str.length

        if (length <= 3) {
            return str
        }

        // The rightmost 3 digits
        val lastThree = str.substring(length - 3)
        var remaining = str.substring(0, length - 3)

        val groups = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            if (remaining.length <= 2) {
                groups.add(0, remaining)
                remaining = ""
            } else {
                val chunk = remaining.substring(remaining.length - 2)
                groups.add(0, chunk)
                remaining = remaining.substring(0, remaining.length - 2)
            }
        }

        return groups.joinToString(",") + "," + lastThree
    }

    /**
     * Converts a user input string (e.g. "150", "150.50", "1,200") to Long paise.
     */
    fun parseToPaise(input: String): Long {
        val clean = input.replace("₹", "")
            .replace(",", "")
            .replace("Rs", "", ignoreCase = true)
            .trim()
        if (clean.isBlank()) return 0L

        return try {
            val bd = BigDecimal(clean)
            bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Converts rupees to paise safely.
     */
    fun rupeesToPaise(rupees: Long): Long = rupees * 100L

    /**
     * Converts paise to whole rupees (floor).
     */
    fun paiseToRupees(paise: Long): Long = paise / 100L
}
