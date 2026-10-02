package com.spendtag.app.core.split

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.spendtag.app.core.common.CurrencyFormatter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * WhatsApp reminder generator for settling friend splits without awkwardness.
 * Formats polite messages and embeds clickable standard NPCI UPI payment deep links.
 */
object WhatsAppReminderHelper {

    /**
     * Builds a polite, friendly WhatsApp reminder message.
     */
    fun buildReminderMessage(
        friendName: String,
        expenseDescription: String?,
        amountPaise: Long,
        receiverUpiId: String? = null,
        receiverName: String? = null
    ): String {
        val formattedAmount = CurrencyFormatter.formatPaise(amountPaise)
        val forDesc = if (!expenseDescription.isNullOrBlank()) " for $expenseDescription" else ""

        val upiLink = if (!receiverUpiId.isNullOrBlank()) {
            val amountInr = String.format(Locale.ENGLISH, "%.2f", amountPaise / 100.0)
            val nameParam = if (!receiverName.isNullOrBlank()) {
                "&pn=" + URLEncoder.encode(receiverName, StandardCharsets.UTF_8.toString())
            } else ""
            "\n\nYou can pay me directly via UPI:\nupi://pay?pa=$receiverUpiId$nameParam&am=$amountInr&cu=INR"
        } else ""

        return "Hey $friendName, your share$forDesc is $formattedAmount.$upiLink"
    }

    /**
     * Creates an Intent to send the reminder message directly via WhatsApp or system share.
     */
    fun createReminderIntent(
        context: Context,
        friendName: String,
        phoneNumber: String?,
        expenseDescription: String?,
        amountPaise: Long,
        receiverUpiId: String? = null,
        receiverName: String? = null
    ): Intent {
        val message = buildReminderMessage(
            friendName = friendName,
            expenseDescription = expenseDescription,
            amountPaise = amountPaise,
            receiverUpiId = receiverUpiId,
            receiverName = receiverName
        )

        val cleanPhone = phoneNumber?.replace(Regex("[^0-9]"), "")
        val formattedPhone = when {
            cleanPhone.isNullOrBlank() -> null
            cleanPhone.length == 10 -> "91$cleanPhone" // Standard Indian 10-digit mobile
            else -> cleanPhone
        }

        return if (formattedPhone != null) {
            val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg")
            Intent(Intent.ACTION_VIEW, uri)
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
            }
        }
    }
}
