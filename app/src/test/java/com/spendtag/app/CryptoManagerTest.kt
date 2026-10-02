package com.spendtag.app

import com.spendtag.app.core.security.LocalEncryptionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Unit tests verifying AES-256 GCM authenticated encryption,
 * initialization vector (IV) uniqueness, and cryptographic tamper detection.
 */
class CryptoManagerTest {

    private fun generateTestKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    @Test
    fun encryptAndDecrypt_recoversOriginalPlaintext() {
        val key = generateTestKey()
        val originalText = "SpendTag Financial Backup: Sensitive UPI and Transaction History"

        val encryptedPayload = LocalEncryptionEngine.encrypt(originalText.toByteArray(Charsets.UTF_8), key)
        val decryptedBytes = LocalEncryptionEngine.decrypt(encryptedPayload, key)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun encrypt_producesNonDeterministicCiphertextViaRandomIv() {
        val key = generateTestKey()
        val originalText = "Constant sensitive string"

        val payload1 = LocalEncryptionEngine.encrypt(originalText.toByteArray(Charsets.UTF_8), key)
        val payload2 = LocalEncryptionEngine.encrypt(originalText.toByteArray(Charsets.UTF_8), key)

        // AES-GCM must use a unique 12-byte IV for every encryption
        assertNotEquals(payload1.iv.toList(), payload2.iv.toList())
        assertNotEquals(payload1.ciphertext.toList(), payload2.ciphertext.toList())

        // Both decrypt to identical original text
        assertEquals(originalText, String(LocalEncryptionEngine.decrypt(payload1, key), Charsets.UTF_8))
        assertEquals(originalText, String(LocalEncryptionEngine.decrypt(payload2, key), Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun decrypt_throwsExceptionWhenCiphertextIsTampered() {
        val key = generateTestKey()
        val originalText = "Authentic data"

        val payload = LocalEncryptionEngine.encrypt(originalText.toByteArray(Charsets.UTF_8), key)

        // Tamper with the first byte of ciphertext
        val tamperedCiphertext = payload.ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        val tamperedPayload = LocalEncryptionEngine.EncryptedPayload(
            iv = payload.iv,
            ciphertext = tamperedCiphertext
        )

        // GCM Authentication Tag verification MUST fail
        LocalEncryptionEngine.decrypt(tamperedPayload, key)
    }
}
