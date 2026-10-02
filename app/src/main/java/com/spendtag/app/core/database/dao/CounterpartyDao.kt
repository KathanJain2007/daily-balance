package com.spendtag.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendtag.app.core.database.entity.CounterpartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterpartyDao {

    @Query("SELECT * FROM counterparties WHERE upiId = :upiId LIMIT 1")
    suspend fun getByUpiId(upiId: String): CounterpartyEntity?

    @Query("SELECT * FROM counterparties ORDER BY lastUsedAt DESC")
    fun getAllCounterparties(): Flow<List<CounterpartyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(counterparty: CounterpartyEntity): Long

    @Query("UPDATE counterparties SET defaultCategoryId = :categoryId, lastUsedAt = :timestamp WHERE upiId = :upiId")
    suspend fun updateDefaultCategory(upiId: String, categoryId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE counterparties SET alias = :alias, lastUsedAt = :timestamp WHERE upiId = :upiId")
    suspend fun updateAlias(upiId: String, alias: String, timestamp: Long = System.currentTimeMillis())

    suspend fun recordCategoryChoice(upiId: String, categoryId: Long, fallbackAlias: String) {
        val existing = getByUpiId(upiId)
        if (existing != null) {
            updateDefaultCategory(upiId, categoryId, System.currentTimeMillis())
        } else {
            insertOrUpdate(
                CounterpartyEntity(
                    upiId = upiId,
                    alias = fallbackAlias,
                    defaultCategoryId = categoryId,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
