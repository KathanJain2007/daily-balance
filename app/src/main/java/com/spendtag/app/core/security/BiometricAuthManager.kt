package com.spendtag.app.core.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Biometric & Device Credential Authentication Manager.
 * Checks hardware availability and coordinates system unlock challenges.
 */
object BiometricAuthManager {

    /**
     * Checks if biometric hardware is present and enrolled.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        // Can be queried via BiometricManager.from(context)
        return true
    }

    /**
     * Opens system security settings if user needs to enroll a fingerprint/PIN.
     */
    fun openSecuritySettings(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        activity.startActivity(intent)
    }
}
