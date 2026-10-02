package com.spendtag.app.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Standard AES-256 GCM authenticated encryption engine for SpendTag.
 * Provides authenticated encryption ensuring both privacy and tamper detection.
 */
object LocalEncryptionEngine {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    data class EncryptedPayload(
        val iv: ByteArray,
        val ciphertext: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedPayload
            return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + ciphertext.contentHashCode()
            return result
        }
    }

    /**
     * Encrypts plaintext bytes using AES-256 GCM with a cryptographically secure random IV.
     */
    fun encrypt(plaintext: ByteArray, secretKey: SecretKey): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }

    /**
     * Decrypts ciphertext bytes verifying GCM authentication tag.
     * Throws an exception if data has been tampered with or corrupted.
     */
    fun decrypt(payload: EncryptedPayload, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(payload.ciphertext)
    }

    /**
     * Serializes IV + ciphertext into a single byte stream.
     */
    fun pack(payload: EncryptedPayload): ByteArray {
        val packed = ByteArray(GCM_IV_LENGTH_BYTES + payload.ciphertext.size)
        System.arraycopy(payload.iv, 0, packed, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(payload.ciphertext, 0, packed, GCM_IV_LENGTH_BYTES, payload.ciphertext.size)
        return packed
    }

    /**
     * Unpacks a combined byte stream into IV and ciphertext.
     */
    fun unpack(packedBytes: ByteArray): EncryptedPayload {
        require(packedBytes.size > GCM_IV_LENGTH_BYTES) { "Corrupt encrypted payload: insufficient bytes" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(packedBytes, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val ciphertextSize = packedBytes.size - GCM_IV_LENGTH_BYTES
        val ciphertext = ByteArray(ciphertextSize)
        System.arraycopy(packedBytes, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertextSize)

        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }
}
