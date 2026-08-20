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
import com.witte.lozify.domain.model.NoteThread
import com.witte.lozify.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
 * Stage 6 Fix: Refactored to use Room @Relation pattern instead of fragile combine(List<Flow>).
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
    private val relationDao: NoteRelationDao,
    private val preferencesManager: com.witte.lozify.core.preferences.UserPreferencesManager
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotesWithRelations().map { notesWithRelations ->
            notesWithRelations.toFilteredDomainModels()
        }
    }

    override fun getNoteById(noteId: Long): Flow<Note?> {
        return noteDao.getNoteById(noteId).map { noteEntity ->
            noteEntity?.toDomainModel()
        }
    }

    override fun getNoteThread(noteId: Long): Flow<NoteThread?> {
        return noteDao.getAllNotesWithRelations().map { notesWithRelations ->
            val allNotes = notesWithRelations.toFilteredDomainModels()
            val allNotesMap = allNotes.associateBy { it.id }
            val mainNote = allNotesMap[noteId] ?: return@map null

            // Parents: notes mentioning mainNote (incoming relations from active notes)
            val parentIds = mainNote.incomingRelations.map { it.fromNoteId }
            val parents = parentIds.mapNotNull { allNotesMap[it] }

            // Children: notes mentioned by mainNote (outgoing relations to active notes)
            val childIds = mainNote.outgoingRelations.map { it.toNoteId }
            val children = childIds.mapNotNull { allNotesMap[it] }

            NoteThread(
                parents = parents,
                mainNote = mainNote,
                children = children
            )
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotesWithRelations(query).map { notesWithRelations ->
            notesWithRelations.toFilteredDomainModels()
        }
    }

    override fun getPinnedNotes(): Flow<List<Note>> {
        return noteDao.getPinnedNotesWithRelations().map { notesWithRelations ->
            notesWithRelations.toFilteredDomainModels()
        }
    }

    override fun getDeletedNotes(): Flow<List<Note>> {
        return noteDao.getDeletedNotesWithRelations().map { notesWithRelations ->
            notesWithRelations.toDomainModels()
        }
    }

    override fun getTrashNotes(): Flow<List<Note>> {
        return noteDao.getDeletedNotesWithRelations().map { notesWithRelations ->
            notesWithRelations.toDomainModels()
        }
    }

    override fun getAllNotesIncludingDeleted(): Flow<List<Note>> {
        return noteDao.getAllNotesWithRelationsIncludingDeleted().map { notesWithRelations ->
            notesWithRelations.toDomainModels()
        }
    }

    override fun getNotesByTag(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTagWithRelations(tagId).map { notesWithRelations ->
            notesWithRelations.toFilteredDomainModels()
        }
    }

    private fun List<com.witte.lozify.data.local.model.NoteWithTagsAndAttachments>.toFilteredDomainModels(): List<Note> {
        val activeIds = map { it.note.id }.toSet()
        return map { nwr ->
            nwr.toDomainModel().copy(
                outgoingRelations = nwr.outgoingRelations
                    .filter { it.toNoteId in activeIds }
                    .toDomainModels(),
                incomingRelations = nwr.incomingRelations
                    .filter { it.fromNoteId in activeIds }
                    .toDomainModels()
            )
        }
    }

    override suspend fun insertNote(note: Note): Long {
        // Stage 5 Bug Fix: Remove manual incrementUsageCount - now calculated via COUNT(*)

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
        // Stage 5 Bug Fix: Properly handle tag associations without manual increment/decrement
        // The new TagDao.getAllTags() uses real-time COUNT(*), so we don't need manual updates

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
        }

        // Note: Attachments are managed by AttachmentRepository explicitly to prevent accidental file/DB wipe

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
        // Stage 7 Bug Fix: Ensure cascade deletion of all related entities
        // Even though ForeignKey.CASCADE is configured, explicitly clean up to be safe

        // Get note entity first
        val noteEntity = noteDao.getNoteByIdDirect(noteId) ?: noteDao.getNoteById(noteId).first()

        noteEntity?.let { entity ->
            // Record syncId in purged tombstones to ensure multi-device synchronization
            val syncId = entity.syncId
            if (!syncId.isNullOrBlank()) {
                preferencesManager.recordPurgedSyncIds(listOf(syncId))
            }

            // Delete all tag associations (CASCADE should handle this, but explicit is safer)
            tagDao.deleteAllTagsForNote(noteId)

            // Delete all attachments (CASCADE should handle this)
            attachmentDao.deleteAllAttachmentsForNote(noteId)

            // Delete all note relations (CASCADE should handle this)
            relationDao.deleteAllOutgoingRelations(noteId)
            relationDao.deleteAllIncomingRelations(noteId)

            // Finally delete the note entity
            noteDao.deleteNote(entity)

            // Stage 55: Clean up orphaned tags
            tagDao.cleanupOrphanedTags()
        }
    }

    override suspend fun emptyTrash() {
        val trashedNotes = noteDao.getDeletedNotesWithRelations().first()
        val syncIds = trashedNotes.mapNotNull { it.note.syncId }.filter { it.isNotBlank() }
        if (syncIds.isNotEmpty()) {
            preferencesManager.recordPurgedSyncIds(syncIds)
        }

        trashedNotes.forEach { noteWithRel ->
            hardDeleteNote(noteWithRel.note.id)
        }

        // Stage 55: Clean up orphaned tags
        tagDao.cleanupOrphanedTags()
    }

    override suspend fun togglePinStatus(noteId: Long, isPinned: Boolean) {
        noteDao.updatePinStatus(noteId, isPinned, Instant.now())
    }

    override suspend fun toggleTrashStatus(noteId: Long, isInTrash: Boolean) {
        if (isInTrash) {
            softDeleteNote(noteId)
        } else {
            restoreNote(noteId)
        }
    }

    override suspend fun restoreNote(noteId: Long) {
        noteDao.restoreNote(noteId, Instant.now())

        // Stage 55: Re-link tags from restored note content
        val note = noteDao.getNoteByIdDirect(noteId)
        if (note != null) {
            val tagNames = com.witte.lozify.core.common.RichTextUtils.stripFormatting(note.content)
                .let { clean ->
                    Regex("""#([a-zA-Z0-9\u4e00-\u9fa5_]+)""").findAll(clean).map { it.groupValues[1] }.distinct().toList()
                }
            tagNames.forEach { tagName ->
                val tagId = tagDao.getTagByName(tagName)?.id ?: tagDao.insertTag(
                    com.witte.lozify.data.local.entity.TagEntity(name = tagName, createdAt = Instant.now())
                )
                tagDao.insertNoteTagCrossRef(com.witte.lozify.data.local.entity.NoteTagCrossRef(noteId, tagId))
            }
        }
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
