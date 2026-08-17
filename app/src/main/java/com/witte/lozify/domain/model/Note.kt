package com.witte.lozify.domain.model

import java.time.Instant

/**
 * Domain model representing a note.
 *
 * ⚠️ PURE KOTLIN - NO ANDROID DEPENDENCIES
 * This is a business logic model, completely decoupled from Room/Android framework.
 *
 * @property id Unique identifier (0 for unsaved notes)
 * @property content Full text content of the note
 * @property createdAt Creation timestamp
 * @property updatedAt Last modification timestamp
 * @property isPinned Whether note is pinned to top of feed
 * @property isArchived Whether note is archived (hidden from main feed)
 * @property isDeleted Soft delete flag (for trash/undo functionality)
 * @property tags List of associated tags
 * @property attachments List of image attachments
 * @property outgoingRelations Notes that this note @mentions
 * @property incomingRelations Notes that @mention this note (backlinks)
 */
data class Note(
    val id: Long = 0,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val outgoingRelations: List<NoteRelation> = emptyList(),
    val incomingRelations: List<NoteRelation> = emptyList()
) {
    /**
     * Get the first line or first 50 characters of content as title.
     * Used for @mention display text and list previews.
     */
    fun getTitle(): String {
        val firstLine = content.lines().firstOrNull()?.trim() ?: ""
        return if (firstLine.length > 50) {
            firstLine.take(50) + "..."
        } else {
            firstLine.ifEmpty { "未命名笔记" }
        }
    }

    /**
     * Check if note has any attachments.
     */
    fun hasAttachments(): Boolean = attachments.isNotEmpty()

    /**
     * Check if note has any tags.
     */
    fun hasTags(): Boolean = tags.isNotEmpty()

    /**
     * Check if note is empty (no content and no attachments).
     */
    fun isEmpty(): Boolean = content.isBlank() && attachments.isEmpty()
}
