package com.witte.lozify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a note in the database.
 *
 * This table stores the core note data including content, metadata, and flags.
 * Follows soft-delete pattern (isDeleted flag) to support cloud sync in future stages.
 *
 * Indexes:
 * - created_at: For sorting notes by creation time
 * - updated_at: For syncing and sorting by last modification
 * - is_deleted: For filtering out deleted notes efficiently
 * - is_archived: For filtering out archived notes efficiently
 * - is_pinned: For sorting pinned notes to top
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["updated_at"]),
        Index(value = ["is_deleted"]),
        Index(value = ["is_archived"]),
        Index(value = ["is_pinned"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    /**
     * Sync metadata for future cloud sync implementation.
     * Null if never synced to cloud.
     */
    @ColumnInfo(name = "sync_id")
    val syncId: String? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Instant? = null
)
