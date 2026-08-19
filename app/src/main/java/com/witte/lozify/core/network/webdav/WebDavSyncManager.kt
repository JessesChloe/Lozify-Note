package com.witte.lozify.core.network.webdav

import android.content.Context
import com.witte.lozify.core.database.LozifyDatabase
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.data.local.entity.AttachmentEntity
import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.data.local.entity.NoteRelationEntity
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.entity.TagEntity
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stages of WebDAV Synchronization.
 */
enum class SyncStage {
    IDLE,
    CONNECTING,
    FETCHING_REMOTE,
    MERGING_DATA,
    SYNCING_IMAGES,
    UPLOADING_REMOTE,
    COMPLETED,
    FAILED
}

/**
 * Progress updates for UI display.
 */
data class SyncProgress(
    val stage: SyncStage,
    val progress: Float = 0f,
    val detail: String = ""
)

/**
 * Result of a completed synchronization cycle.
 */
data class SyncResult(
    val isSuccess: Boolean,
    val isAlreadyUpToDate: Boolean = false,
    val uploadedNotes: Int = 0,
    val downloadedNotes: Int = 0,
    val uploadedImages: Int = 0,
    val downloadedImages: Int = 0,
    val errorMessage: String? = null
)

/**
 * WebDavSyncManager - Orchestrates two-way delta sync with Jianguoyun and WebDAV servers.
 *
 * Algorithm:
 * 1. Reads WebDAV connection credentials from UserPreferencesManager.
 * 2. Ensures remote root directory (/Lozify/) and image directory (/Lozify/images/) exist.
 * 3. Fast-Path: Probes manifest.json to skip sync in <150ms with 0 payload traffic if no changes on either side.
 * 4. Downloads remote notes_payload.json (if present) and performs Last-Write-Wins (LWW) merge with local Room DB.
 * 5. Syncs images bi-directionally (uploads missing local images, downloads missing remote images).
 * 6. Re-uploads merged notes_payload.json and updates manifest.json.
 * 7. Records lastSyncTime in UserPreferencesManager.
 *
 * Stage 29: WebDAV Cloud Sync Fast-Path Optimization.
 */
