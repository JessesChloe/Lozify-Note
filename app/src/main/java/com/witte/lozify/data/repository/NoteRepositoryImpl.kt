package com.witte.lozify.data.repository

import com.witte.lozify.data.local.dao.AttachmentDao
import com.witte.lozify.data.local.dao.NoteDao
import com.witte.lozify.data.local.dao.NoteRelationDao
import com.witte.lozify.data.local.dao.TagDao
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.mapper.toDomainModel
import com.witte.lozify.data.mapper.toDomainModels
import com.witte.lozify.data.mapper.toEntity
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NoteRepository interface.
 *
 * Handles complex operations involving multiple DAOs and combines data from
 * multiple tables to build complete Note domain models with relations.
 *
 * @property noteDao DAO for note operations
 * @property tagDao DAO for tag operations
 * @property attachmentDao DAO for attachment operations
 * @property relationDao DAO for note relation operations
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val attachmentDao: AttachmentDao,
    private val relationDao: NoteRelationDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { noteEntities ->
            noteEntities.map { noteEntity ->
                buildCompleteNote(noteEntity.id)
            }
        }.map { noteFlows ->
            // Combine all individual note flows into single list
            // For simplicity in MVP, we return basic notes without full relations
            noteFlows
        }
    }

    override fun getNoteById(noteId: Long): Flow<Note?> {
        return noteDao.getNoteById(noteId).map { noteEntity ->
            if (noteEntity == null) {
                null
            } else {
                // Build complete note with all relations
                val tags = tagDao.getTagsForNote(noteId)
                val attachments = attachmentDao.getAttachmentsForNote(noteId)
                val outgoingRelations = relationDao.getOutgoingRelations(noteId)
                val incomingRelations = relationDao.getIncomingRelations(noteId)

                // Combine all flows
                combine(
                    tags,
                    attachments,
                    outgoingRelations,
                    incomingRelations
                ) { tagList, attachmentList, outgoing, incoming ->
                    noteEntity.toDomainModel().copy(
                        tags = tagList.toDomainModels(),
                        attachments = attachmentList.toDomainModels(),
                        outgoingRelations = outgoing.toDomainModels(),
                        incomingRelations = incoming.toDomainModels()
                    )
                }
            }
        }.flatMapConcat { it ?: kotlinx.coroutines.flow.flowOf(null) }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { it.toDomainModels() }
    }

    override fun getPinnedNotes(): Flow<List<Note>> {
        return noteDao.getPinnedNotes().map { it.toDomainModels() }
    }

    override fun getDeletedNotes(): Flow<List<Note>> {
        return noteDao.getDeletedNotes().map { it.toDomainModels() }
    }

    override fun getNotesByTag(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTag(tagId).map { it.toDomainModels() }
    }

    override suspend fun insertNote(note: Note): Long {
        // Insert note entity
        val noteId = noteDao.insertNote(note.toEntity())

        // Insert tags and create associations
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                // Create new tag
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
            tagDao.incrementUsageCount(tagId)
        }

        // Insert attachments
        note.attachments.forEach { attachment ->
            attachmentDao.insertAttachment(attachment.copy(noteId = noteId).toEntity())
        }

        // Insert relations
        note.outgoingRelations.forEach { relation ->
            relationDao.insertRelation(relation.copy(fromNoteId = noteId).toEntity())
        }

        return noteId
    }

    override suspend fun updateNote(note: Note) {
        // Update note entity
        noteDao.updateNote(note.toEntity())

        // Update tags: remove old associations, add new ones
        tagDao.deleteAllTagsForNote(note.id)
        note.tags.forEach { tag ->
            val tagId = if (tag.id == 0L) {
                tagDao.insertTag(tag.toEntity())
            } else {
                tag.id
            }
            tagDao.insertNoteTagCrossRef(NoteTagCrossRef(note.id, tagId))
            tagDao.incrementUsageCount(tagId)
        }

        // Update attachments: delete old ones, insert new ones
        attachmentDao.deleteAllAttachmentsForNote(note.id)
        note.attachments.forEach { attachment ->
            attachmentDao.insertAttachment(attachment.toEntity())
        }

        // Update relations: remove old outgoing relations, add new ones
        relationDao.deleteAllOutgoingRelations(note.id)
        note.outgoingRelations.forEach { relation ->
            relationDao.insertRelation(relation.toEntity())
        }
    }

    override suspend fun softDeleteNote(noteId: Long) {
        val now = Instant.now().toEpochMilli()
        noteDao.softDeleteNote(noteId, now)
    }

    override suspend fun hardDeleteNote(noteId: Long) {
        // Get note entity first
        val noteEntity = noteDao.getNoteById(noteId)
        // Note: In a real implementation, we should also delete physical attachment files
        // This will be implemented in Stage 6 when we add image handling
        // For now, CASCADE will handle database cleanup
        noteEntity.collect { entity ->
            entity?.let { noteDao.deleteNote(it) }
        }
    }

    override suspend fun togglePinStatus(noteId: Long, isPinned: Boolean) {
        val now = Instant.now().toEpochMilli()
        noteDao.updatePinStatus(noteId, isPinned, now)
    }

    override fun getActiveNotesCount(): Flow<Int> {
        return noteDao.getActiveNotesCount()
    }

    /**
     * Helper function to build a complete Note with all relations.
     * Used internally to avoid code duplication.
     */
    private suspend fun buildCompleteNote(noteId: Long): Note {
        // This is a simplified version for MVP
        // In production, we'd use more efficient joining strategies
        return noteDao.getNoteById(noteId).map { noteEntity ->
            noteEntity?.toDomainModel() ?: Note(
                id = 0,
                content = "",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }.first()
    }
}
