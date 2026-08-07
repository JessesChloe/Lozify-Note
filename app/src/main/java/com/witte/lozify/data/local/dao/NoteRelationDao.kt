package com.witte.lozify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.witte.lozify.data.local.entity.NoteRelationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for NoteRelation (@mention) operations.
 *
 * Handles bi-directional note relationships created via @mentions.
 * Supports querying both outgoing relations (what this note references)
 * and incoming relations (backlinks - what references this note).
 */
@Dao
interface NoteRelationDao {

    /**
     * Get all outgoing relations from a note (notes that this note @mentions).
     */
    @Query("SELECT * FROM note_relations WHERE from_note_id = :noteId ORDER BY created_at DESC")
    fun getOutgoingRelations(noteId: Long): Flow<List<NoteRelationEntity>>

    /**
     * Get all incoming relations to a note (backlinks - notes that @mention this note).
     */
    @Query("SELECT * FROM note_relations WHERE to_note_id = :noteId ORDER BY created_at DESC")
    fun getIncomingRelations(noteId: Long): Flow<List<NoteRelationEntity>>

    /**
     * Get a specific relation between two notes.
     * Used to check if relation already exists before inserting.
     */
    @Query("""
        SELECT * FROM note_relations
        WHERE from_note_id = :fromNoteId AND to_note_id = :toNoteId
        LIMIT 1
    """)
    suspend fun getRelation(fromNoteId: Long, toNoteId: Long): NoteRelationEntity?

    /**
     * Get count of outgoing relations for a note.
     */
    @Query("SELECT COUNT(*) FROM note_relations WHERE from_note_id = :noteId")
    suspend fun getOutgoingRelationsCount(noteId: Long): Int

    /**
     * Get count of incoming relations (backlinks) for a note.
     */
    @Query("SELECT COUNT(*) FROM note_relations WHERE to_note_id = :noteId")
    suspend fun getIncomingRelationsCount(noteId: Long): Int

    /**
     * Insert a new note relation. Returns the generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: NoteRelationEntity): Long

    /**
     * Insert multiple relations (batch operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<NoteRelationEntity>)

    /**
     * Delete a specific relation.
     */
    @Delete
    suspend fun deleteRelation(relation: NoteRelationEntity)

    /**
     * Delete all outgoing relations from a note.
     * Used when updating note content - remove old @mentions first.
     */
    @Query("DELETE FROM note_relations WHERE from_note_id = :noteId")
    suspend fun deleteAllOutgoingRelations(noteId: Long)

    /**
     * Delete a specific relation by note IDs.
     */
    @Query("DELETE FROM note_relations WHERE from_note_id = :fromNoteId AND to_note_id = :toNoteId")
    suspend fun deleteRelationByNoteIds(fromNoteId: Long, toNoteId: Long)

    /**
     * Get total count of all relations (for statistics).
     */
    @Query("SELECT COUNT(*) FROM note_relations")
    fun getRelationsCount(): Flow<Int>
}
