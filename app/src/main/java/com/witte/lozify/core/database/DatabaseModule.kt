package com.witte.lozify.core.database

import android.content.Context
import androidx.room.Room
import com.witte.lozify.data.local.dao.AttachmentDao
import com.witte.lozify.data.local.dao.NoteDao
import com.witte.lozify.data.local.dao.NoteRelationDao
import com.witte.lozify.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances.
 *
 * All database-related dependencies are scoped as Singleton
 * to ensure single database instance throughout app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLozifyDatabase(
        @ApplicationContext context: Context
    ): LozifyDatabase {
        return Room.databaseBuilder(
            context,
            LozifyDatabase::class.java,
            LozifyDatabase.DATABASE_NAME
        )
            // Enable multi-instance invalidation for cross-process scenarios
            .enableMultiInstanceInvalidation()
            // Stage 6 Fix: Temporary destructive migration for development
            // This prevents crashes from old database schema without attachments table
            // TODO: Remove before production - use explicit Migration classes
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: LozifyDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: LozifyDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideAttachmentDao(database: LozifyDatabase): AttachmentDao {
        return database.attachmentDao()
    }

    @Provides
    @Singleton
    fun provideNoteRelationDao(database: LozifyDatabase): NoteRelationDao {
        return database.noteRelationDao()
    }
}
