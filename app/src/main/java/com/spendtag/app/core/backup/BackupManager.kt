package com.spendtag.app.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.core.database.SpendTagDatabase
import com.spendtag.app.core.database.entity.CounterpartyEntity
import com.spendtag.app.core.database.entity.FriendEntity
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.SettlementEntity
import com.spendtag.app.core.database.entity.SplitShareEntity
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Encrypted Database Backup & Restore Engine.
 * 
 * Strict Data Rule:
 * Backups are encrypted at rest using AES-256 GCM backed by the device's KeyStore.
 * Restores execute atomically in a single database transaction.
 */
object BackupManager {

    data class BackupMetadata(
        val version: Int,
        val createdAt: Long,
        val paymentCount: Int,
        val friendCount: Int
    )

    /**
     * Creates an encrypted backup file (.spendtag) and returns its File.
     */
    suspend fun createEncryptedBackup(context: Context, database: SpendTagDatabase): File = withContext(Dispatchers.IO) {
        val payments = database.paymentDao().getAllPayments().first()
        val counterparties = database.counterpartyDao().getAllCounterparties().first()
        val friends = database.friendDao().getAllFriends().first()

        val json = JSONObject().apply {
            put("version", 1)
            put("createdAt", System.currentTimeMillis())

            // Payments
            val paymentsArray = JSONArray()
            for (p in payments) {
                paymentsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("amountPaise", p.amountPaise)
                    put("direction", p.direction.name)
                    put("upiId", p.upiId)
                    put("refNo", p.refNo)
                    put("source", p.source.name)
                    put("createdAt", p.createdAt)
                    put("categoryId", p.categoryId)
                    put("myShare", p.myShare)
                    put("status", p.status.name)
                    put("note", p.note)
                })
            }
            put("payments", paymentsArray)

            // Counterparties
            val cpArray = JSONArray()
            for (c in counterparties) {
                cpArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("upiId", c.upiId)
                    put("alias", c.alias)
                    put("defaultCategoryId", c.defaultCategoryId)
                    put("lastUsedAt", c.lastUsedAt)
                })
            }
            put("counterparties", cpArray)

            // Friends
            val friendsArray = JSONArray()
            for (f in friends) {
                friendsArray.put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("upiId", f.upiId)
                    put("phoneNumber", f.phoneNumber)
                    put("createdAt", f.createdAt)
                })
            }
            put("friends", friendsArray)
        }

        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
        val encryptedBytes = CryptoManager.encrypt(jsonBytes)

        val todayStr = LocalDate.now(DateTimeUtils.IndianZoneId).toString().replace("-", "_")
        val filename = "SpendTag_Encrypted_Backup_$todayStr.spendtag"

        val exportDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val targetFile = File(exportDir, filename)

        FileOutputStream(targetFile).use { it.write(encryptedBytes) }
        targetFile
    }

    /**
     * Creates an Android share intent to export the encrypted backup file.
     */
    fun createBackupShareIntent(context: Context, backupFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SpendTag Encrypted Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Decrypts and restores an encrypted backup into the Room database atomically.
     */
    suspend fun restoreFromBackup(
        backupFile: File,
        database: SpendTagDatabase
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val encryptedBytes = FileInputStream(backupFile).use { it.readBytes() }
            val decryptedBytes = CryptoManager.decrypt(encryptedBytes)
            val json = JSONObject(String(decryptedBytes, Charsets.UTF_8))

            val version = json.getInt("version")
            val createdAt = json.getLong("createdAt")

            val paymentsArray = json.getJSONArray("payments")
            val cpArray = json.getJSONArray("counterparties")
            val friendsArray = json.getJSONArray("friends")

            database.runInTransaction {
                // Restore Counterparties
                for (i in 0 until cpArray.length()) {
                    val obj = cpArray.getJSONObject(i)
                    database.openHelper.writableDatabase.execSQL(
                        """
                        INSERT OR REPLACE INTO counterparties (id, upiId, alias, defaultCategoryId, lastUsedAt)
                        VALUES (${obj.getLong("id")}, '${obj.getString("upiId").replace("'", "''")}', '${obj.getString("alias").replace("'", "''")}', ${if (obj.isNull("defaultCategoryId")) "NULL" else obj.getLong("defaultCategoryId")}, ${obj.getLong("lastUsedAt")})
                        """.trimIndent()
                    )
                }

                // Restore Friends
                for (i in 0 until friendsArray.length()) {
                    val obj = friendsArray.getJSONObject(i)
                    val upiVal = if (obj.isNull("upiId")) "NULL" else "'${obj.getString("upiId").replace("'", "''")}'"
                    val phoneVal = if (obj.isNull("phoneNumber")) "NULL" else "'${obj.getString("phoneNumber").replace("'", "''")}'"
                    database.openHelper.writableDatabase.execSQL(
                        """
                        INSERT OR REPLACE INTO friends (id, name, upiId, phoneNumber, createdAt)
                        VALUES (${obj.getLong("id")}, '${obj.getString("name").replace("'", "''")}', $upiVal, $phoneVal, ${obj.getLong("createdAt")})
                        """.trimIndent()
                    )
                }

                // Restore Payments
                for (i in 0 until paymentsArray.length()) {
                    val obj = paymentsArray.getJSONObject(i)
                    val upiVal = if (obj.isNull("upiId")) "NULL" else "'${obj.getString("upiId").replace("'", "''")}'"
                    val refVal = if (obj.isNull("refNo")) "NULL" else "'${obj.getString("refNo").replace("'", "''")}'"
                    val catVal = if (obj.isNull("categoryId")) "NULL" else obj.getLong("categoryId").toString()
                    val noteVal = if (obj.isNull("note")) "NULL" else "'${obj.getString("note").replace("'", "''")}'"

                    database.openHelper.writableDatabase.execSQL(
                        """
                        INSERT OR REPLACE INTO payments (id, amountPaise, direction, upiId, refNo, source, createdAt, categoryId, myShare, status, note)
                        VALUES (${obj.getLong("id")}, ${obj.getLong("amountPaise")}, '${obj.getString("direction")}', $upiVal, $refVal, '${obj.getString("source")}', ${obj.getLong("createdAt")}, $catVal, ${obj.getLong("myShare")}, '${obj.getString("status")}', $noteVal)
                        """.trimIndent()
                    )
                }
            }

            Result.success(
                BackupMetadata(
                    version = version,
                    createdAt = createdAt,
                    paymentCount = paymentsArray.length(),
                    friendCount = friendsArray.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
