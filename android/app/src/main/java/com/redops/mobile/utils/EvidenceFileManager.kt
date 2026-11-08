package com.redops.mobile.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class ImportedFileInfo(
    val originalFilename: String,
    val storedFilename: String,
    val localPath: String,
    val mimeType: String,
    val fileSize: Long,
    val sha256Checksum: String
)

object EvidenceFileManager {

    const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
    const val MAX_PREVIEW_TEXT_CHARS = 25000

    fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun calculateSha256(file: File): String {
        if (!file.exists()) return ""
        return file.inputStream().use { calculateSha256(it) }
    }

    fun sanitizeFilename(filename: String): String {
        // Strip directory traversal patterns and replace unsafe characters
        val cleanName = filename.substringAfterLast("/").substringAfterLast("\\")
        val sanitized = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return if (sanitized.isBlank()) "unnamed_artifact.bin" else sanitized
    }

    fun copyUriToPrivateStorage(context: Context, sourceUri: Uri): ImportedFileInfo {
        val contentResolver = context.contentResolver

        // 1. Resolve original display name & size from ContentResolver
        var originalName = "evidence_${System.currentTimeMillis()}"
        var declaredSize = 0L

        contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) originalName = name
                }
                if (sizeIndex != -1) {
                    declaredSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (declaredSize > MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("Selected file exceeds maximum allowed size of 50 MB.")
        }

        val sanitizedOriginal = sanitizeFilename(originalName)
        val shortUuid = UUID.randomUUID().toString().take(8)
        val storedFilename = "ev_${System.currentTimeMillis()}_${shortUuid}_$sanitizedOriginal"

        // 2. Prepare internal app-private directory
        val evidenceDir = File(context.filesDir, "evidence").apply { mkdirs() }
        val tempFile = File(evidenceDir, "$storedFilename.tmp")
        val targetFile = File(evidenceDir, storedFilename)

        var actualSize = 0L
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        actualSize += read
                        if (actualSize > MAX_FILE_SIZE_BYTES) {
                            throw IllegalArgumentException("File stream exceeded maximum allowed size of 50 MB.")
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: throw IllegalStateException("Unable to open input stream for selected file.")

            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if rename fails
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            if (targetFile.exists()) targetFile.delete()
            throw e
        }

        val checksum = digest.digest().joinToString("") { "%02x".format(it) }
        val mimeType = contentResolver.getType(sourceUri) ?: detectMimeType(sanitizedOriginal)

        return ImportedFileInfo(
            originalFilename = sanitizedOriginal,
            storedFilename = storedFilename,
            localPath = targetFile.absolutePath,
            mimeType = mimeType,
            fileSize = targetFile.length(),
            sha256Checksum = checksum
        )
    }

    fun detectMimeType(filename: String): String {
        val ext = filename.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt", "log", "conf", "ini", "json", "xml", "csv" -> "text/plain"
            "zip", "tar", "gz" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    fun deleteFile(localPath: String): Boolean {
        if (localPath.isBlank()) return false
        val file = File(localPath)
        if (!file.exists()) return false
        return try {
            // Secure shredding: overwrite contents with zeroes before unlinking
            if (file.isFile && file.length() > 0 && file.length() <= MAX_FILE_SIZE_BYTES) {
                java.io.RandomAccessFile(file, "rws").use { raf ->
                    val zeroBuffer = ByteArray(4096)
                    var remaining = raf.length()
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, zeroBuffer.size.toLong()).toInt()
                        raf.write(zeroBuffer, 0, toWrite)
                        remaining -= toWrite
                    }
                }
            }
            file.delete()
        } catch (_: Exception) {
            file.delete()
        }
    }

    fun getShareableUri(context: Context, localPath: String): Uri? {
        val file = File(localPath)
        if (!file.exists()) return null
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    fun readTextPreview(localPath: String, maxChars: Int = MAX_PREVIEW_TEXT_CHARS): String {
        val file = File(localPath)
        if (!file.exists()) return "[File not found on device: $localPath]"
        return try {
            file.bufferedReader().use { reader ->
                val buffer = CharArray(maxChars)
                val read = reader.read(buffer, 0, maxChars)
                if (read > 0) {
                    val preview = String(buffer, 0, read)
                    if (file.length() > maxChars) {
                        preview + "\n\n--- [Output truncated for performance: ${file.length() / 1024} KB total] ---"
                    } else {
                        preview
                    }
                } else {
                    "[Empty file]"
                }
            }
        } catch (e: Exception) {
            "[Error reading file preview: ${e.message}]"
        }
    }

    fun renderPdfFirstPage(context: Context, localPath: String): Bitmap? {
        val file = File(localPath)
        if (!file.exists()) return null
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = page.width.coerceAtMost(1080)
                val height = (page.height * (width.toFloat() / page.width)).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfRenderer.close()
                fileDescriptor.close()
                bitmap
            } else {
                pdfRenderer.close()
                fileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openFileWithExternalApp(context: Context, localPath: String, mimeType: String) {
        val uri = getShareableUri(context, localPath)
        if (uri == null) {
            Toast.makeText(context, "File not accessible on local storage.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No installed application can open this file type ($mimeType).", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, localPath: String, mimeType: String, title: String) {
        val uri = getShareableUri(context, localPath)
        if (uri == null) {
            Toast.makeText(context, "File not accessible on local storage.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Evidence Securely").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to initiate secure sharing.", Toast.LENGTH_SHORT).show()
        }
    }
}