@Singleton
class WebDavSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDavClient: WebDavClient,
    private val preferencesManager: UserPreferencesManager,
    private val database: LozifyDatabase,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) {

    /**
     * Test connection to configured WebDAV server.
     */
    suspend fun testConnection(): Result<Boolean> {
        val serverUrl = preferencesManager.webdavServerUrl.value
        val username = preferencesManager.webdavUsername.value
        val password = preferencesManager.webdavPassword.value

        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写完整的服务器地址、账号及授权密码"))
        }

        return webDavClient.testConnection(serverUrl, username, password)
    }

    /**
     * Perform complete two-way synchronization with Fast-Path support.
     *
     * @param forceFullSync If true, skips manifest timestamp shortcut and performs full merge.
     */
    suspend fun performSync(
        forceFullSync: Boolean = false,
        onProgress: (SyncProgress) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        val serverUrl = preferencesManager.webdavServerUrl.value
        val username = preferencesManager.webdavUsername.value
        val password = preferencesManager.webdavPassword.value
        val remoteDir = preferencesManager.webdavRemoteDir.value.let {
            if (it.startsWith("/")) it else "/$it"
        }.let {
            if (it.endsWith("/")) it else "$it/"
        }

        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext SyncResult(
                isSuccess = false,
                errorMessage = "请先配置 WebDAV 服务器地址与应用授权密码"
            )
        }

        try {
            // Stage 1: Connecting
            onProgress(SyncProgress(SyncStage.CONNECTING, 0.1f, "正在连接 WebDAV 服务器..."))
            val connTest = webDavClient.testConnection(serverUrl, username, password)
            if (connTest.isFailure) {
                val errorMsg = connTest.exceptionOrNull()?.message ?: "连接 WebDAV 服务器失败"
                onProgress(SyncProgress(SyncStage.FAILED, 0f, errorMsg))
                return@withContext SyncResult(isSuccess = false, errorMessage = errorMsg)
            }

            // Ensure directories
            val dataDir = "${remoteDir}data/"
            val imagesDir = "${remoteDir}images/"
            webDavClient.ensureDirectory(serverUrl, username, password, remoteDir)
            webDavClient.ensureDirectory(serverUrl, username, password, dataDir)
            webDavClient.ensureDirectory(serverUrl, username, password, imagesDir)

            val isEncryptionEnabled = preferencesManager.webdavEncryptionEnabled.value
            val encryptionPassword = preferencesManager.webdavEncryptionPassword.value

            if (isEncryptionEnabled && encryptionPassword.isBlank()) {
                return@withContext SyncResult(
                    isSuccess = false,
                    errorMessage = "已开启端到端加密，但未设置加密主密码，请先输入密码"
                )
            }

            // Fast-Path: Probe manifest.json to skip sync if no updates occurred
            if (!forceFullSync) {
                val manifestRes = webDavClient.downloadBytes(serverUrl, username, password, "${remoteDir}manifest.json")
                if (manifestRes.isSuccess) {
                    val manifestObj = try {
                        JSONObject(String(manifestRes.getOrThrow(), StandardCharsets.UTF_8))
                    } catch (e: Exception) {
                        null
                    }

                    if (manifestObj != null) {
                        val remoteLastSyncTime = manifestObj.optLong("lastSyncTime", 0L)
                        val remoteNoteCount = manifestObj.optInt("noteCount", -1)
                        val localLastSyncTime = preferencesManager.webdavLastSyncTime.value

                        val localAllNotes = noteRepository.getAllNotesIncludingDeleted().first()
                        val maxLocalUpdatedAt = localAllNotes.maxOfOrNull { it.updatedAt.toEpochMilli() } ?: 0L
                        val activeLocalNotesCount = localAllNotes.count { !it.isDeleted && !it.isArchived }

                        if (remoteLastSyncTime > 0 && localLastSyncTime > 0 &&
                            remoteLastSyncTime <= localLastSyncTime &&
                            maxLocalUpdatedAt <= localLastSyncTime &&
                            remoteNoteCount == activeLocalNotesCount
                        ) {
                            onProgress(SyncProgress(SyncStage.COMPLETED, 1.0f, "已是最新数据"))
                            return@withContext SyncResult(
                                isSuccess = true,
                                isAlreadyUpToDate = true
                            )
                        }
                    }
                }
            }

            // Stage 2: Fetching Remote Payload
            onProgress(SyncProgress(SyncStage.FETCHING_REMOTE, 0.25f, "正在拉取云端数据..."))
            val encPayloadPath = "${dataDir}notes_payload.json.enc"
            val plainPayloadPath = "${dataDir}notes_payload.json"

            var remoteJson: JSONObject? = null

            // 1. Try downloading encrypted payload
            val downloadEncRes = webDavClient.downloadBytes(serverUrl, username, password, encPayloadPath)
            if (downloadEncRes.isSuccess) {
                if (encryptionPassword.isBlank()) {
                    return@withContext SyncResult(
                        isSuccess = false,
                        errorMessage = "云端数据已开启端到端加密，请在设置中输入加密主密码后重试"
                    )
                }
                try {
                    val decryptedBytes = com.witte.lozify.core.security.CryptoUtils.decryptBytes(
                        downloadEncRes.getOrThrow(),
                        encryptionPassword
                    )
                    remoteJson = JSONObject(String(decryptedBytes, StandardCharsets.UTF_8))
                } catch (e: Exception) {
                    return@withContext SyncResult(
                        isSuccess = false,
                        errorMessage = "端到端加密主密码错误，无法解密云端数据，请检查密码设置"
                    )
                }
            } else {
                // 2. Try downloading plain payload
                val downloadPlainRes = webDavClient.downloadBytes(serverUrl, username, password, plainPayloadPath)
                if (downloadPlainRes.isSuccess) {
                    try {
                        remoteJson = JSONObject(String(downloadPlainRes.getOrThrow(), StandardCharsets.UTF_8))
                    } catch (e: Exception) {
                        remoteJson = null
                    }
                }
            }

            // Stage 3: Merging Data (Distributed UUID & Dual-Fingerprint Anti-Duplication)
            onProgress(SyncProgress(SyncStage.MERGING_DATA, 0.45f, "正在进行双向数据比对与防重复合并..."))
            var downloadedNotesCount = 0
            var uploadedNotesCount = 0

            fun noteFingerprint(createdAtMillis: Long, content: String): String = "$createdAtMillis:${content.trim()}"

            // Stage 30/31/35 Self-Healing:
            // 1. Clean up any historical duplicate notes locally
            val rawLocalNotes = noteRepository.getAllNotesIncludingDeleted().first()
            val groupedDuplicates = rawLocalNotes.filter { !it.isDeleted }.groupBy { noteFingerprint(it.createdAt.toEpochMilli(), it.content) }
            for ((_, duplicateList) in groupedDuplicates) {
                if (duplicateList.size > 1) {
                    val toKeep = duplicateList.maxByOrNull { it.id } ?: duplicateList.first()
                    val toRemove = duplicateList.filter { it.id != toKeep.id }
                    for (extra in toRemove) {
                        database.noteDao().softDeleteNote(extra.id, System.currentTimeMillis())
                    }
                }
            }

            // 2. Ensure all active and soft-deleted local notes have a persistent global syncId (UUID)
            val refreshedLocalNotes = noteRepository.getAllNotesIncludingDeleted().first()
            for (rawNote in refreshedLocalNotes) {
                if (rawNote.syncId.isBlank()) {
                    val assignedSyncId = "lz-${rawNote.createdAt.toEpochMilli()}-${java.util.UUID.randomUUID().toString().take(8)}"
                    database.noteDao().updateSyncId(rawNote.id, assignedSyncId)
                }
            }

            val localNotes = noteRepository.getAllNotesIncludingDeleted().first()
            val localTags = tagRepository.getAllTags().first()

            val remoteNotesBySyncId = mutableMapOf<String, JSONObject>()
            val remoteNotesByFingerprint = mutableMapOf<String, JSONObject>()
            val remoteTagsMap = mutableMapOf<String, JSONObject>()

            if (remoteJson != null) {
                // Parse remote tags
                val tagsArr = remoteJson.optJSONArray("tags") ?: JSONArray()
                for (i in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(i)
                    val tagName = tagObj.optString("name")
                    if (tagName.isNotBlank()) {
                        remoteTagsMap[tagName] = tagObj
                    }
                }

                // Parse remote notes with strict fingerprint deduplication
                val notesArr = remoteJson.optJSONArray("notes") ?: JSONArray()
                for (i in 0 until notesArr.length()) {
                    val noteObj = notesArr.getJSONObject(i)
                    val createdAt = noteObj.optLong("createdAt", 0L)
                    val content = noteObj.optString("content", "")
                    val fp = noteFingerprint(createdAt, content)
                    val syncId = noteObj.optString("syncId").ifEmpty {
                        val legacyId = noteObj.optLong("id", 0L)
                        "lz-$createdAt-$legacyId"
                    }
                    if (syncId.isNotBlank()) {
                        val existing = remoteNotesByFingerprint[fp]
                        if (existing == null) {
                            remoteNotesByFingerprint[fp] = noteObj
                            remoteNotesBySyncId[syncId] = noteObj
                        } else {
                            // If remote payload had duplicate entries, keep the newer one
                            val existingUpdatedAt = existing.optLong("updatedAt", 0L)
                            val thisUpdatedAt = noteObj.optLong("updatedAt", 0L)
                            if (thisUpdatedAt > existingUpdatedAt) {
                                val oldSyncId = existing.optString("syncId").ifEmpty { "lz-$createdAt-${existing.optLong("id", 0L)}" }
                                remoteNotesBySyncId.remove(oldSyncId)
                                remoteNotesByFingerprint[fp] = noteObj
                                remoteNotesBySyncId[syncId] = noteObj
                            }
                        }
                    }
                }
            }

            val localNotesBySyncId = localNotes.associateBy { it.syncId }.toMutableMap()
            val localNotesByFingerprint = localNotes.associateBy { noteFingerprint(it.createdAt.toEpochMilli(), it.content) }.toMutableMap()
            val processedLocalSyncIds = mutableSetOf<String>()

            // Merge tags into local DB
            for ((tagName, tagObj) in remoteTagsMap) {
                val existingLocal = tagRepository.getTagByName(tagName)
                if (existingLocal == null) {
                    val newTag = TagEntity(
                        name = tagName,
                        createdAt = Instant.now(),
                        icon = tagObj.optString("icon").ifEmpty { null },
                        isPinned = tagObj.optBoolean("isPinned", false),
                        pinOrder = tagObj.optInt("pinOrder", 0)
                    )
                    database.tagDao().insertTag(newTag)
                }
            }

            // Merge remote notes with dual-fingerprint anti-duplication & deletion LWW
            for ((remoteSyncId, remoteObj) in remoteNotesBySyncId) {
                val remoteCreatedAt = remoteObj.optLong("createdAt", 0L)
                val remoteContent = remoteObj.optString("content", "")
                val isRemoteDeleted = remoteObj.optBoolean("isDeleted", false)
                val remoteFingerprint = noteFingerprint(remoteCreatedAt, remoteContent)

                // 1. First attempt: match by syncId
                var matchedLocal = localNotesBySyncId[remoteSyncId]

                // 2. Second attempt: match by timestamp + content fingerprint (cross-version anti-duplication)
                if (matchedLocal == null) {
                    matchedLocal = localNotesByFingerprint[remoteFingerprint]
                    if (matchedLocal != null) {
                        // Adopt remote syncId for local note
                        database.noteDao().updateSyncId(matchedLocal.id, remoteSyncId)
                        localNotesBySyncId[remoteSyncId] = matchedLocal.copy(syncId = remoteSyncId)
                    }
                }

                if (matchedLocal == null) {
                    // Truly brand-new note -> insert locally
                    insertRemoteNoteToLocal(remoteSyncId, remoteObj)
                    if (!isRemoteDeleted) {
                        downloadedNotesCount++
                    }
                    // Dynamic tracking: register inserted note in memory to prevent duplicate in same session
                    val dummyInserted = Note(
                        id = 0L,
                        syncId = remoteSyncId,
                        content = remoteContent,
                        isDeleted = isRemoteDeleted,
                        isArchived = remoteObj.optBoolean("isArchived", false),
                        isPinned = remoteObj.optBoolean("isPinned", false),
                        createdAt = Instant.ofEpochMilli(remoteCreatedAt),
                        updatedAt = Instant.ofEpochMilli(remoteObj.optLong("updatedAt", remoteCreatedAt))
                    )
                    localNotesBySyncId[remoteSyncId] = dummyInserted
                    localNotesByFingerprint[remoteFingerprint] = dummyInserted
                    processedLocalSyncIds.add(remoteSyncId)
                } else {
                    processedLocalSyncIds.add(matchedLocal.syncId)
                    val remoteUpdatedAt = remoteObj.optLong("updatedAt", 0L)
                    val localUpdatedAt = matchedLocal.updatedAt.toEpochMilli()

                    if (remoteUpdatedAt > localUpdatedAt) {
                        // Remote is newer -> update local DB (including deletion state)
                        updateLocalNoteFromRemote(matchedLocal.id, remoteSyncId, remoteObj)
                        if (!isRemoteDeleted) {
                            downloadedNotesCount++
                        }
                    } else if (localUpdatedAt > remoteUpdatedAt) {
                        // Local is newer (whether active or deleted) -> will be written to merged payload
                        if (!matchedLocal.isDeleted) {
                            uploadedNotesCount++
                        }
                    }
                }
            }

            // Count local notes not in remote payload
            for ((localSyncId, _) in localNotesBySyncId) {
                if (localSyncId !in processedLocalSyncIds && localSyncId !in remoteNotesBySyncId) {
                    uploadedNotesCount++
                }
            }

            // Stage 4: Syncing Images
            onProgress(SyncProgress(SyncStage.SYNCING_IMAGES, 0.65f, "正在同步多媒体图片附件..."))
            var uploadedImagesCount = 0
            var downloadedImagesCount = 0

            val remoteFilesRes = webDavClient.listFiles(serverUrl, username, password, imagesDir)
            val remoteImageNames = if (remoteFilesRes.isSuccess) {
                remoteFilesRes.getOrThrow().map { it.href.substringAfterLast('/') }.toSet()
            } else {
                emptySet()
            }

            // Upload missing local images
            val localImagesDir = File(context.filesDir, "images")
            if (localImagesDir.exists()) {
                val localImageFiles = localImagesDir.listFiles() ?: emptyArray()
                for (localImg in localImageFiles) {
                    if (localImg.isFile) {
                        if (isEncryptionEnabled && encryptionPassword.isNotBlank()) {
                            val encFileName = "${localImg.name}.enc"
                            if (encFileName !in remoteImageNames) {
                                val tempEncFile = File(context.cacheDir, encFileName)
                                try {
                                    com.witte.lozify.core.security.CryptoUtils.encryptFile(localImg, tempEncFile, encryptionPassword)
                                    val uploadImgRes = webDavClient.uploadFile(
                                        serverUrl, username, password,
                                        "$imagesDir$encFileName",
                                        tempEncFile,
                                        contentType = "application/octet-stream"
                                    )
                                    if (uploadImgRes.isSuccess) {
                                        uploadedImagesCount++
                                    }
                                } finally {
                                    tempEncFile.delete()
                                }
                            }
                        } else {
                            if (localImg.name !in remoteImageNames) {
                                val uploadImgRes = webDavClient.uploadFile(
                                    serverUrl, username, password,
                                    "$imagesDir${localImg.name}",
                                    localImg
                                )
                                if (uploadImgRes.isSuccess) {
                                    uploadedImagesCount++
                                }
                            }
                        }
                    }
                }
            }

            // Download missing remote images
            val allMergedNotes = noteRepository.getAllNotes().first()
            for (note in allMergedNotes) {
                for (att in note.attachments) {
                    val localTarget = File(context.filesDir, att.filePath)
                    if (!localTarget.exists()) {
                        val imgFileName = att.filePath.substringAfterLast('/')
                        val encFileName = "$imgFileName.enc"

                        if (encFileName in remoteImageNames && encryptionPassword.isNotBlank()) {
                            val tempEncFile = File(context.cacheDir, encFileName)
                            try {
                                val dlRes = webDavClient.downloadFile(
                                    serverUrl, username, password,
                                    "$imagesDir$encFileName",
                                    tempEncFile
                                )
                                if (dlRes.isSuccess) {
                                    com.witte.lozify.core.security.CryptoUtils.decryptFile(tempEncFile, localTarget, encryptionPassword)
                                    downloadedImagesCount++
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                tempEncFile.delete()
                            }
                        } else if (imgFileName in remoteImageNames) {
                            val dlRes = webDavClient.downloadFile(
                                serverUrl, username, password,
                                "$imagesDir$imgFileName",
                                localTarget
                            )
                            if (dlRes.isSuccess) {
                                downloadedImagesCount++
                            }
                        }
                    }
                }
            }

            // Stage 5: Uploading Final Merged Payload (Sanitized & Deduplicated)
            onProgress(SyncProgress(SyncStage.UPLOADING_REMOTE, 0.85f, "正在回写云端全量数据..."))
            val allRawNotes = noteRepository.getAllNotesIncludingDeleted().first()
            val finalUniqueNotes = mutableListOf<Note>()
            val seenFinalFp = mutableSetOf<String>()
            for (n in allRawNotes) {
                val fp = noteFingerprint(n.createdAt.toEpochMilli(), n.content)
                if (fp in seenFinalFp) {
                    database.noteDao().softDeleteNote(n.id, System.currentTimeMillis())
                } else {
                    seenFinalFp.add(fp)
                    finalUniqueNotes.add(n)
                }
            }
            val finalTags = tagRepository.getAllTags().first()

            val mergedPayloadJson = serializePayloadJson(finalUniqueNotes, finalTags)
            val jsonBytes = mergedPayloadJson.toString(2).toByteArray(StandardCharsets.UTF_8)

            if (isEncryptionEnabled && encryptionPassword.isNotBlank()) {
                val encryptedPayloadBytes = com.witte.lozify.core.security.CryptoUtils.encryptBytes(jsonBytes, encryptionPassword)
                webDavClient.uploadBytes(
                    serverUrl, username, password,
                    encPayloadPath,
                    encryptedPayloadBytes,
                    contentType = "application/octet-stream"
                )
            } else {
                webDavClient.uploadBytes(
                    serverUrl, username, password,
                    plainPayloadPath,
                    jsonBytes
                )
            }

            // Update manifest.json
            val nowEpoch = System.currentTimeMillis()
            val manifestJson = JSONObject().apply {
                put("version", 1)
                put("isEncrypted", isEncryptionEnabled)
                put("lastSyncTime", nowEpoch)
                put("noteCount", finalUniqueNotes.count { !it.isDeleted && !it.isArchived })
                put("tagCount", finalTags.size)
                put("device", android.os.Build.MODEL ?: "Android")
            }
            webDavClient.uploadBytes(
                serverUrl, username, password,
                "${remoteDir}manifest.json",
                manifestJson.toString(2).toByteArray(StandardCharsets.UTF_8)
            )

            // Save sync timestamp
            preferencesManager.setWebDavLastSyncTime(nowEpoch)

            onProgress(SyncProgress(SyncStage.COMPLETED, 1.0f, "同步成功！"))

            return@withContext SyncResult(
                isSuccess = true,
                uploadedNotes = uploadedNotesCount,
                downloadedNotes = downloadedNotesCount,
                uploadedImages = uploadedImagesCount,
                downloadedImages = downloadedImagesCount
            )
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "同步失败"
            onProgress(SyncProgress(SyncStage.FAILED, 0f, err))
            return@withContext SyncResult(isSuccess = false, errorMessage = err)
        }
    }

    private suspend fun insertRemoteNoteToLocal(syncId: String, remote: JSONObject) {
        val content = remote.optString("content", "")
        val isPinned = remote.optBoolean("isPinned", false)
        val isArchived = remote.optBoolean("isArchived", false)
        val isDeleted = remote.optBoolean("isDeleted", false)
        val createdAt = Instant.ofEpochMilli(remote.optLong("createdAt", System.currentTimeMillis()))
        val updatedAt = Instant.ofEpochMilli(remote.optLong("updatedAt", System.currentTimeMillis()))

        val entity = NoteEntity(
            id = 0L, // Let Room auto-generate device-specific local ID (avoids ID collision)
            content = content,
            isPinned = isPinned,
            isArchived = isArchived,
            isDeleted = isDeleted,
            syncId = syncId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        val newLocalNoteId = database.noteDao().insertNote(entity)

        // Tags
        val tagNames = remote.optJSONArray("tags")
        if (tagNames != null) {
            for (i in 0 until tagNames.length()) {
                val name = tagNames.optString(i)
                if (name.isNotBlank()) {
                    val existingTag = database.tagDao().getTagByName(name)
                    val tagId = existingTag?.id ?: database.tagDao().insertTag(TagEntity(name = name, createdAt = Instant.now()))
                    database.tagDao().insertNoteTagCrossRef(NoteTagCrossRef(noteId = newLocalNoteId, tagId = tagId))
                }
            }
        }

        // Attachments
        val atts = remote.optJSONArray("attachments")
        if (atts != null) {
            for (i in 0 until atts.length()) {
                val attObj = atts.getJSONObject(i)
                val filePath = attObj.optString("filePath")
                val displayOrder = attObj.optInt("displayOrder", i)
                val mimeType = attObj.optString("mimeType", "image/jpeg")
                if (filePath.isNotBlank()) {
                    database.attachmentDao().insertAttachment(
                        AttachmentEntity(
                            noteId = newLocalNoteId,
                            filePath = filePath,
                            displayOrder = displayOrder,
                            createdAt = Instant.now(),
                            mimeType = mimeType
                        )
                    )
                }
            }
        }
    }

    private suspend fun updateLocalNoteFromRemote(localNoteId: Long, syncId: String, remote: JSONObject) {
        val content = remote.optString("content", "")
        val isPinned = remote.optBoolean("isPinned", false)
        val isArchived = remote.optBoolean("isArchived", false)
        val isDeleted = remote.optBoolean("isDeleted", false)
        val createdAt = Instant.ofEpochMilli(remote.optLong("createdAt", System.currentTimeMillis()))
        val updatedAt = Instant.ofEpochMilli(remote.optLong("updatedAt", System.currentTimeMillis()))

        val entity = NoteEntity(
            id = localNoteId,
            content = content,
            isPinned = isPinned,
            isArchived = isArchived,
            isDeleted = isDeleted,
            syncId = syncId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        database.noteDao().updateNote(entity)

        // Clear and re-insert cross refs
        database.tagDao().deleteAllTagsForNote(localNoteId)
        val tagNames = remote.optJSONArray("tags")
        if (tagNames != null) {
            for (i in 0 until tagNames.length()) {
                val name = tagNames.optString(i)
                if (name.isNotBlank()) {
                    val existingTag = database.tagDao().getTagByName(name)
                    val tagId = existingTag?.id ?: database.tagDao().insertTag(TagEntity(name = name, createdAt = Instant.now()))
                    database.tagDao().insertNoteTagCrossRef(NoteTagCrossRef(noteId = localNoteId, tagId = tagId))
                }
            }
        }
    }

    private fun serializePayloadJson(notes: List<Note>, tags: List<Tag>): JSONObject {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", Instant.now().toString())

        val tagsArr = JSONArray()
        tags.forEach { tag ->
            val obj = JSONObject().apply {
                put("id", tag.id)
                put("name", tag.name)
                put("icon", tag.icon ?: "")
                put("isPinned", tag.isPinned)
                put("pinOrder", tag.pinOrder)
                put("usageCount", tag.usageCount)
            }
            tagsArr.put(obj)
        }
        root.put("tags", tagsArr)

        val notesArr = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                val persistentSyncId = note.syncId.ifEmpty { "lz-${note.createdAt.toEpochMilli()}-${note.id}" }
                put("syncId", persistentSyncId)
                put("id", note.id)
                put("content", note.content)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("isDeleted", note.isDeleted)
                put("createdAt", note.createdAt.toEpochMilli())
                put("updatedAt", note.updatedAt.toEpochMilli())

                val tagNames = JSONArray()
                note.tags.forEach { tagNames.put(it.name) }
                put("tags", tagNames)

                val relations = JSONArray()
                note.outgoingRelations.forEach { rel ->
                    val relObj = JSONObject().apply {
                        put("toNoteId", rel.toNoteId)
                        put("mentionText", rel.mentionText)
                    }
                    relations.put(relObj)
                }
                put("outgoingRelations", relations)

                val atts = JSONArray()
                note.attachments.forEach { att ->
                    val attObj = JSONObject().apply {
                        put("filePath", att.filePath)
                        put("displayOrder", att.displayOrder)
                        put("mimeType", att.mimeType ?: "image/jpeg")
                    }
                    atts.put(attObj)
                }
                put("attachments", atts)
            }
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        return root
    }
}
