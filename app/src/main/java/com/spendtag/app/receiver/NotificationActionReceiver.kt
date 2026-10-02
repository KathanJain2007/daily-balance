package com.spendtag.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spendtag.app.SpendTagApplication
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.notification.NotificationHelper
import com.spendtag.app.core.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background BroadcastReceiver handling one-tap actions from SpendTag notifications.
 * Executes category tagging, settlement, or 2-hour reminder scheduling in < 50ms
 * without opening the user interface.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val paymentId = intent.getLongExtra(NotificationHelper.EXTRA_PAYMENT_ID, -1L)
        val actionType = intent.getStringExtra(NotificationHelper.EXTRA_ACTION_TYPE) ?: intent.action
        val categoryId = intent.getLongExtra(NotificationHelper.EXTRA_CATEGORY_ID, -1L)

        if (paymentId == -1L) return

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val app = context.applicationContext as? SpendTagApplication ?: return@launch
                val db = app.database
                val paymentRepo = app.paymentRepository
                val counterpartyDao = db.counterpartyDao()
                val tagHistoryDao = db.tagHistoryDao()

                when (actionType) {
                    NotificationHelper.ACTION_TAG_CATEGORY -> {
                        if (categoryId != -1L) {
                            // 1. Tag the payment in Room
                            paymentRepo.tagPayment(paymentId, categoryId)

                            // 2. Retrieve payment details to learn counterparty association
                            val payment = paymentRepo.getPaymentById(paymentId)
                            if (payment?.upiId != null) {
                                counterpartyDao.recordCategoryChoice(payment.upiId, categoryId, payment.note ?: payment.upiId)
                            }

                            // 3. Record in Tag History
                            tagHistoryDao.insertTagHistory(
                                TagHistoryEntity(
                                    paymentId = paymentId,
                                    upiId = payment?.upiId,
                                    categoryId = categoryId,
                                    isSilent = false
                                )
                            )

                            // 4. Cancel active notification and cancel any scheduled 2-hour reminder
                            NotificationHelper.cancelNotification(context, paymentId)
                            WorkScheduler.cancelReminder(context, paymentId)
                            Log.d(TAG, "Quick-tagged payment $paymentId to category $categoryId")
                        }
                    }

                    NotificationHelper.ACTION_SETTLE -> {
                        // User chose to settle an incoming reimbursement payment from a friend
                        val payment = paymentRepo.getPaymentById(paymentId)
                        if (payment != null && payment.upiId != null) {
                            val friend = db.friendDao().getFriendByUpiId(payment.upiId)
                            if (friend != null) {
                                app.splitRepository.recordSettlement(
                                    friendId = friend.id,
                                    paymentId = paymentId,
                                    amountPaise = payment.amountPaise,
                                    note = "Settlement from ${friend.name}"
                                )
                                // Tag payment to Income / Transfer category (11)
                                paymentRepo.tagPayment(paymentId, 11L)
                            }
                        }
                        NotificationHelper.cancelNotification(context, paymentId)
                        WorkScheduler.cancelReminder(context, paymentId)
                        Log.d(TAG, "Marked incoming payment $paymentId for friend settlement")
                    }

                    NotificationHelper.ACTION_INCOME -> {
                        // User marked incoming money as income
                        if (categoryId != -1L) {
                            paymentRepo.tagPayment(paymentId, categoryId)
                        }
                        NotificationHelper.cancelNotification(context, paymentId)
                        WorkScheduler.cancelReminder(context, paymentId)
                        Log.d(TAG, "Marked incoming payment $paymentId as income")
                    }

                    NotificationHelper.ACTION_DISMISS -> {
                        // Notification swiped away / skipped -> schedule single 2-hour reminder
                        WorkScheduler.scheduleTwoHourReminder(context, paymentId)
                        Log.d(TAG, "Notification dismissed for payment $paymentId. Scheduled 2-hour reminder.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}
