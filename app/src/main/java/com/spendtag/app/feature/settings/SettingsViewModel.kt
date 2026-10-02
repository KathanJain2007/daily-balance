package com.spendtag.app.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendtag.app.core.backup.BackupManager
import com.spendtag.app.core.database.SpendTagDatabase
import com.spendtag.app.core.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val database: SpendTagDatabase
) : ViewModel() {

    private val _isAppLockEnabled = MutableStateFlow(preferencesManager.isAppLockEnabled)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isReminderEnabled = MutableStateFlow(preferencesManager.isReminderEnabled)
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    private val _isDigestEnabled = MutableStateFlow(preferencesManager.isDigestEnabled)
    val isDigestEnabled: StateFlow<Boolean> = _isDigestEnabled.asStateFlow()

    private val _monthlyBudgetPaise = MutableStateFlow(preferencesManager.monthlyBudgetPaise)
    val monthlyBudgetPaise: StateFlow<Long> = _monthlyBudgetPaise.asStateFlow()

    private val _userName = MutableStateFlow(preferencesManager.userName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userUpiId = MutableStateFlow(preferencesManager.userUpiId)
    val userUpiId: StateFlow<String> = _userUpiId.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        preferencesManager.isAppLockEnabled = enabled
        _isAppLockEnabled.value = enabled
    }

    fun setReminderEnabled(enabled: Boolean) {
        preferencesManager.isReminderEnabled = enabled
        _isReminderEnabled.value = enabled
    }

    fun setDigestEnabled(enabled: Boolean) {
        preferencesManager.isDigestEnabled = enabled
        _isDigestEnabled.value = enabled
    }

    fun setMonthlyBudget(budgetPaise: Long) {
        preferencesManager.monthlyBudgetPaise = budgetPaise
        _monthlyBudgetPaise.value = budgetPaise
    }

    fun saveProfile(name: String, upiId: String) {
        preferencesManager.userName = name.trim()
        preferencesManager.userUpiId = upiId.trim()
        _userName.value = name.trim()
        _userUpiId.value = upiId.trim()
    }

    fun exportBackup(context: Context, onReadyToShare: (Intent) -> Unit) {
        viewModelScope.launch {
            val backupFile = BackupManager.createEncryptedBackup(context, database)
            val shareIntent = BackupManager.createBackupShareIntent(context, backupFile)
            onReadyToShare(shareIntent)
        }
    }

    fun restoreBackup(backupFile: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.restoreFromBackup(backupFile, database)
            result.fold(
                onSuccess = { meta ->
                    onResult(true, "Successfully restored ${meta.paymentCount} payments and ${meta.friendCount} friends.")
                },
                onFailure = { error ->
                    onResult(false, "Restore failed: ${error.message}")
                }
            )
        }
    }
}
