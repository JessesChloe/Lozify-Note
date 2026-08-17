package com.witte.lozify.domain.model

/**
 * Domain model representing a thread/context chain of notes.
 *
 * Stage 13: Thread View (上下文时间线视图)
 *
 * @param parents Notes that mention the main note (incoming relations / backlinks)
 * @param mainNote The focused target note
 * @param children Notes mentioned by the main note (outgoing relations)
 */
data class NoteThread(
    val parents: List<Note> = emptyList(),
    val mainNote: Note,
    val children: List<Note> = emptyList()
)
