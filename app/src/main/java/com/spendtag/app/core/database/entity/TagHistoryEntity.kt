package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TagHistory table definition.
 * Records user and silent tagging decisions to compute confidence heuristics.
 */
@Entity(
    tableName = "tag_history",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["upiId"]),
        Index(value = ["categoryId"])
    ]
)
data class TagHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val paymentId: Long,
    val upiId: String? = null,
    val categoryId: Long,
    val taggedAt: Long = System.currentTimeMillis(),
    val isSilent: Boolean = false
)
