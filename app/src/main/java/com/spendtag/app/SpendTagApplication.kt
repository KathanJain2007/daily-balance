package com.spendtag.app

import android.app.Application
import com.spendtag.app.core.database.SpendTagDatabase
import com.spendtag.app.data.repository.CategoryRepository
import com.spendtag.app.data.repository.PaymentRepository

import com.spendtag.app.core.notification.NotificationHelper
import com.spendtag.app.core.work.WorkScheduler
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.receiver.SmsReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SpendTag Application class.
 * Central entry point for initializing local-first state, Room database,
 * background notification channels, and periodic workers.
 */
class SpendTagApplication : Application() {

    val database by lazy { SpendTagDatabase.getInstance(this) }
    val paymentRepository by lazy { PaymentRepository(database.paymentDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val splitRepository by lazy {
        com.spendtag.app.data.repository.SplitRepository(
            splitDao = database.splitDao(),
            friendDao = database.friendDao(),
            paymentDao = database.paymentDao()
        )
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1. Create Android Notification Channels (API 26+)
        NotificationHelper.createNotificationChannels(this)

        // 2. Schedule 9:00 PM Daily Evening Digest
        WorkScheduler.scheduleDailyEveningDigest(this)

        // 3. Connect SMS detection to Smart Tagging Engine & instant Quick-Tag Notification
        SmsReceiver.onPaymentInsertedListener = { context, paymentId, payment ->
            applicationScope.launch {
                val counterparty = if (payment.upiId != null) {
                    database.counterpartyDao().getByUpiId(payment.upiId)
                } else null

                val recentHistory = if (payment.upiId != null) {
                    database.tagHistoryDao().getRecentTagsForUpi(payment.upiId, 10)
                } else emptyList()

                val decision = com.spendtag.app.core.smarttag.SmartTaggingEngine.evaluateWithHistory(
                    payment = payment,
                    counterparty = counterparty,
                    recentHistory = recentHistory
                )

                when (decision) {
                    is com.spendtag.app.core.smarttag.TagDecision.SilentTag -> {
                        // Confident habit detected (>= 3 consecutive identical tags): Tag silently without interrupting user
                        paymentRepository.tagPayment(paymentId, decision.categoryId)

                        database.tagHistoryDao().insertTagHistory(
                            com.spendtag.app.core.database.entity.TagHistoryEntity(
                                paymentId = paymentId,
                                upiId = payment.upiId,
                                categoryId = decision.categoryId,
                                taggedAt = System.currentTimeMillis(),
                                isSilent = true
                            )
                        )

                        if (payment.upiId != null) {
                            database.counterpartyDao().updateDefaultCategory(
                                upiId = payment.upiId,
                                categoryId = decision.categoryId,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    }

                    is com.spendtag.app.core.smarttag.TagDecision.AskUser -> {
                        // Check if incoming payment is from a friend who owes money
                        var friendName: String? = null
                        if (payment.direction == PaymentDirection.IN && payment.upiId != null) {
                            val friend = database.friendDao().getFriendByUpiId(payment.upiId)
                            if (friend != null) {
                                val owedBalance = database.splitDao().getNetBalanceOwedByFriend(friend.id).first()
                                if (owedBalance > 0L) {
                                    friendName = friend.name
                                }
                            }
                        }

                        NotificationHelper.showQuickTagNotification(
                            context = context,
                            paymentId = paymentId,
                            payment = payment,
                            bestGuessCategoryId = decision.bestGuessCategoryId,
                            secondGuessCategoryId = decision.secondGuessCategoryId,
                            friendNameForSettlement = friendName
                        )
                    }
                }
            }
        }
    }
}
