package com.witte.lozify.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.witte.lozify.data.local.entity.AttachmentEntity
import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.entity.TagEntity

/**
 * Room @Relation data class for loading Note with all associated Tags and Attachments.
 *
 * This replaces the fragile `combine(List<Flow>)` pattern that caused ClassCastException.
 * Room handles the JOIN queries and relationship loading efficiently in a single transaction.
 *
 * Stage 6 Fix: Standard Room architecture for relationship loading.
 */
data class NoteWithTagsAndAttachments(
    @Embedded val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "note_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "note_id"
    )
    val attachments: List<AttachmentEntity>
)
