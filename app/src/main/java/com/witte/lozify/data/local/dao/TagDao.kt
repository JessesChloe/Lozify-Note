package com.witte.lozify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tag operations.
 *
 * Handles tag CRUD and tag-note associations.
 * Uses Flow for reactive updates and suspend for async operations.
 */
@Dao
interface TagDao {

    /**
     * Get all tags with REAL usage count calculated from active note associations.
     *
     * Stage 5 Bug Fix: Replace manual increment/decrement with real-time COUNT(*).
     * This eliminates usage count drift caused by manual +1/-1 operations.
     */
    @Query("""
        SELECT
            tags.id,
            tags.name,
            tags.created_at,
            tags.icon,
            COUNT(DISTINCT note_tag_cross_ref.note_id) as usage_count
        FROM tags
        LEFT JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tag_id
        LEFT JOIN notes ON note_tag_cross_ref.note_id = notes.id AND notes.is_deleted = 0
        GROUP BY tags.id
        ORDER BY usage_count DESC, tags.name ASC
    """)
    fun getAllTags(): Flow<List<TagEntity>>

    /**
     * Get a single tag by ID.
     */
    @Query("SELECT * FROM tags WHERE id = :tagId")
    fun getTagById(tagId: Long): Flow<TagEntity?>

    /**
     * Get a tag by exact name (for lookup when parsing #tags).
     */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    /**
     * Search tags by name prefix (for autocomplete).
     */
    @Query("SELECT * FROM tags WHERE name LIKE :query || '%' ORDER BY usage_count DESC LIMIT 10")
    fun searchTags(query: String): Flow<List<TagEntity>>

    /**
     * Get all tags associated with a specific note.
     */
    @Transaction
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tag_id
        WHERE note_tag_cross_ref.note_id = :noteId
        ORDER BY tags.name ASC
    """)
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

    /**
     * Insert a new tag. Returns the generated ID.
     * If tag already exists (name conflict), replaces it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    /**
     * Update an existing tag.
     */
    @Update
    suspend fun updateTag(tag: TagEntity)

    /**
     * Delete a tag. CASCADE will automatically remove all associations.
     */
    @Delete
    suspend fun deleteTag(tag: TagEntity)

    /**
     * Increment usage count for a tag.
     */
    @Query("UPDATE tags SET usage_count = usage_count + 1 WHERE id = :tagId")
    suspend fun incrementUsageCount(tagId: Long)

    /**
     * Decrement usage count for a tag (when note-tag association is removed).
     */
    @Query("UPDATE tags SET usage_count = usage_count - 1 WHERE id = :tagId AND usage_count > 0")
    suspend fun decrementUsageCount(tagId: Long)

    /**
     * Insert a note-tag association.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    /**
     * Delete a note-tag association.
     */
    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)

    /**
     * Delete all tag associations for a specific note.
     * Used when updating note tags - delete old associations first.
     */
    @Query("DELETE FROM note_tag_cross_ref WHERE note_id = :noteId")
    suspend fun deleteAllTagsForNote(noteId: Long)

    /**
     * Get tags that have zero usage (orphaned tags).
     * Can be used for cleanup in future.
     */
    @Query("SELECT * FROM tags WHERE usage_count = 0")
    fun getUnusedTags(): Flow<List<TagEntity>>

    /**
     * Get total count of tags (for statistics).
     */
    @Query("SELECT COUNT(*) FROM tags")
    fun getTagsCount(): Flow<Int>
}
