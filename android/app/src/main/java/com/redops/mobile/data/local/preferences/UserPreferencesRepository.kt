package com.redops.mobile.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "redops_user_preferences")

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val isBiometricEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val pinHash: String = "",
    val autoLockEnabled: Boolean = true,
    val inactivityTimeoutMinutes: Int = 0,
    val screenshotProtectionEnabled: Boolean = false,
    val apiBaseUrl: String = "http://10.0.2.2:5000/api/v1/",
    val authToken: String = ""
)

open class UserPreferencesRepository(private val context: Context?) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val AUTO_LOCK = booleanPreferencesKey("auto_lock")
        val INACTIVITY_TIMEOUT = intPreferencesKey("inactivity_timeout")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    open val userPreferencesFlow: Flow<UserPreferences> = context?.userDataStore?.data
        ?.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        ?.map { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
            val theme = try {
                ThemeMode.valueOf(themeStr)
            } catch (_: Exception) {
                ThemeMode.DARK
            }
            val biometric = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
            val pin = preferences[PreferencesKeys.PIN_ENABLED] ?: false
            val pinHash = preferences[PreferencesKeys.PIN_HASH] ?: ""
            val autoLock = preferences[PreferencesKeys.AUTO_LOCK] ?: true
            val timeout = preferences[PreferencesKeys.INACTIVITY_TIMEOUT] ?: 0
            val screenshotProtection = preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false
            val apiBaseUrl = preferences[PreferencesKeys.API_BASE_URL] ?: "http://10.0.2.2:5000/api/v1/"
            val authToken = preferences[PreferencesKeys.AUTH_TOKEN] ?: ""

            UserPreferences(
                themeMode = theme,
                isBiometricEnabled = biometric,
                isPinEnabled = pin,
                pinHash = pinHash,
                autoLockEnabled = autoLock,
                inactivityTimeoutMinutes = timeout,
                screenshotProtectionEnabled = screenshotProtection,
                apiBaseUrl = apiBaseUrl,
                authToken = authToken
            )
        } ?: emptyFlow()

    open suspend fun setThemeMode(mode: ThemeMode) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    open suspend fun setBiometricEnabled(enabled: Boolean) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    open suspend fun setPin(enabled: Boolean, hash: String) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.PIN_ENABLED] = enabled
            preferences[PreferencesKeys.PIN_HASH] = hash
        }
    }

    open suspend fun setAutoLockEnabled(enabled: Boolean) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK] = enabled
        }
    }

    open suspend fun setInactivityTimeoutMinutes(minutes: Int) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.INACTIVITY_TIMEOUT] = minutes
        }
    }

    open suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION] = enabled
        }
    }

    open suspend fun setApiBaseUrl(url: String) {
        val sanitized = if (!url.endsWith("/")) "$url/" else url
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.API_BASE_URL] = sanitized
        }
    }

    open suspend fun setAuthToken(token: String) {
        context?.userDataStore?.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }
    }

    open suspend fun clearAll() {
        context?.userDataStore?.edit { preferences ->
            preferences.clear()
        }
    }
}
