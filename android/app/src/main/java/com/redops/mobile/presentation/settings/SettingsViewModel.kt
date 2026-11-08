package com.redops.mobile.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.redops.mobile.data.local.preferences.ThemeMode
import com.redops.mobile.data.local.preferences.UserPreferences
import com.redops.mobile.data.local.preferences.UserPreferencesRepository
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.model.ReportHistory
import com.redops.mobile.domain.repository.*
import com.redops.mobile.security.BiometricStatus
import com.redops.mobile.security.SecurityManager
import com.redops.mobile.utils.SampleDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupPayload(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val engagements: List<Engagement> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val notes: List<NotebookEntry> = emptyList(),
    val evidence: List<EvidenceRecord> = emptyList(),
    val reports: List<ReportHistory> = emptyList()
)

enum class RestoreStrategy {
    REPLACE_ALL,
    MERGE_RECORDS
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val securityManager: SecurityManager,
    private val engagementRepository: EngagementRepository,
    private val findingRepository: FindingRepository,
    private val notebookRepository: NotebookRepository,
    private val evidenceRepository: EvidenceRepository,
    private val dashboardRepository: DashboardRepository,
    private val reportHistoryRepository: ReportHistoryRepository? = null
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _pendingRestorePayload = MutableStateFlow<BackupPayload?>(null)
    val pendingRestorePayload = _pendingRestorePayload.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun clearPendingRestore() {
        _pendingRestorePayload.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !canUseBiometrics()) {
                val status = securityManager.getBiometricStatus()
                _message.value = when (status) {
                    BiometricStatus.NOT_ENROLLED -> "No biometrics enrolled. Please set up fingerprint or face unlock in system settings."
                    BiometricStatus.NO_HARDWARE -> "Biometric hardware is not available on this device."
                    else -> "Biometrics unavailable on this device."
                }
                userPreferencesRepository.setBiometricEnabled(false)
                return@launch
            }
            userPreferencesRepository.setBiometricEnabled(enabled)
            _message.value = if (enabled) "Biometric authentication enabled." else "Biometric authentication disabled."
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            val hash = securityManager.hashPin(pin)
            userPreferencesRepository.setPin(true, hash)
            securityManager.recordSuccessfulAttempt()
            _message.value = "PIN lock configured with PBKDF2 key derivation."
        }
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val currentHash = preferences.value.pinHash
        if (!securityManager.verifyPin(oldPin, currentHash)) {
            _message.value = "Current PIN is incorrect. Failed attempts: ${securityManager.getFailedAttempts()}"
            return false
        }
        setPin(newPin)
        return true
    }

    fun removePin() {
        viewModelScope.launch {
            userPreferencesRepository.setPin(false, "")
            securityManager.resetAttempts()
            _message.value = "PIN lock removed."
        }
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoLockEnabled(enabled)
            _message.value = if (enabled) "Auto-lock on background enabled." else "Auto-lock disabled."
        }
    }

    fun setInactivityTimeout(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setInactivityTimeoutMinutes(minutes)
        }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setScreenshotProtectionEnabled(enabled)
            _message.value = if (enabled) "Screenshot protection enabled (FLAG_SECURE active)." else "Screenshot protection disabled."
        }
    }

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            userPreferencesRepository.setApiBaseUrl(url)
            _message.value = "Toolkit API Base URL updated."
        }
    }

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAuthToken(token)
            _message.value = "Operator Session Token updated."
        }
    }

    fun canUseBiometrics(): Boolean {
        return securityManager.canAuthenticateBiometrics()
    }

    fun getBiometricStatus(): BiometricStatus {
        return securityManager.getBiometricStatus()
    }

    fun loadSampleData() {
        viewModelScope.launch {
            SampleDataGenerator.populateSampleData(
                engagementRepository,
                findingRepository,
                notebookRepository,
                evidenceRepository,
                dashboardRepository
            )
            _message.value = "Sample university evaluation dataset loaded successfully."
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            engagementRepository.clearAll()
            findingRepository.clearAll()
            notebookRepository.clearAll()
            evidenceRepository.clearAll()
            reportHistoryRepository?.clearAll()
            dashboardRepository.clearActivities()
            _message.value = "All assessment records securely erased from device."
        }
    }

    // ========================================================
    // Authenticated Encrypted Backup & Restore Implementation
    // ========================================================

    fun exportEncryptedBackup(context: Context, destinationUri: Uri, password: String) {
        viewModelScope.launch {
            try {
                if (password.length < 6) {
                    _message.value = "Backup password must be at least 6 characters."
                    return@launch
                }

                val engs = engagementRepository.getAllEngagements().first()
                val fnds = findingRepository.getAllFindings().first()
                val nts = notebookRepository.getAllEntries().first()
                val evs = evidenceRepository.getAllEvidence().first()
                val reps = reportHistoryRepository?.getAllReports()?.first() ?: emptyList()

                val payload = BackupPayload(
                    engagements = engs,
                    findings = fnds,
                    notes = nts,
                    evidence = evs,
                    reports = reps
                )

                val json = Gson().toJson(payload)
                val encryptedBytes = withContext(Dispatchers.Default) {
                    securityManager.encryptBackup(json, password)
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        out.write(encryptedBytes)
                    }
                }

                dashboardRepository.logActivity("Encrypted Backup Exported", "SECURITY", "EXPORTED")
                _message.value = "Encrypted backup successfully exported (${encryptedBytes.size / 1024} KB). Password protection active."
            } catch (e: Exception) {
                _message.value = "Backup export failed: ${e.message}"
            }
        }
    }

    fun inspectBackup(context: Context, sourceUri: Uri, password: String) {
        viewModelScope.launch {
            try {
                val encryptedBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytes()
                    } ?: throw IllegalArgumentException("Could not read backup file from device.")
                }

                val decryptedJson = withContext(Dispatchers.Default) {
                    securityManager.decryptBackup(encryptedBytes, password)
                }

                val payload = Gson().fromJson(decryptedJson, BackupPayload::class.java)
                if (payload == null || (payload.engagements.isEmpty() && payload.findings.isEmpty() && payload.notes.isEmpty())) {
                    _message.value = "The backup file is valid but contains no assessment records."
                    return@launch
                }

                _pendingRestorePayload.value = payload
            } catch (e: SecurityException) {
                _message.value = e.message ?: "Authentication failed: Incorrect backup password."
            } catch (e: Exception) {
                _message.value = "Failed to inspect backup: ${e.message}"
            }
        }
    }

    fun confirmRestore(strategy: RestoreStrategy) {
        val payload = _pendingRestorePayload.value ?: return
        viewModelScope.launch {
            try {
                if (strategy == RestoreStrategy.REPLACE_ALL) {
                    engagementRepository.clearAll()
                    findingRepository.clearAll()
                    notebookRepository.clearAll()
                    evidenceRepository.clearAll()
                    reportHistoryRepository?.clearAll()
                }

                payload.engagements.forEach { engagementRepository.saveEngagement(it) }
                payload.findings.forEach { findingRepository.saveFinding(it) }
                payload.notes.forEach { notebookRepository.saveEntry(it) }
                payload.evidence.forEach { evidenceRepository.saveEvidence(it) }
                payload.reports.forEach { reportHistoryRepository?.recordReport(it) }

                dashboardRepository.logActivity(
                    "Database Restored (${strategy.name})",
                    "SECURITY",
                    "RESTORED"
                )

                _message.value = "Successfully restored ${payload.engagements.size} engagements, ${payload.findings.size} findings, and ${payload.notes.size} notes."
                _pendingRestorePayload.value = null
            } catch (e: Exception) {
                _message.value = "Restore operation failed: ${e.message}"
            }
        }
    }

    fun exportBackup(context: Context, destinationUri: Uri) {
        exportEncryptedBackup(context, destinationUri, "RedOps_Default_2026")
    }

    fun restoreBackup(context: Context, sourceUri: Uri) {
        inspectBackup(context, sourceUri, "RedOps_Default_2026")
    }
}
