package com.witte.lozify.domain.repository

import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Note operations.
 *
 * ⚠️ PURE KOTLIN INTERFACE - NO ANDROID DEPENDENCIES
 * Defines the contract for note data access at the domain layer.
 * Implementation resides in data layer (data/repository package).
 *
 * Uses Flow for reactive data streams and suspend for async operations.
 */
interface NoteRepository {

    /**
     * Get all notes that are not deleted, sorted by pinned status and creation time.
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Get a single note by ID with all related data (tags, attachments, relations).
     */
    fun getNoteById(noteId: Long): Flow<Note?>

    /**
     * Search notes by content.
     */
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * Get all pinned notes.
     */
    fun getPinnedNotes(): Flow<List<Note>>

    /**
     * Get all deleted notes (trash view).
     */
    fun getDeletedNotes(): Flow<List<Note>>

    /**
     * Get notes filtered by a specific tag.
     */
    fun getNotesByTag(tagId: Long): Flow<List<Note>>

    /**
     * Insert a new note. Returns the generated note ID.
     * Also handles tag extraction and attachment saving.
     */
    suspend fun insertNote(note: Note): Long

    /**
     * Update an existing note.
     * Updates note content, tags, attachments, and relations.
     */
    suspend fun updateNote(note: Note)

    /**
     * Soft delete a note (set isDeleted = true).
     * Preferred method for deletion to support undo and cloud sync.
     */
    suspend fun softDeleteNote(noteId: Long)

    /**
     * Hard delete a note from database.
     * Use only for permanent deletion (e.g., emptying trash).
     */
    suspend fun hardDeleteNote(noteId: Long)

    /**
     * Toggle pin status for a note.
     */
    suspend fun togglePinStatus(noteId: Long, isPinned: Boolean)

    /**
     * Get total count of active notes.
     */
    fun getActiveNotesCount(): Flow<Int>
}
