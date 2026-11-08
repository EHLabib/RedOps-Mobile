package com.redops.mobile.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecurityManagerTest {

    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        securityManager = SecurityManager(null)
    }

    @Test
    fun hashPin_producesPbkdf2HashWithSaltAndIterations() {
        val pin = "4321"
        val hash = securityManager.hashPin(pin)

        assertTrue(hash.startsWith("v2:"))
        val parts = hash.split(":")
        assertEquals(4, parts.size)
        assertEquals("v2", parts[0])
        assertTrue("Salt should be non-empty base64", parts[1].isNotBlank())
        assertEquals("12000", parts[2])
        assertTrue("Hash should be non-empty base64", parts[3].isNotBlank())
    }

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val pin = "8899"
        val hash = securityManager.hashPin(pin)

        val result = securityManager.verifyPin(pin, hash)
        assertTrue(result)
        assertEquals(0, securityManager.getFailedAttempts())
        assertFalse(securityManager.isLockedOut())
    }

    @Test
    fun verifyPin_incorrectPin_returnsFalseAndIncrementsFailures() {
        val correctPin = "1234"
        val wrongPin = "9999"
        val hash = securityManager.hashPin(correctPin)

        val result = securityManager.verifyPin(wrongPin, hash)
        assertFalse(result)
        assertEquals(1, securityManager.getFailedAttempts())
    }

    @Test
    fun verifyPin_legacySha256Hash_returnsTrueForBackwardCompatibility() {
        // Pre-calculated legacy hash for "1337" + "RedOps_Secure_Salt_2026"
        val legacyHash = "3db89d538eec7bf099a805f31952a26569107936fe7cece08dfd54ba85c490a0"
        val legacySalt = "RedOps_Secure_Salt_2026"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val expected = md.digest(("1337" + legacySalt).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val result = securityManager.verifyPin("1337", expected)
        assertTrue(result)
    }

    @Test
    fun verifyPin_throttlesAfterFiveConsecutiveFailures() {
        val hash = securityManager.hashPin("5555")

        repeat(5) {
            securityManager.verifyPin("0000", hash)
        }

        assertEquals(5, securityManager.getFailedAttempts())
        assertTrue(securityManager.isLockedOut())
        assertTrue(securityManager.getRemainingLockoutSeconds() > 0)

        // Even with the correct PIN, verification fails during active lockout
        assertFalse(securityManager.verifyPin("5555", hash))

        // Reset clears lockout
        securityManager.resetAttempts()
        assertFalse(securityManager.isLockedOut())
        assertTrue(securityManager.verifyPin("5555", hash))
    }

    @Test
    fun encryptAndDecryptData_preservesOriginalBytes() {
        val originalText = "Top-Secret-Pentest-Observation-Data-2026"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = securityManager.encryptData(originalBytes)
        assertFalse(originalBytes.contentEquals(encrypted))

        val decrypted = securityManager.decryptData(encrypted)
        assertArrayEquals(originalBytes, decrypted)
        assertEquals(originalText, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun encryptAndDecryptBackup_withValidPassword_succeeds() {
        val backupJson = """{"engagements":[{"id":"eng-1","title":"Audit"}]}"""
        val password = "SuperSecretPassword123!"

        val encryptedBackup = securityManager.encryptBackup(backupJson, password)
        assertTrue(encryptedBackup.isNotEmpty())

        val decryptedJson = securityManager.decryptBackup(encryptedBackup, password)
        assertEquals(backupJson, decryptedJson)
    }

    @Test
    fun decryptBackup_withWrongPassword_throwsSecurityException() {
        val backupJson = """{"engagements":[]}"""
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"

        val encryptedBackup = securityManager.encryptBackup(backupJson, correctPassword)

        assertThrows(SecurityException::class.java) {
            securityManager.decryptBackup(encryptedBackup, wrongPassword)
        }
    }

    @Test
    fun decryptBackup_withCorruptedPayload_throwsException() {
        val backupJson = """{"data":"sensitive"}"""
        val password = "Password123"

        val encryptedBackup = securityManager.encryptBackup(backupJson, password)
        // Corrupt a byte in the ciphertext payload
        encryptedBackup[encryptedBackup.size - 5] = (encryptedBackup[encryptedBackup.size - 5] + 1).toByte()

        assertThrows(SecurityException::class.java) {
            securityManager.decryptBackup(encryptedBackup, password)
        }
    }
}

