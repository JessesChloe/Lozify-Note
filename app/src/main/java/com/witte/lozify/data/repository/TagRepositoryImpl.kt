package com.witte.lozify.data.repository

import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.dao.TagDao
import com.witte.lozify.data.local.dao.NoteDao
import com.witte.lozify.data.mapper.toDomainModel
import com.witte.lozify.data.mapper.toDomainModels
import com.witte.lozify.data.mapper.toEntity
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TagRepository interface.
 *
 * Handles tag operations and tag-note associations.
 *
 * Stage 12: Added dangerous tag operations (remove, delete with trash, rename).
 *
 * @property tagDao DAO for tag operations
 * @property noteDao DAO for note operations (needed for content regex replacement)
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val noteDao: NoteDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { it.toDomainModels() }
    }

    override fun getTagById(tagId: Long): Flow<Tag?> {
        return tagDao.getTagById(tagId).map { it?.toDomainModel() }
    }

    override suspend fun getTagByName(name: String): Tag? {
        return tagDao.getTagByName(name)?.toDomainModel()
    }

    override fun searchTags(query: String): Flow<List<Tag>> {
        return tagDao.searchTags(query).map { it.toDomainModels() }
    }

    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> {
        return tagDao.getTagsForNote(noteId).map { it.toDomainModels() }
    }

    override suspend fun getOrCreateTag(name: String): Long {
        // Try to find existing tag
        val existingTag = tagDao.getTagByName(name)
        return if (existingTag != null) {
            existingTag.id
        } else {
            // Create new tag
            val newTag = Tag(
                id = 0,
                name = name,
                createdAt = Instant.now(),
                usageCount = 0
            )
            tagDao.insertTag(newTag.toEntity())
        }
    }

    override suspend fun insertTag(tag: Tag): Long {
        return tagDao.insertTag(tag.toEntity())
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun deleteTag(tagId: Long) {
        // Get tag first, then delete
        val tag = tagDao.getTagById(tagId).first()
        tag?.let {
            tagDao.deleteTag(it)
        }
    }

    override suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        // Stage 5 Bug Fix: Remove manual increment/decrement
        // TagDao.getAllTags() now uses real-time COUNT(*) from cross-ref table

        // Remove all existing associations
        tagDao.deleteAllTagsForNote(noteId)

        // Add new associations
        tagIds.forEach { tagId ->
            tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
        }
    }

    override fun getTagsCount(): Flow<Int> {
        return tagDao.getTagsCount()
    }

    /**
     * Stage 12: Remove tag from all notes using regex replacement.
     *
     * Strategy:
     * 1. Find all notes containing the tag via cross-ref table
     * 2. Use word boundary regex to replace #tagName with empty string
     * 3. Clean up extra spaces after removal
     * 4. Update note content and remove tag associations
     *
     * Regex Pattern: (?<=\s|^)#tagName(?=\s|$)
     * - Ensures tag is surrounded by whitespace or string boundaries
     * - Prevents damaging #task_list when removing #task
     */
    override suspend fun removeTagFromAllNotes(tagName: String) {
        // Get tag ID first
        val tag = tagDao.getTagByName(tagName) ?: return

        // Get all notes with this tag
        val notes = noteDao.getNotesByTag(tag.id).first()

        // Remove tag from each note's content
        notes.forEach { noteEntity ->
            // Build regex pattern matching tag boundaries
            val tagPattern = Regex("""#${Regex.escape(tagName)}(?![a-zA-Z0-9\u4e00-\u9fa5_])""")

            // Replace tag with empty string
            var updatedContent = noteEntity.content.replace(tagPattern, "")

            // Clean up extra spaces (multiple consecutive spaces → single space)
            updatedContent = updatedContent.replace(Regex("""\s{2,}"""), " ").trim()

            // Update note entity
            val updatedEntity = noteEntity.copy(
                content = updatedContent,
                updatedAt = Instant.now()
            )
            noteDao.updateNote(updatedEntity)
        }

        // Remove all tag associations (CASCADE will clean up cross-ref)
        tagDao.deleteAllTagsForNote(tag.id)

        // Optionally delete the tag entity itself if no longer used
        // (Check if usage count is 0 after removal)
        val updatedTag = tagDao.getTagById(tag.id).first()
        if (updatedTag != null && updatedTag.usageCount == 0) {
            tagDao.deleteTag(updatedTag)
        }
    }

    /**
     * Stage 12: Delete tag and move all associated notes to trash.
     *
     * Strategy:
     * 1. Find all notes with this tag
     * 2. Set is_archived = 1 for all these notes (soft delete to trash)
     * 3. Delete the tag entity (CASCADE removes associations)
     */
    override suspend fun deleteTagAndMoveNotesToTrash(tagId: Long) {
        // Get all notes with this tag
        val notes = noteDao.getNotesByTag(tagId).first()

        // Move all notes to trash (soft delete)
        val now = System.currentTimeMillis()
        notes.forEach { noteEntity ->
            noteDao.softDeleteNote(noteEntity.id, now)
        }

        // Delete the tag entity (CASCADE will remove cross-ref entries)
        val tag = tagDao.getTagById(tagId).first()
        tag?.let { tagDao.deleteTag(it) }
    }

    /**
     * Stage 12 & 13: Rename tag across all notes using regex replacement and update icon.
     *
     * Strategy:
     * 1. Get existing tag entity
     * 2. If tag name changed, replace #oldName with #newName in note contents
     * 3. Update tag entity itself with new name and new icon
     *
     * Regex Pattern: (?<![a-zA-Z0-9])#oldName(?![a-zA-Z0-9\u4e00-\u9fa5_])
     * - Ensures tag is not preceded by ASCII alphanumeric
     * - Ensures tag is not followed by alphanumeric/CJK characters
     */
    override suspend fun renameTagInAllNotes(
        tagId: Long,
        oldName: String,
        newName: String,
        newIcon: String?
    ) {
        // Get tag entity first
        val oldTag = tagDao.getTagById(tagId).first() ?: return

        // If name changed, update tag name in each note's content
        if (oldName != newName) {
            val notes = noteDao.getNotesByTag(tagId).first()

            notes.forEach { noteEntity ->
                // Build regex pattern matching tag boundaries for old tag
                val oldTagPattern = Regex("""#${Regex.escape(oldName)}(?![a-zA-Z0-9\u4e00-\u9fa5_])""")

                // Replace with new tag name
                val updatedContent = noteEntity.content.replace(oldTagPattern, "#$newName")

                // Update note entity
                val updatedEntity = noteEntity.copy(
                    content = updatedContent,
                    updatedAt = Instant.now()
                )
                noteDao.updateNote(updatedEntity)
            }
        }

        // Update tag entity itself (name and icon)
        val updatedTag = oldTag.copy(name = newName, icon = newIcon)
        tagDao.updateTag(updatedTag)
    }

    /**
     * Stage 16: Toggle pin status of a tag.
     */
    override suspend fun togglePinTag(tagId: Long, isPinned: Boolean) {
        tagDao.updatePinStatus(tagId, isPinned)
    }
}
