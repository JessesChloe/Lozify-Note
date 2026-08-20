package com.witte.lozify.core.network.webdav

import com.witte.lozify.domain.model.Attachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AttachmentDownloadManager - Manages on-demand downloading of cloud attachments.
 *
 * Stage 48: Lazy download architecture for WebDAV generic attachments.
 */
@Singleton
class AttachmentDownloadManager @Inject constructor(
    private val webDavSyncManager: WebDavSyncManager
) {
    private val _downloadingStates = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val downloadingStates: StateFlow<Map<Long, Float>> = _downloadingStates.asStateFlow()

    /**
     * Download attachment on-demand with reactive progress reporting.
     */
    suspend fun downloadAttachment(attachment: Attachment): Result<File> {
        val id = attachment.id
        _downloadingStates.value = _downloadingStates.value + (id to 0.1f)

        try {
            val result = webDavSyncManager.downloadAttachmentOnDemand(
                attachment = attachment,
                onProgress = { progress ->
                    _downloadingStates.value = _downloadingStates.value + (id to progress)
                }
            )
            return result
        } finally {
            _downloadingStates.value = _downloadingStates.value - id
        }
    }
}
