package com.spendtag.app.core.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Central scheduler for background WorkManager jobs (2-hour reminder and 9 PM evening digest).
 */
object WorkScheduler {

    /**
     * Schedules a single one-time reminder 2 hours after payment detection.
     */
    fun scheduleTwoHourReminder(context: Context, paymentId: Long) {
        val inputData = Data.Builder()
            .putLong(PaymentReminderWorker.KEY_PAYMENT_ID, paymentId)
            .build()

        val reminderRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(2, TimeUnit.HOURS)
            .addTag("reminder_$paymentId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$paymentId",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )
    }

    /**
     * Cancels any pending 2-hour reminder once the payment is tagged.
     */
    fun cancelReminder(context: Context, paymentId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$paymentId")
    }

    /**
     * Schedules the recurring Daily Evening Digest at 9:00 PM (Asia/Kolkata).
     */
    fun scheduleDailyEveningDigest(context: Context) {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = LocalDateTime.now(zoneId)
        val targetTime = LocalTime.of(21, 0) // 9:00 PM

        var targetDateTime = now.with(targetTime)
        if (now.isAfter(targetDateTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val initialDelayMillis = Duration.between(now, targetDateTime).toMillis()

        val digestRequest = PeriodicWorkRequestBuilder<EveningDigestWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EveningDigestWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            digestRequest
        )
    }
}
