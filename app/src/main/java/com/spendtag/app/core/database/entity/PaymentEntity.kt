package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus

/**
 * Payment table definition.
 * 
 * Strict Data Rules:
 * 1. [amountPaise] is strictly Long paise (no floats/doubles).
 * 2. [myShare] defaults to [amountPaise]. Category totals ALWAYS sum [myShare].
 * 3. Never stores raw SMS text.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["refNo"]),
        Index(value = ["createdAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"]),
        Index(value = ["amountPaise", "direction", "upiId", "createdAt"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amountPaise: Long,
    val direction: PaymentDirection,
    val upiId: String? = null,
    val refNo: String? = null,
    val source: PaymentSource,
    val createdAt: Long,
    val categoryId: Long? = null,
    val myShare: Long = amountPaise,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val note: String? = null
)
