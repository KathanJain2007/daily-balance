package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Counterparty table definition.
 * Stores learned associations for UPI IDs, aliases, and default categories.
 * 
 * Strict Data Rule:
 * upiId is unique.
 */
@Entity(
    tableName = "counterparties",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["defaultCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["upiId"], unique = true),
        Index(value = ["defaultCategoryId"])
    ]
)
data class CounterpartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val upiId: String,
    val alias: String,
    val defaultCategoryId: Long? = null,
    val lastUsedAt: Long = System.currentTimeMillis()
)
