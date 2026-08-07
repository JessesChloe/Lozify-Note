package com.witte.lozify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing an image attachment for a note.
 *
 * This table implements the one-to-many relationship between notes and images.
 * Each note can have 0-9 images stored in app's internal sandbox storage.
 *
 * Storage Strategy (as per ADR-007):
 * - All images are stored in app-private directory (Context.filesDir/images/)
 * - filePath stores the relative path within sandbox (e.g., "images/note_123_img_0.jpg")
 * - Original URIs from gallery are copied and compressed on insert
 * - Images are automatically deleted when parent note is deleted (CASCADE)
 *
 * Display Order:
 * - displayOrder field ensures consistent image ordering in 3-column grid
 * - Values typically start from 0, incremented for each additional image
 *
 * Indexes:
 * - note_id: For fast retrieval of all images belonging to a note
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "note_id")
    val noteId: Long,

    /**
     * Relative file path within app's internal storage.
     * Example: "images/note_123_img_0.jpg"
     */
    @ColumnInfo(name = "file_path")
    val filePath: String,

    /**
     * Display order in the 3-column grid (0-indexed).
     */
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    /**
     * Optional MIME type (e.g., "image/jpeg", "image/png").
     * Used for proper image loading with Coil.
     */
    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    /**
     * Optional file size in bytes.
     * Can be used for storage management in future.
     */
    @ColumnInfo(name = "file_size")
    val fileSize: Long? = null
)
