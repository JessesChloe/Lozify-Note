package com.witte.lozify.domain.model

import java.time.Instant

/**
 * Domain model representing an image attachment.
 *
 * ⚠️ PURE KOTLIN - NO ANDROID DEPENDENCIES
 * This is a business logic model, completely decoupled from Room/Android framework.
 *
 * Storage Strategy (ADR-007):
 * - All images stored in app's internal sandbox (Context.filesDir/images/)
 * - filePath is relative path within sandbox (e.g., "images/note_123_img_0.jpg")
 * - Original gallery URIs are copied and compressed during insert
 *
 * @property id Unique identifier (0 for unsaved attachments)
 * @property noteId Parent note ID
 * @property filePath Relative path within app's internal storage
 * @property displayOrder Order in 3-column grid (0-indexed)
 * @property createdAt Creation timestamp
 * @property mimeType Optional MIME type (e.g., "image/jpeg")
 * @property fileSize Optional file size in bytes
 */
data class Attachment(
    val id: Long = 0,
    val noteId: Long,
    val filePath: String,
    val displayOrder: Int,
    val createdAt: Instant,
    val mimeType: String? = null,
    val fileSize: Long? = null
) {
    /**
     * Check if this is an image attachment.
     */
    fun isImage(): Boolean {
        if (mimeType?.startsWith("image/", ignoreCase = true) == true) return true
        val ext = filePath.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg")
    }

    /**
     * Check if this is a generic file/document attachment.
     */
    fun isFile(): Boolean = !isImage()

    /**
     * Check if this is a JPEG image.
     */
    fun isJpeg(): Boolean = mimeType?.lowercase()?.contains("jpeg") == true

    /**
     * Check if this is a PNG image.
     */
    fun isPng(): Boolean = mimeType?.lowercase()?.contains("png") == true

    /**
     * Get clean display file name without internal UUID prefix.
     */
    fun getDisplayName(): String {
        val fileName = filePath.substringAfterLast('/')
        val underscoreIdx = fileName.indexOf('_')
        return if (underscoreIdx in 1..9) {
            fileName.substring(underscoreIdx + 1)
        } else {
            fileName
        }
    }

    /**
     * Get formatted file size string (e.g. 1.2 MB, 450 KB).
     */
    fun getFormattedFileSize(): String {
        val bytes = fileSize ?: return ""
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", kb)
        }
        val mb = kb / 1024.0
        if (mb < 1024) {
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }

    /**
     * Get file size in MB (for display purposes).
     */
    fun getFileSizeInMB(): Double? {
        return fileSize?.let { it / (1024.0 * 1024.0) }
    }
}
