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
}
