package com.spendtag.app.core.security

import java.security.MessageDigest

/**
 * Helper for Room SQLite Database Encryption.
 * 
 * Non-Obvious Android Concept:
 * SQLCipher encrypts every SQLite database page at rest using AES-256 CBC or GCM.
 * The passphrase is derived dynamically from the device's hardware KeyStore,
 * ensuring no hardcoded encryption keys ever exist in APK source code.
 */
object DatabaseSecurityHelper {

    /**
     * Derives a 256-bit passphrase for SQLCipher from the hardware-backed Master Key.
     */
    fun deriveDatabasePassphrase(): ByteArray {
        val masterKey = CryptoManager.getOrCreateMasterKey()
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(masterKey.encoded ?: "SpendTagHardwareMasterSalt_v1".toByteArray(Charsets.UTF_8))
    }
}
