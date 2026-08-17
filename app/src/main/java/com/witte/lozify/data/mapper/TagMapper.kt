package com.witte.lozify.data.mapper

import com.witte.lozify.data.local.entity.TagEntity
import com.witte.lozify.domain.model.Tag

/**
 * Mapper extension functions for TagEntity <-> Tag conversion.
 *
 * These mappers handle the transformation between:
 * - Room entities (data layer) - used for database persistence
 * - Domain models (domain layer) - used for business logic
 */

/**
 * Convert TagEntity to Domain Tag model.
 */
fun TagEntity.toDomainModel(): Tag {
    return Tag(
        id = id,
        name = name,
        createdAt = createdAt,
        usageCount = usageCount,
        icon = icon
    )
}

/**
 * Convert Domain Tag to TagEntity for database storage.
 */
fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        usageCount = usageCount,
        icon = icon
    )
}

/**
 * Batch convert list of TagEntity to list of Tag.
 */
fun List<TagEntity>.toDomainModels(): List<Tag> {
    return map { it.toDomainModel() }
}
