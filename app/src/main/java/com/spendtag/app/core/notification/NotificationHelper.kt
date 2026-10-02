package com.spendtag.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spendtag.app.MainActivity
import com.spendtag.app.R
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.receiver.NotificationActionReceiver
import com.spendtag.app.ui.theme.CategoryPalette

/**
 * Helper for creating channels and displaying instant quick-tag notifications.
 * 
 * Non-Obvious Android Concept:
 * Unique PendingIntent request codes are generated for each payment and each action button
 * using (paymentId * 10 + actionIndex) to avoid Android's intent-recycling collisions.
 */
object NotificationHelper {

    const val CHANNEL_EXPENSE_ALERTS = "spendtag_expense_alerts"
    const val CHANNEL_REMINDERS = "spendtag_reminders"

    const val EXTRA_PAYMENT_ID = "extra_payment_id"
    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val EXTRA_CATEGORY_ID = "extra_category_id"

    const val ACTION_TAG_CATEGORY = "action_tag_category"
    const val ACTION_SETTLE = "action_settle"
    const val ACTION_INCOME = "action_income"
    const val ACTION_DISMISS = "action_dismiss"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. High-Priority Heads-Up Channel for instant tagging
            val alertChannel = NotificationChannel(
                CHANNEL_EXPENSE_ALERTS,
                "Instant Expense Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate 1-second category tagging notifications for UPI and card payments"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Default Priority Channel for Reminders and Evening Digest
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders & Evening Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "2-hour single reminder and 9 PM evening digest for untagged payments"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    /**
     * Shows a quick-tag notification with up to 3 one-tap action buttons.
     */
    fun showQuickTagNotification(
        context: Context,
        paymentId: Long,
        payment: PaymentEntity,
        bestGuessCategoryId: Long = CategoryPalette.FoodAndDining.id,
        secondGuessCategoryId: Long = CategoryPalette.Groceries.id,
        friendNameForSettlement: String? = null
    ) {
        val notificationManager = NotificationManagerCompat.from(context)

        val formattedAmount = CurrencyFormatter.formatPaise(payment.amountPaise)
        val payeeName = payment.note?.ifBlank { null }
            ?: payment.upiId
            ?: "Card Payment"

        val reqCodeBase = (paymentId % 100000).toInt() * 10

        // Intent when clicking the notification body -> Opens Pending screen in app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pending")
            putExtra("payment_id", paymentId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reqCodeBase,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete Intent (Swiped away by user) -> Schedules one-time 2-hour reminder
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_PAYMENT_ID, paymentId)
            putExtra(EXTRA_ACTION_TYPE, ACTION_DISMISS)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            reqCodeBase + 4,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_EXPENSE_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (payment.direction == PaymentDirection.OUT) {
            builder.setContentTitle("$formattedAmount to $payeeName")
            builder.setContentText("What was this for?")

            // Button 1: Best guess category
            val bestCategory = CategoryPalette.getCategory(bestGuessCategoryId)
            val action1Intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_TAG_CATEGORY
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_ACTION_TYPE, ACTION_TAG_CATEGORY)
                putExtra(EXTRA_CATEGORY_ID, bestGuessCategoryId)
            }
            val pending1 = PendingIntent.getBroadcast(
                context,
                reqCodeBase + 1,
                action1Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, bestCategory.name, pending1)

            // Button 2: Second guess category
            val secondCategory = CategoryPalette.getCategory(secondGuessCategoryId)
            val action2Intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_TAG_CATEGORY
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_ACTION_TYPE, ACTION_TAG_CATEGORY)
                putExtra(EXTRA_CATEGORY_ID, secondGuessCategoryId)
            }
            val pending2 = PendingIntent.getBroadcast(
                context,
                reqCodeBase + 2,
                action2Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, secondCategory.name, pending2)

            // Button 3: More (opens Pending in app)
            builder.addAction(0, "More…", contentPendingIntent)

        } else {
            // Incoming Money (Credit)
            val title = if (friendNameForSettlement != null) {
                "$formattedAmount from $friendNameForSettlement"
            } else {
                "$formattedAmount received"
            }
            builder.setContentTitle(title)
            builder.setContentText("Choose how to record this transfer")

            // Button 1: Settle
            val settleLabel = if (friendNameForSettlement != null) "Settle ($friendNameForSettlement)" else "Settle"
            val settleIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SETTLE
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_ACTION_TYPE, ACTION_SETTLE)
            }
            val settlePending = PendingIntent.getBroadcast(
                context,
                reqCodeBase + 1,
                settleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, settleLabel, settlePending)

            // Button 2: Income
            val incomeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_INCOME
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_ACTION_TYPE, ACTION_INCOME)
                putExtra(EXTRA_CATEGORY_ID, CategoryPalette.Transfers.id)
            }
            val incomePending = PendingIntent.getBroadcast(
                context,
                reqCodeBase + 2,
                incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Income", incomePending)

            // Button 3: More
            builder.addAction(0, "More…", contentPendingIntent)
        }

        try {
            notificationManager.notify(paymentId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Handled if POST_NOTIFICATIONS is not yet granted
        }
    }

    fun cancelNotification(context: Context, paymentId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(paymentId.toInt())
    }
}
