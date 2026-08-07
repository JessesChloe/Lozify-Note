package com.witte.lozify.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
 * Database Version: 1 (MVP initial schema)
 *
 * Contains 5 tables:
 * - notes: Core note data
 * - tags: Tag metadata
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
    version = 1,
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
    }
}
