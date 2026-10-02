package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Settlement table definition.
 * Links an incoming reimbursement payment to a friend balance so that
 * the incoming money is NEVER counted as income.
 */
@Entity(
    tableName = "settlements",
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
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val paymentId: Long,
    val friendId: Long,
    val amountPaise: Long,
    val settledAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
