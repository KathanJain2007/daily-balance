package com.spendtag.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.spendtag.app.core.database.entity.SettlementEntity
import com.spendtag.app.core.database.entity.SplitShareEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitShare(share: SplitShareEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitShares(shares: List<SplitShareEntity>)

    @Query("SELECT * FROM split_shares WHERE paymentId = :paymentId")
    fun getSharesForPayment(paymentId: Long): Flow<List<SplitShareEntity>>

    @Query("SELECT * FROM split_shares WHERE friendId = :friendId AND isSettled = 0")
    fun getUnsettledSharesForFriend(friendId: Long): Flow<List<SplitShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Query("SELECT * FROM settlements WHERE friendId = :friendId ORDER BY settledAt DESC")
    fun getSettlementsForFriend(friendId: Long): Flow<List<SettlementEntity>>

    /**
     * Total amount this friend owes you (sum of all unsettled shares).
     */
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(sharePaise) FROM split_shares WHERE friendId = :friendId) - 
            (SELECT COALESCE(SUM(amountPaise), 0) FROM settlements WHERE friendId = :friendId), 
            0
        )
    """)
    fun getNetBalanceOwedByFriend(friendId: Long): Flow<Long>

    /**
     * Total amount you are owed across ALL friends.
     */
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(sharePaise) FROM split_shares) - 
            (SELECT COALESCE(SUM(amountPaise), 0) FROM settlements), 
            0
        )
    """)
    fun getTotalOwedAcrossAllFriends(): Flow<Long>

    @Query("""
        SELECT f.id, f.name, f.upiId, f.phoneNumber, f.createdAt,
               COALESCE((SELECT SUM(s.sharePaise) FROM split_shares s WHERE s.friendId = f.id), 0) -
               COALESCE((SELECT SUM(st.amountPaise) FROM settlements st WHERE st.friendId = f.id), 0) AS balancePaise
        FROM friends f
        ORDER BY balancePaise DESC, f.name ASC
    """)
    fun getFriendsWithBalances(): Flow<List<FriendWithBalance>>

    @Query("DELETE FROM split_shares WHERE paymentId = :paymentId")
    suspend fun deleteSharesForPayment(paymentId: Long)

    @Query("UPDATE split_shares SET isSettled = 1, settledAt = :settledAt WHERE friendId = :friendId AND isSettled = 0")
    suspend fun markSharesSettledForFriend(friendId: Long, settledAt: Long = System.currentTimeMillis())
}

data class FriendWithBalance(
    val id: Long,
    val name: String,
    val upiId: String?,
    val phoneNumber: String?,
    val createdAt: Long,
    val balancePaise: Long
)
