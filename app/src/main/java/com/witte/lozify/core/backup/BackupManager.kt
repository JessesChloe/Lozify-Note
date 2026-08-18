package com.witte.lozify.core.backup

import android.content.Context
import com.witte.lozify.core.database.LozifyDatabase
import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.entity.TagEntity
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a backup import operation.
 */
data class ImportResult(
    val isSuccess: Boolean,
    val notesImported: Int = 0,
    val tagsImported: Int = 0,
    val errorMessage: String? = null
)

/**
 * BackupManager - Manages data export, Markdown packaging, and JSON backup restoration.
 *
 * Stage 16: Complete Local Data Lifecycle Management.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val database: LozifyDatabase
) {

    /**
     * Export all notes and tags to a structured JSON file.
     */
    suspend fun exportBackupJson(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allNotes = noteRepository.getAllNotes().first()
        val allTags = tagRepository.getAllTags().first()

        val rootJson = JSONObject().apply {
            put("version", 1)
            put("appName", "Lozify")
            put("exportedAt", Instant.now().toString())

            // Tags Array
            val tagsArray = JSONArray()
            allTags.forEach { tag ->
                val tagObj = JSONObject().apply {
                    put("id", tag.id)
                    put("name", tag.name)
                    put("icon", tag.icon ?: "")
                    put("isPinned", tag.isPinned)
                    put("pinOrder", tag.pinOrder)
                    put("usageCount", tag.usageCount)
                }
                tagsArray.put(tagObj)
            }
            put("tags", tagsArray)

            // Notes Array
            val notesArray = JSONArray()
            allNotes.forEach { note ->
                val noteObj = JSONObject().apply {
                    put("id", note.id)
                    put("content", note.content)
                    put("isPinned", note.isPinned)
                    put("isArchived", note.isArchived)
                    put("isDeleted", note.isDeleted)
                    put("createdAt", note.createdAt.toEpochMilli())
                    put("updatedAt", note.updatedAt.toEpochMilli())

                    val tagNamesArray = JSONArray()
                    note.tags.forEach { tagNamesArray.put(it.name) }
                    put("tags", tagNamesArray)

                    val outgoingArray = JSONArray()
                    note.outgoingRelations.forEach { rel ->
                        val relObj = JSONObject().apply {
                            put("toNoteId", rel.toNoteId)
                            put("mentionText", rel.mentionText)
                        }
                        outgoingArray.put(relObj)
                    }
                    put("outgoingRelations", outgoingArray)

                    // Stage 16.8: Image Attachments with Base64 encoding
                    val attachmentsArray = JSONArray()
                    note.attachments.forEach { att ->
                        val file = java.io.File(context.filesDir, att.filePath)
                        if (file.exists()) {
                            try {
                                val bytes = file.readBytes()
                                val base64 = java.util.Base64.getEncoder().encodeToString(bytes)
                                val attObj = JSONObject().apply {
                                    put("displayOrder", att.displayOrder)
                                    put("mimeType", att.mimeType ?: "image/jpeg")
                                    put("base64Data", base64)
                                }
                                attachmentsArray.put(attObj)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    put("attachments", attachmentsArray)
                }
                notesArray.put(noteObj)
            }
            put("notes", notesArray)
        }

        outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(rootJson.toString(2))
        }
    }

    /**
     * Export active notes into a ZIP archive containing individual .md files with YAML frontmatter
     * and packaged images/ directory.
     */
    suspend fun exportMarkdownArchive(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allNotes = noteRepository.getAllNotes().first()
        val activeNotes = allNotes.filter { !it.isDeleted && !it.isArchived }

        ZipOutputStream(outputStream.buffered()).use { zipOut ->
            val writtenImagePaths = mutableSetOf<String>()

            activeNotes.forEachIndexed { index, note ->
                val fileName = "note_${index + 1}_${note.id}.md"
                val tagListStr = if (note.tags.isEmpty()) "[]" else "[${note.tags.joinToString(", ") { it.name }}]"

                val imageRefs = mutableListOf<String>()

                // Package attached image files into images/ inside the ZIP
                note.attachments.forEach { att ->
                    val sourceFile = java.io.File(context.filesDir, att.filePath)
                    if (sourceFile.exists()) {
                        val ext = sourceFile.extension.ifEmpty { "jpg" }
                        val zipImagePath = "images/note_${note.id}_img_${att.displayOrder}.$ext"
                        if (!writtenImagePaths.contains(zipImagePath)) {
                            writtenImagePaths.add(zipImagePath)
                            val imageEntry = ZipEntry(zipImagePath)
                            zipOut.putNextEntry(imageEntry)
                            sourceFile.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                        imageRefs.add("![image]($zipImagePath)")
                    }
                }

                val markdownContent = buildString {
                    appendLine("---")
                    appendLine("id: ${note.id}")
                    appendLine("created_at: ${note.createdAt}")
                    appendLine("updated_at: ${note.updatedAt}")
                    appendLine("pinned: ${note.isPinned}")
                    appendLine("tags: $tagListStr")
                    appendLine("---")
                    appendLine()
                    appendLine(note.content)
                    if (imageRefs.isNotEmpty()) {
                        appendLine()
                        imageRefs.forEach { appendLine(it) }
                    }
                }

                val zipEntry = ZipEntry(fileName)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(markdownContent.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()
            }
        }
    }

    /**
     * Import notes, tags, and images from a JSON backup file and merge into Room database.
     */
    suspend fun importBackupJson(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val rootJson = JSONObject(jsonString)

            if (!rootJson.has("notes") || !rootJson.has("tags")) {
                return@withContext ImportResult(
                    isSuccess = false,
                    errorMessage = "备份文件格式不正确，缺少必要的标签或笔记数据。"
                )
            }

            val tagsArray = rootJson.getJSONArray("tags")
            val notesArray = rootJson.getJSONArray("notes")

            var tagsImported = 0
            var notesImported = 0

            val existingTags = tagRepository.getAllTags().first().associateBy { it.name }
            val tagMap = mutableMapOf<String, Long>() // tagName -> tagId in DB

            // 1. Process Tags
            for (i in 0 until tagsArray.length()) {
                val tagObj = tagsArray.getJSONObject(i)
                val tagName = tagObj.getString("name").trim()
                if (tagName.isEmpty()) continue

                val icon = if (tagObj.has("icon")) tagObj.optString("icon").ifEmpty { null } else null
                val isPinned = tagObj.optBoolean("isPinned", false)
                val pinOrder = tagObj.optInt("pinOrder", 0)

                val existing = existingTags[tagName]
                if (existing != null) {
                    tagMap[tagName] = existing.id
                } else {
                    val newTagId = database.tagDao().insertTag(
                        TagEntity(
                            name = tagName,
                            icon = icon,
                            isPinned = isPinned,
                            pinOrder = pinOrder,
                            createdAt = Instant.now()
                        )
                    )
                    tagMap[tagName] = newTagId
                    tagsImported++
                }
            }

            // 2. Process Notes & Attachments
            val imagesDir = java.io.File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            for (i in 0 until notesArray.length()) {
                val noteObj = notesArray.getJSONObject(i)
                val content = noteObj.getString("content")
                val isPinned = noteObj.optBoolean("isPinned", false)
                val isArchived = noteObj.optBoolean("isArchived", false)
                val isDeleted = noteObj.optBoolean("isDeleted", false)
                val createdAt = Instant.ofEpochMilli(noteObj.optLong("createdAt", System.currentTimeMillis()))
                val updatedAt = Instant.ofEpochMilli(noteObj.optLong("updatedAt", System.currentTimeMillis()))

                val noteEntity = NoteEntity(
                    content = content,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isDeleted = isDeleted,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )

                val newNoteId = database.noteDao().insertNote(noteEntity)
                notesImported++

                // Link Tags
                if (noteObj.has("tags")) {
                    val noteTagsArray = noteObj.getJSONArray("tags")
                    for (t in 0 until noteTagsArray.length()) {
                        val tagName = noteTagsArray.getString(t)
                        val tagId = tagMap[tagName]
                        if (tagId != null) {
                            database.tagDao().insertNoteTagCrossRef(
                                NoteTagCrossRef(
                                    noteId = newNoteId,
                                    tagId = tagId
                                )
                            )
                        }
                    }
                }

                // Restore Image Attachments
                if (noteObj.has("attachments")) {
                    val attachmentsArray = noteObj.getJSONArray("attachments")
                    for (a in 0 until attachmentsArray.length()) {
                        val attObj = attachmentsArray.getJSONObject(a)
                        if (attObj.has("base64Data")) {
                            try {
                                val base64 = attObj.getString("base64Data")
                                val bytes = java.util.Base64.getDecoder().decode(base64)
                                val newFileName = "img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
                                val destFile = java.io.File(imagesDir, newFileName)
                                destFile.writeBytes(bytes)

                                val relativePath = "images/$newFileName"
                                database.attachmentDao().insertAttachment(
                                    com.witte.lozify.data.local.entity.AttachmentEntity(
                                        noteId = newNoteId,
                                        filePath = relativePath,
                                        displayOrder = attObj.optInt("displayOrder", a),
                                        createdAt = Instant.now(),
                                        mimeType = attObj.optString("mimeType", "image/jpeg"),
                                        fileSize = bytes.size.toLong()
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            ImportResult(
                isSuccess = true,
                notesImported = notesImported,
                tagsImported = tagsImported
            )
        } catch (e: Exception) {
            ImportResult(
                isSuccess = false,
                errorMessage = "导入失败: ${e.localizedMessage ?: "未知错误"}"
            )
        }
    }
}
