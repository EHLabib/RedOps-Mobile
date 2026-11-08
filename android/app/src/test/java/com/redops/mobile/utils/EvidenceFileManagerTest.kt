package com.redops.mobile.utils

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class EvidenceFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun calculateSha256_computesCorrectHexDigest() {
        val input = "RedOps_Mobile_Evidence_Integrity_Check"
        val expectedSha256 = "23505d13462938028e6ff1a3c36657efb8f814fba8672a66f6af543479580e92"

        val inputStream = ByteArrayInputStream(input.toByteArray(Charsets.UTF_8))
        val computed = EvidenceFileManager.calculateSha256(inputStream)

        assertEquals(expectedSha256, computed)
    }

    @Test
    fun calculateSha256_fromFile_returnsCorrectHash() {
        val testFile = tempFolder.newFile("sample_evidence.txt")
        testFile.writeText("Evidence Content for Audit", Charsets.UTF_8)

        val hash1 = EvidenceFileManager.calculateSha256(testFile)
        assertFalse(hash1.isBlank())
        assertEquals(64, hash1.length) // 64 hex characters for SHA-256

        // Modifying file changes hash
        testFile.appendText(" modified")
        val hash2 = EvidenceFileManager.calculateSha256(testFile)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun sanitizeFilename_stripsPathTraversalAndUnsafeCharacters() {
        assertEquals("report.pdf", EvidenceFileManager.sanitizeFilename("../../report.pdf"))
        assertEquals("dump.sql", EvidenceFileManager.sanitizeFilename("..\\..\\dump.sql"))
        assertEquals("capture_01.png", EvidenceFileManager.sanitizeFilename("capture 01.png"))
        assertEquals("poc_test_log.txt", EvidenceFileManager.sanitizeFilename("poc:test*log.txt"))
        assertEquals("unnamed_artifact.bin", EvidenceFileManager.sanitizeFilename(""))
    }

    @Test
    fun detectMimeType_resolvesCorrectTypesByExtension() {
        assertEquals("image/png", EvidenceFileManager.detectMimeType("screenshot.png"))
        assertEquals("image/jpeg", EvidenceFileManager.detectMimeType("artifact.jpg"))
        assertEquals("application/pdf", EvidenceFileManager.detectMimeType("pentest_report.pdf"))
        assertEquals("text/plain", EvidenceFileManager.detectMimeType("nmap_scan.txt"))
        assertEquals("text/plain", EvidenceFileManager.detectMimeType("server.log"))
        assertEquals("text/plain", EvidenceFileManager.detectMimeType("nginx.conf"))
        assertEquals("application/zip", EvidenceFileManager.detectMimeType("archive.zip"))
        assertEquals("application/octet-stream", EvidenceFileManager.detectMimeType("unknown.dat"))
    }

    @Test
    fun readTextPreview_handlesExistingAndMissingFilesSafely() {
        // Missing file
        val missingPreview = EvidenceFileManager.readTextPreview("D:/nonexistent_evidence_file.txt")
        assertTrue(missingPreview.contains("File not found on device"))

        // Existing file
        val textFile = tempFolder.newFile("burp_response.txt")
        textFile.writeText("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"status\":\"vulnerable\"}")
        val preview = EvidenceFileManager.readTextPreview(textFile.absolutePath)
        assertTrue(preview.contains("HTTP/1.1 200 OK"))
        assertTrue(preview.contains("vulnerable"))
    }

    @Test
    fun deleteFile_deletesExistingFileAndReturnsTrue() {
        val file = tempFolder.newFile("to_delete.txt")
        assertTrue(file.exists())

        val result = EvidenceFileManager.deleteFile(file.absolutePath)
        assertTrue(result)
        assertFalse(file.exists())

        // Calling again on deleted file returns false
        val secondResult = EvidenceFileManager.deleteFile(file.absolutePath)
        assertFalse(secondResult)
    }
}

