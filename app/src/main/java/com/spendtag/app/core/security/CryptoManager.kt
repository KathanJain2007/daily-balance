package com.spendtag.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Android KeyStore Master Key Manager.
 * Stores SpendTag's root encryption key in the device's hardware Secure Element / TEE.
 */
object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "spendtag_master_key_v1"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Retrieves or generates the 256-bit AES master key inside Android KeyStore.
     */
    fun getOrCreateMasterKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts arbitrary bytes using hardware-backed master key.
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val key = getOrCreateMasterKey()
        val payload = LocalEncryptionEngine.encrypt(plaintext, key)
        return LocalEncryptionEngine.pack(payload)
    }

    /**
     * Decrypts packed bytes using hardware-backed master key.
     */
    fun decrypt(packedBytes: ByteArray): ByteArray {
        val key = getOrCreateMasterKey()
        val payload = LocalEncryptionEngine.unpack(packedBytes)
        return LocalEncryptionEngine.decrypt(payload, key)
    }
}
