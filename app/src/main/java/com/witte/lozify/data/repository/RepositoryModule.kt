package com.witte.lozify.data.repository

import com.witte.lozify.data.local.dao.NoteDao
import com.witte.lozify.data.local.dao.TagDao
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 *
 * This module tells Hilt which implementation to use when a repository
 * interface is requested for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        tagRepositoryImpl: TagRepositoryImpl
    ): TagRepository
}
