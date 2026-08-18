package com.witte.lozify.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.witte.lozify.data.local.converter.Converters
import com.witte.lozify.data.local.dao.AttachmentDao
import com.witte.lozify.data.local.dao.NoteDao
import com.witte.lozify.data.local.dao.NoteRelationDao
import com.witte.lozify.data.local.dao.TagDao
import com.witte.lozify.data.local.entity.AttachmentEntity
import com.witte.lozify.data.local.entity.NoteEntity
import com.witte.lozify.data.local.entity.NoteRelationEntity
import com.witte.lozify.data.local.entity.NoteTagCrossRef
import com.witte.lozify.data.local.entity.TagEntity

/**
 * Room Database for Lozify application.
 *
 * Database Version: 3 (Added icon column to tags table)
 *
 * Contains 5 tables:
 * - notes: Core note data
 * - tags: Tag metadata (with optional emoji icon)
 * - note_tag_cross_ref: Many-to-many note-tag associations
 * - attachments: Image attachments for notes
 * - note_relations: @mention relationships between notes
 *
 * Schema export location: app/schemas/
 * Configured in app/build.gradle.kts via KSP arg
 */
@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AttachmentEntity::class,
        NoteRelationEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LozifyDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteRelationDao(): NoteRelationDao

    companion object {
        const val DATABASE_NAME = "lozify_database"

        /**
         * Migration from version 2 to 3: Add icon column to tags table.
         * Stage 13: Support emoji icons for tags.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tags ADD COLUMN icon TEXT")
            }
        }

        /**
         * Migration from version 3 to 4: Add is_pinned and pin_order columns to tags table.
         * Stage 16: Support pinned tags in drawer.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tags ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tags ADD COLUMN pin_order INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
