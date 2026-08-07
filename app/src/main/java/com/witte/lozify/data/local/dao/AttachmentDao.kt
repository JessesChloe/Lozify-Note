package com.witte.lozify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.witte.lozify.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Attachment (image) operations.
 *
 * Handles CRUD operations for note attachments.
 * Images are stored in app's internal sandbox and referenced by relative path.
 */
@Dao
interface AttachmentDao {

    /**
     * Get all attachments for a specific note, ordered by display order.
     */
    @Query("SELECT * FROM attachments WHERE note_id = :noteId ORDER BY display_order ASC")
    fun getAttachmentsForNote(noteId: Long): Flow<List<AttachmentEntity>>

    /**
     * Get a single attachment by ID.
     */
    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: Long): AttachmentEntity?

    /**
     * Get count of attachments for a note (for validation: max 9 images).
     */
    @Query("SELECT COUNT(*) FROM attachments WHERE note_id = :noteId")
    suspend fun getAttachmentCount(noteId: Long): Int

    /**
     * Insert a new attachment. Returns the generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    /**
     * Insert multiple attachments (batch operation for multi-image upload).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    /**
     * Update an existing attachment (e.g., change display order).
     */
    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    /**
     * Delete an attachment. Remember to also delete the physical file from storage.
     */
    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    /**
     * Delete all attachments for a note.
     * Used when deleting a note - remember to clean up physical files too.
     */
    @Query("DELETE FROM attachments WHERE note_id = :noteId")
    suspend fun deleteAllAttachmentsForNote(noteId: Long)

    /**
     * Get total storage size used by all attachments (sum of file_size).
     * Returns null if any file_size is null.
     */
    @Query("SELECT SUM(file_size) FROM attachments WHERE file_size IS NOT NULL")
    fun getTotalStorageUsed(): Flow<Long?>
}
