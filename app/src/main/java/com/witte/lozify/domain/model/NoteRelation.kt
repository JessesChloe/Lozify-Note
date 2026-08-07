package com.witte.lozify.domain.model

import java.time.Instant

/**
 * Domain model representing a bi-directional note relation (@mention).
 *
 * ⚠️ PURE KOTLIN - NO ANDROID DEPENDENCIES
 * This is a business logic model, completely decoupled from Room/Android framework.
 *
 * Represents a directed link from one note to another via @mention feature:
 * - fromNoteId: Note containing the @mention
 * - toNoteId: Note being referenced
 * - mentionText: Display text for the mention (e.g., "@Meeting Notes")
 *
 * @property id Unique identifier (0 for unsaved relations)
 * @property fromNoteId Source note ID
 * @property toNoteId Target note ID
 * @property mentionText Display text shown in @mention
 * @property createdAt Creation timestamp
 */
data class NoteRelation(
    val id: Long = 0,
    val fromNoteId: Long,
    val toNoteId: Long,
    val mentionText: String,
    val createdAt: Instant
) {
    /**
     * Check if this relation forms a self-reference (note mentioning itself).
     * Should be prevented at application level.
     */
    fun isSelfReference(): Boolean = fromNoteId == toNoteId
}
