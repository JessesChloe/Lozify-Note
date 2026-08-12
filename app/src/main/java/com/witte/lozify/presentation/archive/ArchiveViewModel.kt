package com.witte.lozify.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trash Screen (Stage 10, renamed from Archive in Stage 12).
 *
 * Manages trashed notes display and operations:
 * - Load trashed notes
 * - Restore to home
 * - Permanent delete
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * StateFlow of trashed notes, automatically updated from Room Flow.
     */
    val archivedNotes: StateFlow<List<Note>> = noteRepository.getTrashNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Restore a note from trash (back to home screen).
     */
    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.toggleTrashStatus(noteId, isInTrash = false)
        }
    }

    /**
     * Permanently delete a note from database.
     */
    fun deleteNotePermanently(noteId: Long) {
        viewModelScope.launch {
            noteRepository.hardDeleteNote(noteId)
        }
    }
}
