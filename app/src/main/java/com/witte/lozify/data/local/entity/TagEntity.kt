package com.witte.lozify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a tag in the database.
 *
 * This table stores tag metadata. Color is intentionally omitted as per ADR-004:
 * all tags use unified #4C88FF blue color in UI layer.
 *
 * Indexes:
 * - name: For fast tag lookup and autocomplete
 * - usage_count: For sorting tags by popularity
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["usage_count"])
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    /**
     * Number of notes currently associated with this tag.
     * Updated via triggers or application logic when associations change.
     */
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    /**
     * Optional emoji icon for this tag (e.g., "🏷️", "📌").
     * Null means use the default # symbol in UI.
     * Added in database version 3 (Stage 13).
     */
    @ColumnInfo(name = "icon")
    val icon: String? = null
)
