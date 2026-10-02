package com.spendtag.app.data.repository

import com.spendtag.app.core.database.dao.FriendDao
import com.spendtag.app.core.database.dao.PaymentDao
import com.spendtag.app.core.database.dao.SplitDao
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.database.entity.SettlementEntity
import com.spendtag.app.core.database.entity.SplitShareEntity
import com.spendtag.app.core.database.dao.FriendWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository mediating access to Bill Splits, Friends, and Reimbursement Settlements.
 */
class SplitRepository(
    private val splitDao: SplitDao,
    private val friendDao: FriendDao,
    private val paymentDao: PaymentDao
) {

    val allFriends: Flow<List<FriendEntity>> = friendDao.getAllFriends()

    val friendsWithBalances: Flow<List<FriendWithBalance>> = splitDao.getFriendsWithBalances()

    val totalOwedAcrossAllFriends: Flow<Long> = splitDao.getTotalOwedAcrossAllFriends()

    fun getSharesForPayment(paymentId: Long): Flow<List<SplitShareEntity>> =
        splitDao.getSharesForPayment(paymentId)

    suspend fun getFriendById(id: Long): FriendEntity? = friendDao.getFriendById(id)

    suspend fun addFriend(name: String, upiId: String? = null, phoneNumber: String? = null): Long {
        val existing = friendDao.getFriendByName(name.trim())
        if (existing != null) return existing.id

        return friendDao.insertFriend(
            FriendEntity(
                name = name.trim(),
                upiId = upiId?.trim()?.ifBlank { null },
                phoneNumber = phoneNumber?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun deleteFriend(friend: FriendEntity) {
        friendDao.deleteFriend(friend)
    }

    /**
     * Splits a payment among friends.
     * Enforces the Strict Data Rule:
     * - The transaction's myShare is updated to personal share.
     * - Category monthly spending queries will strictly count only myShare.
     */
    suspend fun recordSplit(
        paymentId: Long,
        mySharePaise: Long,
        friendShares: List<Pair<Long, Long>> // List of (FriendId, SharePaise)
    ) {
        // 1. Clear any prior shares for this payment to allow editing
        splitDao.deleteSharesForPayment(paymentId)

        // 2. Update payment's personal share
        paymentDao.updateMyShare(paymentId, mySharePaise)

        // 3. Insert individual friend shares
        val shareEntities = friendShares.map { (friendId, sharePaise) ->
            SplitShareEntity(
                paymentId = paymentId,
                friendId = friendId,
                sharePaise = sharePaise,
                isSettled = false
            )
        }
        splitDao.insertSplitShares(shareEntities)
    }

    /**
     * Records an incoming settlement from a friend.
     * Reduces the friend's owed balance without adding to the user's taxable income.
     */
    suspend fun recordSettlement(
        friendId: Long,
        paymentId: Long,
        amountPaise: Long,
        note: String? = null
    ) {
        splitDao.insertSettlement(
            SettlementEntity(
                paymentId = paymentId,
                friendId = friendId,
                amountPaise = amountPaise,
                note = note
            )
        )

        // Check if balance reached zero, if so mark shares settled
        val remaining = splitDao.getNetBalanceOwedByFriend(friendId).first()
        if (remaining <= 0L) {
            splitDao.markSharesSettledForFriend(friendId)
        }
    }
}
