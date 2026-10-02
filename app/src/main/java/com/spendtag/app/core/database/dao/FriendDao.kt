package com.spendtag.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendtag.app.core.database.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getFriendById(id: Long): FriendEntity?

    @Query("SELECT * FROM friends WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getFriendByName(name: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE upiId = :upiId LIMIT 1")
    suspend fun getFriendByUpiId(upiId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity): Long

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendEntity)
}
