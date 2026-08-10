package com.witte.lozify.data.repository

import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.dao.TagDao
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
 * @property tagDao DAO for tag operations
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
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
}
