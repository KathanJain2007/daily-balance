package com.spendtag.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Category table definition.
 * 
 * @property id Primary key.
 * @property name Display name (e.g. "Food & Dining").
 * @property parentId Null for top-level categories; points to another category id for sub-categories.
 * @property colour Hex color string (e.g. "#FF7043").
 * @property icon Icon identifier string (e.g. "restaurant").
 * @property isDefault True for seeded system categories, false for custom user categories.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val parentId: Long? = null,
    val colour: String,
    val icon: String,
    val isDefault: Boolean = false
)
