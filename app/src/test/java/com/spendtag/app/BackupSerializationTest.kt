package com.spendtag.app

import com.spendtag.app.core.security.LocalEncryptionEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.crypto.KeyGenerator

/**
 * Unit tests verifying database backup payload JSON structure,
 * serialization fidelity, and round-trip encrypted restoration.
 */
class BackupSerializationTest {

    @Test
    fun backupPayload_serializesAndRestoresCorrectly() {
        val originalJson = JSONObject().apply {
            put("version", 1)
            put("createdAt", 1728734400000L)

            val paymentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1L)
                    put("amountPaise", 450000L) // Rs 4,500
                    put("direction", "OUT")
                    put("upiId", "dinner@icici")
                    put("refNo", "REF48192831")
                    put("source", "SMS")
                    put("createdAt", 1728734400000L)
                    put("categoryId", 1L)
                    put("myShare", 150000L) // Rs 1,500 personal share
                    put("status", "TAGGED")
                    put("note", "Team Dinner")
                })
            }
            put("payments", paymentsArray)

            val friendsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 10L)
                    put("name", "Rahul")
                    put("upiId", "rahul@okaxis")
                    put("phoneNumber", "9876543210")
                    put("createdAt", 1728734400000L)
                })
            }
            put("friends", friendsArray)
        }

        // 1. Generate test AES-256 Key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        // 2. Encrypt & pack
        val jsonBytes = originalJson.toString().toByteArray(Charsets.UTF_8)
        val encryptedPayload = LocalEncryptionEngine.encrypt(jsonBytes, secretKey)
        val packedBytes = LocalEncryptionEngine.pack(encryptedPayload)

        assertNotNull(packedBytes)

        // 3. Unpack & decrypt
        val unpackedPayload = LocalEncryptionEngine.unpack(packedBytes)
        val decryptedBytes = LocalEncryptionEngine.decrypt(unpackedPayload, secretKey)
        val restoredJson = JSONObject(String(decryptedBytes, Charsets.UTF_8))

        // 4. Verify fields
        assertEquals(1, restoredJson.getInt("version"))
        val restoredPayments = restoredJson.getJSONArray("payments")
        assertEquals(1, restoredPayments.length())

        val p = restoredPayments.getJSONObject(0)
        assertEquals(450000L, p.getLong("amountPaise"))
        assertEquals(150000L, p.getLong("myShare"))
        assertEquals("Team Dinner", p.getString("note"))

        val restoredFriends = restoredJson.getJSONArray("friends")
        val f = restoredFriends.getJSONObject(0)
        assertEquals("Rahul", f.getString("name"))
        assertEquals("rahul@okaxis", f.getString("upiId"))
    }
}
