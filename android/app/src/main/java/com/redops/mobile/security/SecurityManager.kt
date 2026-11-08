package com.redops.mobile.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

class SecurityManager(private val context: Context?) {

    private val keyStoreAlias = "RedOps_Master_Key"
    private val gcmTransformation = "AES/GCM/NoPadding"
    private val secureRandom = SecureRandom()

    // PIN PBKDF2 configuration
    private val pbkdf2Iterations = 12000
    private val pbkdf2KeyLength = 256
    private val saltSizeBytes = 16

    // Throttling configuration
    private var failedPinAttempts = 0
    private var lockoutUntilTimestamp = 0L
    private val maxFailedAttempts = 5
    private val cooldownDurationMs = 30000L // 30 seconds

    // In-memory fallback key for testing environments where AndroidKeyStore is not present
    private var softwareFallbackKey: SecretKey? = null

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyStoreAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyStoreAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // AndroidKeyStore is not available in unit testing JVM runtime; use a secure ephemeral software key
            if (softwareFallbackKey == null) {
                val rawBytes = ByteArray(32)
                secureRandom.nextBytes(rawBytes)
                softwareFallbackKey = SecretKeySpec(rawBytes, "AES")
            }
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(keyStoreAlias)) {
                (keyStore.getKey(keyStoreAlias, null) as? SecretKey) ?: getSoftwareFallbackKey()
            } else {
                getSoftwareFallbackKey()
            }
        } catch (_: Exception) {
            getSoftwareFallbackKey()
        }
    }

    private fun getSoftwareFallbackKey(): SecretKey {
        if (softwareFallbackKey == null) {
            val rawBytes = ByteArray(32)
            secureRandom.nextBytes(rawBytes)
            softwareFallbackKey = SecretKeySpec(rawBytes, "AES")
        }
        return softwareFallbackKey!!
    }

    // ==========================================
    // 1. PIN Key Derivation & Safe Verification
    // ==========================================

    fun hashPin(pin: String): String {
        val salt = ByteArray(saltSizeBytes)
        secureRandom.nextBytes(salt)

        val spec = PBEKeySpec(pin.toCharArray(), salt, pbkdf2Iterations, pbkdf2KeyLength)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded

        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return "v2:$saltB64:$pbkdf2Iterations:$hashB64"
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false

        // Check if currently locked out
        if (isLockedOut()) {
            return false
        }

        val isValid = if (storedHash.startsWith("v2:")) {
            verifyPbkdf2Pin(pin, storedHash)
        } else {
            // Legacy single-iteration SHA-256 backward compatibility
            verifyLegacyPin(pin, storedHash)
        }

        if (isValid) {
            recordSuccessfulAttempt()
        } else {
            recordFailedAttempt()
        }

        return isValid
    }

    private fun verifyPbkdf2Pin(pin: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 4) return false

            val salt = Base64.getDecoder().decode(parts[1])
            val iterations = parts[2].toIntOrNull() ?: pbkdf2Iterations
            val expectedHash = Base64.getDecoder().decode(parts[3])

            val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, expectedHash.size * 8)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val computedHash = skf.generateSecret(spec).encoded

            // Constant-time byte comparison prevents timing attacks
            MessageDigest.isEqual(computedHash, expectedHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyLegacyPin(pin: String, storedHash: String): Boolean {
        val legacySalt = "RedOps_Secure_Salt_2026"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((pin + legacySalt).toByteArray(Charsets.UTF_8))
        val computedHex = bytes.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(computedHex.toByteArray(), storedHash.toByteArray())
    }

    // ==========================================
    // 2. Attempt Throttling & Cooldown
    // ==========================================

    @Synchronized
    fun isLockedOut(): Boolean {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilTimestamp) {
            return true
        }
        if (lockoutUntilTimestamp != 0L && now >= lockoutUntilTimestamp) {
            // Cooldown expired; reset attempt counter
            lockoutUntilTimestamp = 0L
            failedPinAttempts = 0
        }
        return false
    }

    @Synchronized
    fun getRemainingLockoutSeconds(): Long {
        val now = System.currentTimeMillis()
        val diff = lockoutUntilTimestamp - now
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }

    @Synchronized
    fun getFailedAttempts(): Int = failedPinAttempts

    @Synchronized
    fun recordFailedAttempt() {
        failedPinAttempts++
        if (failedPinAttempts >= maxFailedAttempts) {
            val multiplier = (failedPinAttempts - maxFailedAttempts + 1).coerceAtMost(4)
            lockoutUntilTimestamp = System.currentTimeMillis() + (cooldownDurationMs * multiplier)
        }
    }

    @Synchronized
    fun recordSuccessfulAttempt() {
        failedPinAttempts = 0
        lockoutUntilTimestamp = 0L
    }

    @Synchronized
    fun resetAttempts() {
        failedPinAttempts = 0
        lockoutUntilTimestamp = 0L
    }

    // ==========================================
    // 3. Biometric Authentication
    // ==========================================

    fun getBiometricStatus(): BiometricStatus {
        if (context == null) return BiometricStatus.UNAVAILABLE
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun canAuthenticateBiometrics(): Boolean {
        return getBiometricStatus() == BiometricStatus.AVAILABLE
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "RedOps Security Verification",
        subtitle: String = "Authenticate to access authorized assessment records",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                recordSuccessfulAttempt()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                recordFailedAttempt()
                onError("Biometric authentication failed. Please try again.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    // ==========================================
    // 4. Authenticated AES-256-GCM Keystore Encryption
    // ==========================================

    fun encryptData(plainBytes: ByteArray): ByteArray {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(gcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12-byte GCM IV
        val cipherText = cipher.doFinal(plainBytes)

        val buffer = ByteBuffer.allocate(4 + iv.size + cipherText.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(cipherText)
        return buffer.array()
    }

    fun decryptData(encryptedBytes: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(encryptedBytes)
        val ivSize = buffer.int
        if (ivSize <= 0 || ivSize > 32) {
            throw IllegalArgumentException("Invalid IV size in encrypted payload.")
        }
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val key = getSecretKey()
        val cipher = Cipher.getInstance(gcmTransformation)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(cipherText)
    }

    // String convenience methods
    fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(gcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(iv, encrypted)
    }

    fun decrypt(iv: ByteArray, cipherText: ByteArray): String {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(gcmTransformation)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    // ==========================================
    // 5. Password-Protected Backup Encryption & Integrity
    // ==========================================

    private val backupMagicHeader = "REDOPS_ENC_V1".toByteArray(Charsets.UTF_8)
    private val backupIterations = 15000

    fun encryptBackup(plainJson: String, password: String): ByteArray {
        if (password.length < 6) {
            throw IllegalArgumentException("Backup password must be at least 6 characters.")
        }

        val salt = ByteArray(saltSizeBytes)
        secureRandom.nextBytes(salt)

        // Derive 256-bit AES key from password via PBKDF2WithHmacSHA256
        val spec = PBEKeySpec(password.toCharArray(), salt, backupIterations, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = skf.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(gcmTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv // 12-byte GCM IV
        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val outputBuffer = ByteBuffer.allocate(backupMagicHeader.size + salt.size + 4 + iv.size + cipherText.size)
        outputBuffer.put(backupMagicHeader)
        outputBuffer.put(salt)
        outputBuffer.putInt(iv.size)
        outputBuffer.put(iv)
        outputBuffer.put(cipherText)
        return outputBuffer.array()
    }

    fun decryptBackup(encryptedBytes: ByteArray, password: String): String {
        if (password.isBlank()) {
            throw IllegalArgumentException("Please provide the backup password.")
        }

        // Validate Header Magic
        if (encryptedBytes.size < backupMagicHeader.size + saltSizeBytes + 16) {
            throw IllegalArgumentException("The selected file is too small to be a valid RedOps encrypted backup.")
        }

        val buffer = ByteBuffer.wrap(encryptedBytes)
        val magic = ByteArray(backupMagicHeader.size)
        buffer.get(magic)

        if (!MessageDigest.isEqual(magic, backupMagicHeader)) {
            // Check if it's an unencrypted legacy JSON backup
            val text = String(encryptedBytes, Charsets.UTF_8)
            if (text.trimStart().startsWith("{") && text.contains("engagements")) {
                return text
            }
            throw IllegalArgumentException("Invalid backup archive format. Header magic does not match REDOPS_ENC_V1.")
        }

        val salt = ByteArray(saltSizeBytes)
        buffer.get(salt)

        val ivSize = buffer.int
        if (ivSize <= 0 || ivSize > 32) {
            throw IllegalArgumentException("Corrupted IV length in backup archive.")
        }
        val iv = ByteArray(ivSize)
        buffer.get(iv)

        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        // Derive AES key with PBKDF2
        val spec = PBEKeySpec(password.toCharArray(), salt, backupIterations, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = skf.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        try {
            val cipher = Cipher.getInstance(gcmTransformation)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedBytes = cipher.doFinal(cipherText)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: javax.crypto.AEADBadTagException) {
            throw SecurityException("Incorrect backup password or the backup file has been tampered with.")
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt backup: ${e.message}")
        }
    }
}
