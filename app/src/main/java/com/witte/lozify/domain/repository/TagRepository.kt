package com.witte.lozify.domain.repository

import com.witte.lozify.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Tag operations.
 *
 * ⚠️ PURE KOTLIN INTERFACE - NO ANDROID DEPENDENCIES
 * Defines the contract for tag data access at the domain layer.
 * Implementation resides in data layer (data/repository package).
 *
 * Uses Flow for reactive data streams and suspend for async operations.
 */
interface TagRepository {

    /**
     * Get all tags ordered by usage count (most popular first).
     */
    fun getAllTags(): Flow<List<Tag>>

    /**
     * Get a single tag by ID.
     */
    fun getTagById(tagId: Long): Flow<Tag?>

    /**
     * Get a tag by exact name (for lookup when parsing #tags).
     * Returns null if tag does not exist.
     */
    suspend fun getTagByName(name: String): Tag?

    /**
     * Search tags by name prefix (for autocomplete).
     */
    fun searchTags(query: String): Flow<List<Tag>>

    /**
     * Get all tags associated with a specific note.
     */
    fun getTagsForNote(noteId: Long): Flow<List<Tag>>

    /**
     * Get or create a tag by name.
     * If tag exists, returns existing tag. Otherwise creates new tag.
     * Returns the tag ID.
     */
    suspend fun getOrCreateTag(name: String): Long

    /**
     * Insert a new tag. Returns the generated tag ID.
     */
    suspend fun insertTag(tag: Tag): Long

    /**
     * Update an existing tag.
     */
    suspend fun updateTag(tag: Tag)

    /**
     * Delete a tag. CASCADE will remove all note-tag associations.
     */
    suspend fun deleteTag(tagId: Long)

    /**
     * Associate tags with a note.
     * Replaces existing associations - old tags are removed, new ones added.
     */
    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>)

    /**
     * Get total count of tags.
     */
    fun getTagsCount(): Flow<Int>

    /**
     * Stage 12: Remove tag from all notes (regex replace #tagName with empty string).
     * Does not delete the tag entity itself, only removes it from note content.
     *
     * @param tagName Tag name without # prefix
     */
    suspend fun removeTagFromAllNotes(tagName: String)

    /**
     * Stage 12: Delete tag and move all associated notes to trash.
     * Sets is_archived = 1 for all notes containing this tag.
     *
     * @param tagId Tag ID to delete
     */
    suspend fun deleteTagAndMoveNotesToTrash(tagId: Long)

    /**
     * Stage 12 & 13: Rename tag across all notes (regex replace #oldName with #newName)
     * and update optional emoji icon.
     *
     * @param tagId Tag ID to rename
     * @param oldName Current tag name without # prefix
     * @param newName New tag name without # prefix
     * @param newIcon Optional new emoji icon (null = default #)
     */
    suspend fun renameTagInAllNotes(tagId: Long, oldName: String, newName: String, newIcon: String? = null)

    /**
     * Stage 16: Toggle pin status of a tag.
     *
     * @param tagId Tag ID
     * @param isPinned New pin status
     */
    suspend fun togglePinTag(tagId: Long, isPinned: Boolean)

    /**
     * Cleanup orphaned unpinned tags that have 0 active note references.
     */
    suspend fun cleanupOrphanedTags()
}
