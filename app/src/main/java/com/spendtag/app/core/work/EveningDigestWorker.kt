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
import com.spendtag.app.core.notification.NotificationHelper
import kotlinx.coroutines.flow.first

/**
 * Daily 9:00 PM Evening Digest Worker.
 * Summarizes all pending untagged transactions into one clean notification.
 */
class EveningDigestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SpendTagApplication ?: return Result.failure()
        val pendingCount = app.paymentRepository.getPendingCount().first()

        val now = System.currentTimeMillis()
        val startOfDay = com.spendtag.app.core.common.DateTimeUtils.getStartOfDayMillis(now)
        val endOfDay = com.spendtag.app.core.common.DateTimeUtils.getEndOfDayMillis(now)
        val silentCount = app.database.tagHistoryDao().getSilentTagCountBetween(startOfDay, endOfDay)

        if (pendingCount > 0 || silentCount > 0) {
            showDigestNotification(applicationContext, pendingCount, silentCount)
        }

        return Result.success()
    }

    private fun showDigestNotification(context: Context, pendingCount: Int, silentCount: Int) {
        val targetDestination = if (pendingCount > 0) "pending" else "payments"
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", targetDestination)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            pendingCount > 0 && silentCount > 0 -> "$pendingCount payments need tagging"
            pendingCount > 0 -> if (pendingCount == 1) "1 payment needs tagging" else "$pendingCount payments need tagging"
            else -> "SpendTag: $silentCount payments auto-tagged"
        }

        val body = when {
            pendingCount > 0 && silentCount > 0 -> "$silentCount payments auto-tagged silently today. Tap to categorize the rest."
            pendingCount > 0 -> "Organize today's expenses with one tap in your Pending stack."
            else -> if (silentCount == 1) "1 payment was automatically categorized today. Tap to review." else "$silentCount payments were automatically categorized today. Tap to review."
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val WORK_NAME = "spendtag_evening_digest"
    }
}
