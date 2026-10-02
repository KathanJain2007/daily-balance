package com.spendtag.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendtag.app.core.database.dao.CategoryDao
import com.spendtag.app.core.database.dao.CounterpartyDao
import com.spendtag.app.core.database.dao.FriendDao
import com.spendtag.app.core.database.dao.PaymentDao
import com.spendtag.app.core.database.dao.SplitDao
import com.spendtag.app.core.database.dao.TagHistoryDao
import com.spendtag.app.core.database.entity.CategoryEntity
import com.spendtag.app.core.database.entity.CounterpartyEntity
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.SettlementEntity
import com.spendtag.app.core.database.entity.SplitShareEntity
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.ui.theme.CategoryPalette

/**
 * SpendTag local-first Room Database.
 * Holds all 7 required core financial entities.
 * 
 * Strict Data Rule:
 * All data stays on the user's phone. No remote sync, no network.
 */
@Database(
    entities = [
        CategoryEntity::class,
        PaymentEntity::class,
        CounterpartyEntity::class,
        TagHistoryEntity::class,
        FriendEntity::class,
        SplitShareEntity::class,
        SettlementEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(SpendTagTypeConverters::class)
abstract class SpendTagDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun counterpartyDao(): CounterpartyDao
    abstract fun tagHistoryDao(): TagHistoryDao
    abstract fun friendDao(): FriendDao
    abstract fun splitDao(): SplitDao

    companion object {
        private const val DATABASE_NAME = "spendtag.db"

        @Volatile
        private var INSTANCE: SpendTagDatabase? = null

        fun getInstance(context: Context): SpendTagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendTagDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(SeedCategoriesCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Database creation callback that pre-seeds the 12 curated categories
         * atomically within the initial SQLite transaction.
         */
        val SeedCategoriesCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Atomic insert of the 12 curated default categories
                CategoryPalette.defaultCategories.forEach { category ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO categories (id, name, parentId, colour, icon, isDefault) 
                        VALUES (${category.id}, '${category.name.replace("'", "''")}', NULL, '${category.colorHex}', '${category.iconName}', 1)
                        """.trimIndent()
                    )
                }
            }
        }
    }
}
