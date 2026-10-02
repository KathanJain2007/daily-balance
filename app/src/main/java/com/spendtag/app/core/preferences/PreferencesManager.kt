package com.spendtag.app.core.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight local preferences manager for onboarding status and user settings.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var isDigestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGEST_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DIGEST_ENABLED, value).apply()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var monthlyBudgetPaise: Long
        get() = prefs.getLong(KEY_MONTHLY_BUDGET, 3000000L) // Default ₹30,000
        set(value) = prefs.edit().putLong(KEY_MONTHLY_BUDGET, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Me") ?: "Me"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userUpiId: String
        get() = prefs.getString(KEY_USER_UPI_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_UPI_ID, value).apply()

    companion object {
        private const val PREFS_NAME = "spendtag_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_DIGEST_ENABLED = "digest_enabled"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget_paise"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_UPI_ID = "user_upi_id"
    }
}
