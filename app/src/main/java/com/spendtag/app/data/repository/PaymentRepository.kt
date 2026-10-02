package com.spendtag.app.data.repository

import com.spendtag.app.core.database.dao.PaymentDao
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.model.CategorySpending
import com.spendtag.app.core.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating access to Payment data and strict business rules.
 */
class PaymentRepository(private val paymentDao: PaymentDao) {

    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    fun getPendingPayments(): Flow<List<PaymentEntity>> = paymentDao.getPendingPayments()

    fun getPendingCount(): Flow<Int> = paymentDao.getPendingCount()

    fun getRecentPayments(limit: Int = 5): Flow<List<PaymentEntity>> = paymentDao.getRecentPayments(limit)

    fun getPaymentsBetween(startTime: Long, endTime: Long): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsBetween(startTime, endTime)

    suspend fun getPaymentById(id: Long): PaymentEntity? = paymentDao.getPaymentById(id)

    /**
     * Inserts a payment applying deduplication:
     * - Unique refNo
     * - 2-minute time window for missing refNo
     */
    suspend fun insertIfUnique(payment: PaymentEntity): Long? = paymentDao.insertIfUnique(payment)

    suspend fun tagPayment(paymentId: Long, categoryId: Long) {
        paymentDao.updateCategory(paymentId, categoryId, PaymentStatus.TAGGED)
    }

    suspend fun skipPayment(paymentId: Long) {
        paymentDao.updateCategory(paymentId, null, PaymentStatus.UNSORTED)
    }

    suspend fun updateMyShare(paymentId: Long, myShare: Long) {
        paymentDao.updateMyShare(paymentId, myShare)
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        paymentDao.updatePayment(payment)
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        paymentDao.deletePayment(payment)
    }

    /**
     * Strict Rule: Category totals ALWAYS sum myShare.
     */
    fun getCategorySpendingSummary(startTime: Long, endTime: Long): Flow<List<CategorySpending>> =
        paymentDao.getCategorySpendingSummary(startTime, endTime)

    fun getTotalSpentPaise(startTime: Long, endTime: Long): Flow<Long> =
        paymentDao.getTotalSpentPaise(startTime, endTime)

    fun getTopPayees(startTime: Long, endTime: Long, limit: Int = 5) =
        paymentDao.getTopPayees(startTime, endTime, limit)
}
