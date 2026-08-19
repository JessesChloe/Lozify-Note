package com.witte.lozify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.data.local.model.NoteWithTagsAndAttachments
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for Note operations.
 *
 * Uses Flow for reactive data streams - UI automatically updates when data changes.
 * All mutating operations use suspend for coroutine support.
 */
@Dao
interface NoteDao {

    /**
     * Get all notes that are not deleted or archived, sorted by pinned status and update time.
     * Pinned notes appear first, then sorted by recently updated.
     */
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun getAllNotes(): Flow<List<NoteEntity>>

    /**
     * Stage 6 Fix: Get all notes with tags and attachments using Room @Relation.
     * This replaces the fragile combine(List<Flow>) pattern.
     */
    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun getAllNotesWithRelations(): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Get a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteEntity?>

    /**
     * Get all notes containing specific text in content (for search).
     */
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0 AND content LIKE '%' || :searchQuery || '%'
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun searchNotes(searchQuery: String): Flow<List<NoteEntity>>

    /**
     * Stage 6 Fix: Search notes with tags and attachments using Room @Relation.
     */
    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0 AND content LIKE '%' || :searchQuery || '%'
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun searchNotesWithRelations(searchQuery: String): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Get all pinned notes.
     */
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0 AND is_pinned = 1
        ORDER BY updated_at DESC
    """)
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    /**
     * Stage 6 Fix: Get pinned notes with tags and attachments using Room @Relation.
     */
    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 0 AND is_pinned = 1
        ORDER BY updated_at DESC
    """)
    fun getPinnedNotesWithRelations(): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Get all deleted notes (for trash view in future).
     */
    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY updated_at DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    /**
     * Stage 6 Fix: Get deleted notes with tags and attachments using Room @Relation.
     */
    @Transaction
    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY updated_at DESC")
    fun getDeletedNotesWithRelations(): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Stage 10: Get all archived notes (not deleted, only archived).
     * Used for Archive Screen to show archived notes.
     */
    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE is_deleted = 0 AND is_archived = 1
        ORDER BY updated_at DESC
    """)
    fun getArchivedNotesWithRelations(): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Insert a new note. Returns the generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    /**
     * Update an existing note.
     */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * Hard delete a note from database.
     * Use this only for permanent deletion (e.g., emptying trash).
     * For normal deletion, use soft delete (update is_deleted = 1).
     */
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    /**
     * Soft delete a note (set is_deleted = 1).
     * This is the preferred deletion method to support cloud sync and undo.
     */
    @Query("UPDATE notes SET is_deleted = 1, updated_at = :deletedAt WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, deletedAt: Long)

    /**
     * Toggle pin status for a note.
     */
    @Query("UPDATE notes SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id = :noteId")
    suspend fun updatePinStatus(noteId: Long, isPinned: Boolean, updatedAt: Instant)

    /**
     * Toggle archive status for a note.
     */
    @Query("UPDATE notes SET is_archived = :isArchived, updated_at = :updatedAt WHERE id = :noteId")
    suspend fun updateArchiveStatus(noteId: Long, isArchived: Boolean, updatedAt: Instant)

    /**
     * Get notes by tag ID (via join with cross-ref table).
     * This will be used in Stage 5 when tag filtering is implemented.
     */
    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.note_id
        WHERE note_tag_cross_ref.tag_id = :tagId AND notes.is_deleted = 0
        ORDER BY notes.is_pinned DESC, notes.created_at DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>>

    /**
     * Stage 6 Fix: Get notes by tag with tags and attachments using Room @Relation.
     */
    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.note_id
        WHERE note_tag_cross_ref.tag_id = :tagId AND notes.is_deleted = 0 AND notes.is_archived = 0
        ORDER BY notes.is_pinned DESC, notes.updated_at DESC
    """)
    fun getNotesByTagWithRelations(tagId: Long): Flow<List<NoteWithTagsAndAttachments>>

    /**
     * Get total count of active notes (for statistics).
     */
    @Query("SELECT COUNT(*) FROM notes WHERE is_deleted = 0")
    fun getActiveNotesCount(): Flow<Int>

    /**
     * Stage 30: Get a single note by its globally unique syncId.
     */
    @Query("SELECT * FROM notes WHERE sync_id = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): NoteEntity?

    /**
     * Stage 30: Assign a persistent syncId to a note.
     */
    @Query("UPDATE notes SET sync_id = :syncId WHERE id = :noteId")
    suspend fun updateSyncId(noteId: Long, syncId: String)
}
