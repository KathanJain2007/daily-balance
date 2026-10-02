package com.spendtag.app.core.sms

import java.util.Locale

/**
 * Filter to reject non-transactional SMS messages (OTPs, promotional spam, balance alerts).
 * 
 * Strict Data Rule:
 * Never stores or acts on OTPs, passwords, or promotional content.
 */
object SmsFilter {

    private val OTP_PATTERNS = listOf(
        "\\botp\\b",
        "one[- ]time[- ]password",
        "verification code",
        "secret code",
        "authorization code",
        "auth code",
        "do not share",
        "valid for \\d+ min",
        "use code",
        "security code"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val PROMOTIONAL_PATTERNS = listOf(
        "pre-approved",
        "pre approved",
        "apply now",
        "congratulations",
        "congrats",
        "personal loan",
        "instant loan",
        "click here",
        "click http",
        "claim your",
        "special offer",
        "cashback of up to",
        "discount coupon",
        "kyc is due",
        "update your kyc",
        "bill generated",
        "statement is ready"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val TRANSACTION_VERBS = listOf(
        "debited",
        "debit of",
        "credited",
        "credit of",
        "spent",
        "sent",
        "paid",
        "transferred to",
        "transfer to",
        "received",
        "withdrawn",
        "deducted",
        "refund of",
        "refunded"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    /**
     * Returns true if the message should be ignored and discarded immediately.
     */
    fun isIgnored(smsBody: String): Boolean {
        if (smsBody.isBlank()) return true

        val lowerBody = smsBody.lowercase(Locale.ROOT)

        // 1. Immediately drop any OTP / Authentication tokens
        if (OTP_PATTERNS.any { it.containsMatchIn(lowerBody) }) {
            return true
        }

        // 2. Drop promotional and marketing spam
        if (PROMOTIONAL_PATTERNS.any { it.containsMatchIn(lowerBody) }) {
            return true
        }

        // 3. Must contain an active transaction verb
        val hasTransactionVerb = TRANSACTION_VERBS.any { it.containsMatchIn(lowerBody) }
        if (!hasTransactionVerb) {
            return true
        }

        return false
    }
}
