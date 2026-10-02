package com.spendtag.app.core.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendtag.app.MainActivity
import com.spendtag.app.SpendTagApplication
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.notification.NotificationHelper

/**
 * Worker executed ONCE after 2 hours if an expense notification was skipped or dismissed.
 * Checks if the payment is still PENDING; if so, displays a gentle reminder.
 */
class PaymentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val paymentId = inputData.getLong(KEY_PAYMENT_ID, -1L)
        if (paymentId == -1L) return Result.failure()

        val app = applicationContext as? SpendTagApplication ?: return Result.failure()
        val payment = app.paymentRepository.getPaymentById(paymentId) ?: return Result.success()

        // Only remind if the payment remains untagged
        if (payment.status == PaymentStatus.PENDING) {
            showReminderNotification(applicationContext, paymentId, payment.amountPaise, payment.note ?: payment.upiId)
        }

        return Result.success()
    }

    private fun showReminderNotification(
        context: Context,
        paymentId: Long,
        amountPaise: Long,
        payee: String?
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pending")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (paymentId + 50000).toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = CurrencyFormatter.formatPaise(amountPaise)
        val payeeName = payee ?: "payment"

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("1 payment needs tagging")
            .setContentText("$formattedAmount to $payeeName is waiting for your category.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((paymentId + 50000).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val KEY_PAYMENT_ID = "key_payment_id"
    }
}
