package com.witte.lozify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing the @mention relationship between notes.
 *
 * This table stores bi-directional note relations created via the @ feature:
 * - fromNoteId: The note containing the @mention
 * - toNoteId: The note being referenced
 * - mentionText: The display text shown in the @mention (e.g., "@Meeting Notes")
 *
 * CASCADE DELETE behavior:
 * - When either the source or target note is deleted, the relation is removed
 *
 * Indexes:
 * - from_note_id: For querying all outgoing relations from a note
 * - to_note_id: For querying all incoming relations to a note (backlinks)
 */
@Entity(
    tableName = "note_relations",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["from_note_id"]),
        Index(value = ["to_note_id"])
    ]
)
data class NoteRelationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "from_note_id")
    val fromNoteId: Long,

    @ColumnInfo(name = "to_note_id")
    val toNoteId: Long,

    /**
     * The text displayed in the @mention link.
     * Usually the first line or excerpt of the target note.
     * Example: "@Meeting Notes" or "@Project Ideas"
     */
    @ColumnInfo(name = "mention_text")
    val mentionText: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
