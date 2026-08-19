package com.witte.lozify.data.mapper

import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.domain.model.Note

/**
 * Mapper extension functions for NoteEntity <-> Note conversion.
 *
 * These mappers handle the transformation between:
 * - Room entities (data layer) - used for database persistence
 * - Domain models (domain layer) - used for business logic
 *
 * Note: Complete Note mapping with relations (tags, attachments) is handled
 * in repository layer which combines data from multiple tables.
 */

/**
 * Convert NoteEntity to Domain Note model (basic fields only).
 * For complete Note with relations, use repository layer methods.
 */
fun NoteEntity.toDomainModel(): Note {
    return Note(
        id = id,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        isDeleted = isDeleted,
        syncId = syncId ?: "",
        tags = emptyList(), // Populated in repository layer
        attachments = emptyList(), // Populated in repository layer
        outgoingRelations = emptyList(), // Populated in repository layer
        incomingRelations = emptyList() // Populated in repository layer
    )
}

/**
 * Convert Domain Note to NoteEntity for database storage.
 * Only basic note fields are mapped - relations are handled separately.
 */
fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        isDeleted = isDeleted,
        syncId = if (syncId.isNotBlank()) syncId else java.util.UUID.randomUUID().toString(),
        lastSyncedAt = null
    )
}

/**
 * Batch convert list of NoteEntity to list of Note.
 */
fun List<NoteEntity>.toDomainModels(): List<Note> {
    return map { it.toDomainModel() }
}
