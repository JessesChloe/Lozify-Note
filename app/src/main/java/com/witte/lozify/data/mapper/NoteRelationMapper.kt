package com.witte.lozify.data.mapper

import com.witte.lozify.data.local.entity.NoteRelationEntity
import com.witte.lozify.domain.model.NoteRelation

/**
 * Mapper extension functions for NoteRelationEntity <-> NoteRelation conversion.
 *
 * These mappers handle the transformation between:
 * - Room entities (data layer) - used for database persistence
 * - Domain models (domain layer) - used for business logic
 */

/**
 * Convert NoteRelationEntity to Domain NoteRelation model.
 */
fun NoteRelationEntity.toDomainModel(): NoteRelation {
    return NoteRelation(
        id = id,
        fromNoteId = fromNoteId,
        toNoteId = toNoteId,
        mentionText = mentionText,
        createdAt = createdAt
    )
}

/**
 * Convert Domain NoteRelation to NoteRelationEntity for database storage.
 */
fun NoteRelation.toEntity(): NoteRelationEntity {
    return NoteRelationEntity(
        id = id,
        fromNoteId = fromNoteId,
        toNoteId = toNoteId,
        mentionText = mentionText,
        createdAt = createdAt
    )
}

/**
 * Batch convert list of NoteRelationEntity to list of NoteRelation.
 */
fun List<NoteRelationEntity>.toDomainModels(): List<NoteRelation> {
    return map { it.toDomainModel() }
}
