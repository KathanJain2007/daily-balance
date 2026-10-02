package com.spendtag.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.spendtag.app.SpendTagApplication
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for incoming SMS messages.
 * 
 * Non-Obvious Android Concept:
 * Broadcast receivers on Android must not perform blocking disk I/O on the main thread
 * (which causes ANRs). We invoke [goAsync()] to hold the wake lock and move database
 * insertion to an I/O coroutine dispatcher, then signal [pendingResult.finish()] upon completion.
 */
class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                processSmsIntent(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS intent", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processSmsIntent(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // 1. Group multi-part SMS PDUs by originating sender to reconstruct complete narrative
        val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown" }

        val app = context.applicationContext as? SpendTagApplication ?: return
        val paymentRepository = app.paymentRepository

        for ((sender, parts) in messagesBySender) {
            val fullBody = parts.joinToString(separator = "") { it.displayMessageBody ?: "" }
            val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            // 2. Parse financial details (strictly Long paise, never raw text)
            val parsedTxn = SmsParser.parse(
                smsBody = fullBody,
                timestamp = timestamp,
                sender = sender
            ) ?: continue

            // 3. Construct domain entity with myShare defaulting to full amount
            val payment = PaymentEntity(
                amountPaise = parsedTxn.amountPaise,
                direction = parsedTxn.direction,
                upiId = parsedTxn.upiId,
                refNo = parsedTxn.refNo,
                source = PaymentSource.SMS,
                createdAt = parsedTxn.timestamp,
                categoryId = null,
                myShare = parsedTxn.amountPaise,
                status = PaymentStatus.PENDING,
                note = parsedTxn.payeeName
            )

            // 4. Atomic deduplicated insertion
            val insertedId = paymentRepository.insertIfUnique(payment)
            if (insertedId != null) {
                Log.d(TAG, "Successfully recorded payment ID $insertedId for amount ${payment.amountPaise} paise")
                // Hook for Stage 4: Trigger Quick-Tag Notification
                onPaymentInserted(context, insertedId, payment)
            } else {
                Log.d(TAG, "Skipped duplicate payment with refNo ${payment.refNo}")
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"

        /**
         * Stage 4 Hook: Called when a new unique payment is persisted.
         * Subscribed by the notification manager in Stage 4.
         */
        var onPaymentInsertedListener: ((Context, Long, PaymentEntity) -> Unit)? = null

        internal fun onPaymentInserted(context: Context, paymentId: Long, payment: PaymentEntity) {
            onPaymentInsertedListener?.invoke(context, paymentId, payment)
        }
    }
}
