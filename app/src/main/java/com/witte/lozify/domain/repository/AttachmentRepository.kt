package com.witte.lozify.domain.repository

import android.net.Uri
import com.witte.lozify.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Attachment operations.
 *
 * Stage 6: Handles image attachment management with privatization.
 *
 * Key Responsibilities:
 * - Manage attachment metadata in database
 * - Copy images from external storage to app private directory
 * - Clean up orphaned image files
 */
interface AttachmentRepository {

    /**
     * Get all attachments for a specific note.
     *
     * @param noteId Note ID
     * @return Flow of attachment list
     */
    fun getAttachmentsForNote(noteId: Long): Flow<List<Attachment>>

    /**
     * Insert a new attachment.
     *
     * @param attachment Attachment to insert
     * @return Generated attachment ID
     */
    suspend fun insertAttachment(attachment: Attachment): Long

    /**
     * Delete an attachment by ID.
     * Also deletes the physical image file from private storage.
     *
     * @param attachmentId Attachment ID to delete
     */
    suspend fun deleteAttachment(attachmentId: Long)

    /**
     * Add image attachment from external URI.
     *
     * Process:
     * 1. Copy image from source URI to app private storage
     * 2. Compress image to reduce size
     * 3. Create attachment metadata in database
     *
     * @param noteId Note ID to attach image to
     * @param sourceUri External image URI (from gallery, camera, etc.)
     * @return Created attachment, or null if operation failed
     */
    suspend fun addImageAttachment(noteId: Long, sourceUri: Uri): Attachment?

    /**
     * Clean up orphaned images (files not referenced by any attachment).
     *
     * Call this periodically or during app maintenance.
     */
    suspend fun cleanupOrphanedImages()
}
