package com.witte.lozify.domain.repository

/**
 * Repository interface for managing note relations (@mentions).
 *
 * Stage 8: Handles bidirectional note linking.
 */
interface NoteRelationRepository {

    /**
     * Add a relation from one note to another.
     *
     * @param fromNoteId Source note ID (the note containing the @mention)
     * @param toNoteId Target note ID (the mentioned note)
     * @param mentionText Display text of the mention
     */
    suspend fun addRelation(fromNoteId: Long, toNoteId: Long, mentionText: String)

    /**
     * Delete all outgoing relations for a note.
     *
     * Used when updating a note to clear old relations before inserting new ones.
     *
     * @param noteId Note ID whose outgoing relations should be deleted
     */
    suspend fun deleteRelationsForNote(noteId: Long)

    /**
     * Get all outgoing relations for a note (notes this note mentions).
     *
     * @param noteId Source note ID
     * @return List of target note IDs
     */
    suspend fun getOutgoingRelations(noteId: Long): List<Long>

    /**
     * Get all incoming relations for a note (notes that mention this note).
     *
     * @param noteId Target note ID
     * @return List of source note IDs
     */
    suspend fun getIncomingRelations(noteId: Long): List<Long>
}
