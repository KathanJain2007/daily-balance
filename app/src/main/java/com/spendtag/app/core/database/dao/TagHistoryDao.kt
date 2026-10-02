package com.spendtag.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendtag.app.core.database.entity.TagHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagHistory(history: TagHistoryEntity): Long

    @Query("SELECT * FROM tag_history WHERE upiId = :upiId ORDER BY taggedAt DESC")
    fun getHistoryForUpiId(upiId: String): Flow<List<TagHistoryEntity>>

    @Query("SELECT * FROM tag_history WHERE upiId = :upiId ORDER BY taggedAt DESC LIMIT :limit")
    suspend fun getRecentTagsForUpi(upiId: String, limit: Int = 10): List<TagHistoryEntity>

    @Query("SELECT * FROM tag_history WHERE isSilent = 1 AND taggedAt BETWEEN :startMillis AND :endMillis ORDER BY taggedAt DESC")
    suspend fun getSilentTagsBetween(startMillis: Long, endMillis: Long): List<TagHistoryEntity>

    @Query("SELECT COUNT(*) FROM tag_history WHERE isSilent = 1 AND taggedAt BETWEEN :startMillis AND :endMillis")
    suspend fun getSilentTagCountBetween(startMillis: Long, endMillis: Long): Int

    @Query("""
        SELECT categoryId, COUNT(*) as tagCount 
        FROM tag_history 
        WHERE upiId = :upiId 
        GROUP BY categoryId 
        ORDER BY tagCount DESC 
        LIMIT 1
    """)
    suspend fun getMostFrequentCategoryForUpi(upiId: String): CategoryFrequency?

    data class CategoryFrequency(
        val categoryId: Long,
        val tagCount: Int
    )
}
