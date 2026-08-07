package com.witte.lozify.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for HomeScreen.
 *
 * Manages note list state and operations like pin/delete.
 * Observes Room database via Flow for reactive updates.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * UI state representing the home screen data.
     */
    data class HomeUiState(
        val notes: List<Note> = emptyList(),
        val isLoading: Boolean = false
    )

    /**
     * Reactive state flow of notes from database.
     * Automatically updates UI when database changes.
     */
    val uiState: StateFlow<HomeUiState> = noteRepository.getAllNotes()
        .map { notes ->
            HomeUiState(
                notes = notes,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    /**
     * Toggle pin status for a note.
     */
    fun togglePinStatus(noteId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            noteRepository.togglePinStatus(noteId, !isPinned)
        }
    }

    /**
     * Soft delete a note (move to trash).
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.softDeleteNote(noteId)
        }
    }
}
