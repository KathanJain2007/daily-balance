package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SplitShare table definition.
 * Tracks each individual friend's share in a split expense.
 * 
 * Strict Data Rule:
 * sharePaise is in Long paise.
 */
@Entity(
    tableName = "split_shares",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FriendEntity::class,
            parentColumns = ["id"],
            childColumns = ["friendId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["friendId"])
    ]
)
data class SplitShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val paymentId: Long,
    val friendId: Long,
    val sharePaise: Long,
    val isSettled: Boolean = false,
    val settledAt: Long? = null
)
