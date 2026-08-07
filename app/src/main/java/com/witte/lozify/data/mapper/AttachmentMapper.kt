package com.witte.lozify.data.mapper

import com.witte.lozify.data.local.entity.AttachmentEntity
import com.witte.lozify.domain.model.Attachment

/**
 * Mapper extension functions for AttachmentEntity <-> Attachment conversion.
 *
 * These mappers handle the transformation between:
 * - Room entities (data layer) - used for database persistence
 * - Domain models (domain layer) - used for business logic
 */

/**
 * Convert AttachmentEntity to Domain Attachment model.
 */
fun AttachmentEntity.toDomainModel(): Attachment {
    return Attachment(
        id = id,
        noteId = noteId,
        filePath = filePath,
        displayOrder = displayOrder,
        createdAt = createdAt,
        mimeType = mimeType,
        fileSize = fileSize
    )
}

/**
 * Convert Domain Attachment to AttachmentEntity for database storage.
 */
fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = id,
        noteId = noteId,
        filePath = filePath,
        displayOrder = displayOrder,
        createdAt = createdAt,
        mimeType = mimeType,
        fileSize = fileSize
    )
}

/**
 * Batch convert list of AttachmentEntity to list of Attachment.
 */
fun List<AttachmentEntity>.toDomainModels(): List<Attachment> {
    return map { it.toDomainModel() }
}
