package com.spendtag.app.core.sms

import com.spendtag.app.core.model.PaymentDirection

/**
 * Data extracted from a financial transaction SMS.
 * 
 * Strict Data Rule:
 * Never stores the raw SMS text to protect user privacy (no account numbers,
 * available balances, or personal phone numbers are retained).
 * 
 * @property amountPaise Transaction amount strictly in Long paise.
 * @property direction OUT for debits/spends, IN for credits/refunds/settlements.
 * @property upiId Virtual Payment Address (e.g. "chai@okaxis") if present.
 * @property refNo UTR, UPI reference, or bank transaction reference number.
 * @property payeeName Extracted merchant or sender alias (e.g. "Chai Point", "Swiggy").
 * @property timestamp Epoch milliseconds of the SMS receipt.
 * @property senderHeader Bank header code (e.g. "VM-HDFCBK", "AX-SBIINB") if available.
 */
data class ParsedSmsTransaction(
    val amountPaise: Long,
    val direction: PaymentDirection,
    val upiId: String?,
    val refNo: String?,
    val payeeName: String?,
    val timestamp: Long,
    val senderHeader: String? = null
)
