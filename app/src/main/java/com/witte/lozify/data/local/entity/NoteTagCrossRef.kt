package com.witte.lozify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing the many-to-many relationship between notes and tags.
 *
 * This junction table implements CASCADE DELETE:
 * - When a note is deleted, all its tag associations are automatically removed
 * - When a tag is deleted, all its note associations are automatically removed
 *
 * Composite primary key (note_id, tag_id) ensures no duplicate associations.
 *
 * Indexes:
 * - note_id: For querying all tags of a note
 * - tag_id: For querying all notes with a specific tag
 */
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["note_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"]),
        Index(value = ["tag_id"])
    ]
)
data class NoteTagCrossRef(
    @ColumnInfo(name = "note_id")
    val noteId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long
)
