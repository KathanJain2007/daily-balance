package com.spendtag.app.core.sms

import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.model.PaymentDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * High-performance, offline SMS parser for Indian banking and UPI alerts.
 * 
 * Strict Data Rule:
 * Money is converted strictly to Long paise.
 * Never stores raw SMS text.
 */
object SmsParser {

    private val UPI_VPA_REGEX = Regex(
        "([a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64})",
        RegexOption.IGNORE_CASE
    )

    private val UPI_SLASH_REF_REGEX = Regex(
        "UPI/(?:CR|DR)/([0-9]{12})",
        RegexOption.IGNORE_CASE
    )

    private val STANDARD_REF_REGEX = Regex(
        "(?:UPI\\s*Ref(?:\\s*No\\.?)?|Ref(?:\\s*No\\.?|\\s*Num\\.?|\\s*#)?|UTR|RRN|Txn\\s*Id)[\\s/:]*([A-Za-z0-9]{6,22})",
        RegexOption.IGNORE_CASE
    )

    // Transaction Amount Patterns (Ordered by specificity to prevent picking up balance amounts)
    private val AMOUNT_PATTERNS = listOf(
        // "Paid ₹240", "Sent Rs. 350.00", "spent INR 3,250.00"
        Regex("(?:sent|paid|spent)\\s+(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
        // "debited for Rs 1,499.00", "debited by 120.00", "credited by Rs 500.00"
        Regex("(?:debited|credited)\\s+(?:for|by)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
        // "INR 1,500.00 credited", "Rs 500.00 debited"
        Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:debited|credited|spent|sent|paid)", RegexOption.IGNORE_CASE),
        // Fallback: Rs/INR/₹ followed by digits (before any mention of "bal" or "balance")
        Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    )

    // Payee / Merchant Patterns
    private val PAYEE_PATTERNS = listOf(
        // "Paid ₹240 to Chai Point (chai@icici)"
        Regex("(?:paid|sent)\\s+(?:Rs\\.?|INR|₹)?\\s*[0-9,.]+\\s+to\\s+([A-Za-z0-9\\s&'-]+?)(?:\\s*\\(|\\s+on|\\s+Ref|\\.)", RegexOption.IGNORE_CASE),
        // "towards ZOMATO"
        Regex("towards\\s+([A-Za-z0-9\\s&'-]+?)(?:\\.|\\s+Ref|\\s+on|\\s+Avl|$)", RegexOption.IGNORE_CASE),
        // "at RELIANCE RETAIL on"
        Regex("at\\s+([A-Za-z0-9\\s&'-]+?)(?:\\.|\\s+Ref|\\s+on|\\s+Avl|$)", RegexOption.IGNORE_CASE)
    )

    /**
     * Parses an incoming SMS message. Returns [ParsedSmsTransaction] or null if the message
     * is not a valid financial transaction (e.g. OTP, promotional, or unrecognized format).
     */
    fun parse(
        smsBody: String,
        timestamp: Long = System.currentTimeMillis(),
        sender: String? = null
    ): ParsedSmsTransaction? {
        if (SmsFilter.isIgnored(smsBody)) {
            return null
        }

        // 1. Determine Direction (OUT vs IN)
        val direction = determineDirection(smsBody) ?: return null

        // 2. Extract Amount in Long paise
        val amountPaise = extractAmountPaise(smsBody) ?: return null
        if (amountPaise <= 0L) return null

        // 3. Extract UPI ID (if present)
        val upiId = extractUpiId(smsBody)

        // 4. Extract Reference Number (UTR / Ref No)
        val refNo = extractRefNo(smsBody)

        // 5. Extract Payee / Merchant / Counterparty Alias
        val payeeName = extractPayee(smsBody, upiId)

        return ParsedSmsTransaction(
            amountPaise = amountPaise,
            direction = direction,
            upiId = upiId,
            refNo = refNo,
            payeeName = payeeName,
            timestamp = timestamp,
            senderHeader = sender
        )
    }

    private fun determineDirection(body: String): PaymentDirection? {
        val lower = body.lowercase(Locale.ROOT)

        // High confidence credit terms
        val creditMatches = listOf("credited", "credit of", "received", "refund of", "deposited", "refunded")
            .any { lower.contains(it) }

        // High confidence debit terms
        val debitMatches = listOf("debited", "debit of", "sent", "spent", "paid", "transferred to", "transfer to", "withdrawn", "deducted")
            .any { lower.contains(it) }

        return when {
            creditMatches && !debitMatches -> PaymentDirection.IN
            debitMatches && !creditMatches -> PaymentDirection.OUT
            creditMatches && debitMatches -> {
                // If both occur, check which keyword appears first in the narrative
                val firstCreditIndex = listOf("credited", "received").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val firstDebitIndex = listOf("debited", "spent", "sent", "paid").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                if (firstCreditIndex < firstDebitIndex) PaymentDirection.IN else PaymentDirection.OUT
            }
            else -> null
        }
    }

    private fun extractAmountPaise(body: String): Long? {
        // Strip out available balance portions so they are not accidentally picked
        val bodyWithoutBalance = body.split(Regex("(?i)\\b(?:avl\\s*bal|available\\s*balance|bal\\s*is|bal\\s*inr|bal\\s*rs|avl\\s*lmt)\\b"))
            .firstOrNull() ?: body

        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(bodyWithoutBalance) ?: continue
            val amountGroup = match.groups[1]?.value ?: continue
            val cleanAmount = amountGroup.replace(",", "").trim()
            try {
                val bd = BigDecimal(cleanAmount)
                return bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
            } catch (_: Exception) {
                // Try next pattern if decimal conversion fails
            }
        }
        return null
    }

    private fun extractUpiId(body: String): String? {
        val match = UPI_VPA_REGEX.find(body) ?: return null
        val vpa = match.value.trim()
        // Ensure it doesn't end with a punctuation mark
        return vpa.trimEnd('.', ',', ';', ':', ')', ']')
    }

    private fun extractRefNo(body: String): String? {
        // 1. Check for standard UPI slash pattern e.g. UPI/CR/428912345678 or UPI/DR/429012345678
        val slashMatch = UPI_SLASH_REF_REGEX.find(body)
        if (slashMatch != null) {
            return slashMatch.groups[1]?.value
        }

        // 2. Check for "Ref No 12345", "UPI Ref: 12345", "UTR 12345"
        val refMatch = STANDARD_REF_REGEX.find(body)
        if (refMatch != null) {
            val candidate = refMatch.groups[1]?.value?.trim()?.trimEnd('.', ',', ';')
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }

        return null
    }

    private fun extractPayee(body: String, upiId: String?): String? {
        for (pattern in PAYEE_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val payee = match.groups[1]?.value?.trim()?.trimEnd('.', ',', ';')
            if (!payee.isNullOrBlank() && !payee.equals(upiId, ignoreCase = true)) {
                return payee
            }
        }
        return null
    }
}
