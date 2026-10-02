package com.spendtag.app

import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.export.CsvExporter
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying CSV export formatting and escape rules.
 */
class CsvExporterTest {

    @Test
    fun generateCsv_producesStandardHeaderAndEscapedData() {
        val testPayments = listOf(
            PaymentEntity(
                id = 1L,
                amountPaise = 24000L, // Rs 240.00
                direction = PaymentDirection.OUT,
                upiId = "chai@okaxis",
                refNo = "428719283719",
                source = PaymentSource.SMS,
                createdAt = 1728734400000L, // 12 Oct 2024
                categoryId = 1L,
                myShare = 24000L,
                status = PaymentStatus.TAGGED,
                note = "Chai Point, Koramangala" // Has a comma!
            ),
            PaymentEntity(
                id = 2L,
                amountPaise = 500000L, // Rs 5,000.00
                direction = PaymentDirection.OUT,
                upiId = "dinner@icici",
                refNo = "428799881122",
                source = PaymentSource.SMS,
                createdAt = 1728738000000L,
                categoryId = 1L,
                myShare = 100000L, // Rs 1,000.00 personal share
                status = PaymentStatus.TAGGED,
                note = "Dinner \"with\" friends" // Has quotes!
            )
        )

        val csvString = CsvExporter.generateCsvString(
            payments = testPayments,
            categoryNameProvider = { "Food & Dining" }
        )

        // 1. Verify Header row
        assertTrue(csvString.contains("Date,Time,Type,Amount (INR),My Share (INR),Category,Payee / UPI ID,Ref Number,Source,Note"))

        // 2. Verify Row 1: Comma in note is safely wrapped in quotes
        assertTrue(csvString.contains("\"Chai Point, Koramangala\""))
        assertTrue(csvString.contains("240.00"))

        // 3. Verify Row 2: Quotes in note are escaped (""with"")
        assertTrue(csvString.contains("\"Dinner \"\"with\"\" friends\""))
        assertTrue(csvString.contains("5000.00"))
        assertTrue(csvString.contains("1000.00"))
    }
}
