package com.spendtag.app.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * SpendTag Date & Time Utilities
 * 
 * Strict Data Rule:
 * Dates are stored in SQLite as epoch milliseconds (Long).
 * Formatted and displayed using modern java.time APIs.
 */
object DateTimeUtils {

    val IndianZoneId: ZoneId = ZoneId.of("Asia/Kolkata")

    private val dayHeaderFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, hh:mm a", Locale.ENGLISH)

    private val timeOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    /**
     * Converts epoch milliseconds to a readable transaction time (e.g. "04:15 PM").
     */
    fun formatTime(epochMillis: Long, zoneId: ZoneId = IndianZoneId): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zonedDateTime = instant.atZone(zoneId)
        return zonedDateTime.format(timeOnlyFormatter)
    }

    /**
     * Formats epoch milliseconds into a short date & time (e.g. "12 Oct, 03:30 PM").
     */
    fun formatShortDateTime(epochMillis: Long, zoneId: ZoneId = IndianZoneId): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zonedDateTime = instant.atZone(zoneId)
        return zonedDateTime.format(shortDateFormatter)
    }

    /**
     * Sticky day header format (e.g. "Today", "Yesterday", or "Monday, 12 October 2026").
     */
    fun formatDayHeader(epochMillis: Long, zoneId: ZoneId = IndianZoneId): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val date = instant.atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)

        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(dayHeaderFormatter)
        }
    }

    /**
     * Formats YearMonth into Month & Year header (e.g. "October 2026").
     */
    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(monthYearFormatter)
    }

    /**
     * Returns the epoch start of month in milliseconds.
     */
    fun getStartOfMonthMillis(yearMonth: YearMonth, zoneId: ZoneId = IndianZoneId): Long {
        return yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Returns the epoch end of month in milliseconds.
     */
    fun getEndOfMonthMillis(yearMonth: YearMonth, zoneId: ZoneId = IndianZoneId): Long {
        return yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }

    /**
     * Returns the epoch start of day in milliseconds.
     */
    fun getStartOfDayMillis(epochMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = IndianZoneId): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Returns the epoch end of day in milliseconds.
     */
    fun getEndOfDayMillis(epochMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = IndianZoneId): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }
}
