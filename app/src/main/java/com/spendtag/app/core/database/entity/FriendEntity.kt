package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Friend table definition for bill splits and settlements.
 */
@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["upiId"]),
        Index(value = ["name"])
    ]
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val upiId: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
