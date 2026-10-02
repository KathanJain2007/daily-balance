package com.spendtag.app.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.database.entity.PaymentEntity
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Production CSV export engine.
 * Exports financial transactions to standard RFC-4180 compliant CSV files,
 * and shares them securely using Android FileProvider.
 */
object CsvExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)

    private const val CSV_HEADER = "Date,Time,Type,Amount (INR),My Share (INR),Category,Payee / UPI ID,Ref Number,Source,Note"

    /**
     * Generates a standard RFC-4180 formatted CSV string.
     */
    fun generateCsvString(
        payments: List<PaymentEntity>,
        categoryNameProvider: (Long?) -> String
    ): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        for (payment in payments) {
            val instant = Instant.ofEpochMilli(payment.createdAt)
            val zonedDateTime = instant.atZone(DateTimeUtils.IndianZoneId)
            val dateStr = zonedDateTime.format(dateFormatter)
            val timeStr = zonedDateTime.format(timeFormatter)

            val amountInr = String.format(Locale.ENGLISH, "%.2f", payment.amountPaise / 100.0)
            val myShareInr = String.format(Locale.ENGLISH, "%.2f", payment.myShare / 100.0)

            val categoryName = categoryNameProvider(payment.categoryId)
            val payee = payment.note ?: payment.upiId ?: "Payment"
            val refNo = payment.refNo ?: ""
            val note = payment.note ?: ""

            val row = listOf(
                escapeCsv(dateStr),
                escapeCsv(timeStr),
                escapeCsv(payment.direction.name),
                amountInr,
                myShareInr,
                escapeCsv(categoryName),
                escapeCsv(payee),
                escapeCsv(refNo),
                escapeCsv(payment.source.name),
                escapeCsv(note)
            ).joinToString(",")

            sb.append(row).append("\n")
        }

        return sb.toString()
    }

    /**
     * Saves CSV string to app cache directory and returns the File.
     */
    fun writeCsvToFile(context: Context, filename: String, csvContent: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, filename)

        FileWriter(targetFile).use { writer ->
            writer.write(csvContent)
        }

        return targetFile
    }

    /**
     * Creates an ACTION_SEND Intent sharing the CSV file via Android FileProvider.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "SpendTag Expense Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Escapes fields according to RFC-4180 (wraps in double quotes if commas or quotes exist,
     * and doubles inner quotes).
     */
    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            val escaped = value.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return value
    }
}
