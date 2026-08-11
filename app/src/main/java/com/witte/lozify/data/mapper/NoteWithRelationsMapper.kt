package com.witte.lozify.data.mapper

import com.witte.lozify.data.local.model.NoteWithTagsAndAttachments
import com.witte.lozify.domain.model.Note

/**
 * Mapper extensions for NoteWithTagsAndAttachments to Domain Model.
 *
 * Stage 6 Fix: Clean mapper for Room @Relation pattern.
 * Stage 8 Fix: Added mapping for outgoingRelations and incomingRelations.
 */

/**
 * Convert Room relation object to Domain Note with all associations.
 */
fun NoteWithTagsAndAttachments.toDomainModel(): Note {
    return note.toDomainModel().copy(
        tags = tags.toDomainModels(),
        attachments = attachments.toDomainModels(),
        outgoingRelations = outgoingRelations.toDomainModels(),
        incomingRelations = incomingRelations.toDomainModels()
    )
}

/**
 * Convert list of Room relation objects to Domain Notes.
 */
fun List<NoteWithTagsAndAttachments>.toDomainModels(): List<Note> {
    return map { it.toDomainModel() }
}
