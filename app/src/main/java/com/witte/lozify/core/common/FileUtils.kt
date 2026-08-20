package com.witte.lozify.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * FileUtils - Utility object for handling generic file attachments and system intents.
 *
 * Stage 43: Generic file attachment management, categorization, formatting, and FileProvider viewer.
 */
object FileUtils {

    private const val FILES_DIR = "files"

    /**
     * File categorization for distinctive UI rendering and icon/color dispatching.
     */
    enum class FileCategory {
        PDF,
        DOCUMENT, // Word, text, markdown
        SPREADSHEET, // Excel, csv
        PRESENTATION, // PPT, Keynote
        ARCHIVE, // ZIP, RAR, 7Z, TAR
        AUDIO, // MP3, WAV, FLAC, M4A
        VIDEO, // MP4, MKV, AVI
        CODE, // JSON, XML, Java, Kotlin, Python, JS, HTML
        OTHER
    }

    /**
     * Extract display name of a file from Uri.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.takeIf { it.isNotBlank() } ?: "file_${System.currentTimeMillis()}"
    }

    /**
     * Extract size in bytes of a file from Uri.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (index != -1) {
                            return cursor.getLong(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Resolve MIME type from Uri or file extension.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri) ?: getMimeTypeFromExtension(getFileName(context, uri))
    }

    /**
     * Resolve MIME type based on file extension.
     */
    fun getMimeTypeFromExtension(fileNameOrPath: String): String? {
        val extension = fileNameOrPath.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (extension.isBlank()) return null
        val staticMime = when (extension) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar", "gz" -> "application/x-tar"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            else -> null
        }
        if (staticMime != null) return staticMime

        return try {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Check if a file path or MIME type belongs to an image.
     */
    fun isImageFile(filePath: String, mimeType: String? = null): Boolean {
        if (mimeType?.startsWith("image/", ignoreCase = true) == true) {
            return true
        }
        val ext = filePath.substringAfterLast('.', "").lowercase(Locale.getDefault())
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg")
    }

    /**
     * Determine category of file for icon and UI badges.
     */
    fun getFileCategory(fileNameOrPath: String, mimeType: String? = null): FileCategory {
        val ext = fileNameOrPath.substringAfterLast('.', "").lowercase(Locale.getDefault())
        val effectiveMime = mimeType ?: getMimeTypeFromExtension(fileNameOrPath) ?: ""

        return when {
            ext == "pdf" || effectiveMime == "application/pdf" -> FileCategory.PDF
            ext in listOf("doc", "docx", "txt", "md", "rtf", "odt", "wps") || effectiveMime.startsWith("text/") -> FileCategory.DOCUMENT
            ext in listOf("xls", "xlsx", "csv", "tsv") || effectiveMime.contains("spreadsheet") || effectiveMime.contains("excel") -> FileCategory.SPREADSHEET
            ext in listOf("ppt", "pptx", "key") || effectiveMime.contains("powerpoint") || effectiveMime.contains("presentation") -> FileCategory.PRESENTATION
            ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || effectiveMime.contains("zip") || effectiveMime.contains("compressed") -> FileCategory.ARCHIVE
            ext in listOf("mp3", "wav", "m4a", "flac", "aac", "ogg") || effectiveMime.startsWith("audio/") -> FileCategory.AUDIO
            ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") || effectiveMime.startsWith("video/") -> FileCategory.VIDEO
            ext in listOf("kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "c", "cpp", "go", "rs", "sql") -> FileCategory.CODE
            else -> FileCategory.OTHER
        }
    }

    /**
     * Copy a generic file to app internal storage (context.filesDir/files/).
     *
     * @param context Application context
     * @param sourceUri External URI from document picker
     * @return Pair of local File Uri and relative path inside filesDir (e.g. "files/abc_document.pdf")
     */
    fun copyFileToPrivateStorage(
        context: Context,
        sourceUri: Uri
    ): Pair<Uri, String>? {
        return try {
            val originalName = getFileName(context, sourceUri)
            val cleanName = originalName.replace(Regex("[^a-zA-Z0-9._一-龥-]"), "_")
            val shortUuid = UUID.randomUUID().toString().take(8)
            val relativePath = "$FILES_DIR/${shortUuid}_$cleanName"

            val filesDir = File(context.filesDir, FILES_DIR)
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }

            val targetFile = File(context.filesDir, relativePath)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Pair(Uri.fromFile(targetFile), relativePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Format file size in bytes to human readable format (e.g. 1.5 MB, 320 KB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb)
        }
        val mb = kb / 1024.0
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb)
        }
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    /**
     * Open a file with external application using FileProvider.
     */
    fun openFileWithSystemApp(
        context: Context,
        file: File,
        mimeType: String? = null
    ): Boolean {
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在或已被删除", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val resolvedMimeType = mimeType
                ?: getMimeTypeFromExtension(file.name)
                ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "打开附件: ${getDisplayFileName(file.name)}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到能打开该文件的应用", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Clean up UUID/ID prefix (8 alphanumeric chars + underscore) from stored file name for user-facing UI.
     * Example: "a1b2c3d4_my_plan.pdf" -> "my_plan.pdf"
     */
    fun getDisplayFileName(rawPathOrName: String): String {
        val fileName = rawPathOrName.substringAfterLast('/')
        return if (fileName.length > 9 && fileName[8] == '_' && fileName.take(8).all { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' }) {
            fileName.substring(9)
        } else {
            fileName
        }
    }
}
