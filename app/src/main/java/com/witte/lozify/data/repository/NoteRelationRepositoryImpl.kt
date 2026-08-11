package com.witte.lozify.data.repository

import com.witte.lozify.data.local.dao.NoteRelationDao
import com.witte.lozify.data.local.entity.NoteRelationEntity
import com.witte.lozify.domain.repository.NoteRelationRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Implementation of NoteRelationRepository.
 *
 * Stage 8: Handles note relation persistence using Room DAO.
 */
class NoteRelationRepositoryImpl @Inject constructor(
    private val noteRelationDao: NoteRelationDao
) : NoteRelationRepository {

    override suspend fun addRelation(fromNoteId: Long, toNoteId: Long, mentionText: String) {
        val relation = NoteRelationEntity(
            fromNoteId = fromNoteId,
            toNoteId = toNoteId,
            mentionText = mentionText,
            createdAt = Instant.now()
        )
        noteRelationDao.insertRelation(relation)
    }

    override suspend fun deleteRelationsForNote(noteId: Long) {
        noteRelationDao.deleteAllOutgoingRelations(noteId)
    }

    override suspend fun getOutgoingRelations(noteId: Long): List<Long> {
        return noteRelationDao.getOutgoingRelations(noteId)
            .first()
            .map { it.toNoteId }
    }

    override suspend fun getIncomingRelations(noteId: Long): List<Long> {
        return noteRelationDao.getIncomingRelations(noteId)
            .first()
            .map { it.fromNoteId }
    }
}
