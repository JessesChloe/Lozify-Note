package com.witte.lozify.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for HomeScreen.
 *
 * Manages note list state and operations like pin/delete.
 * Observes Room database via Flow for reactive updates.
 *
 * Stage 4: Added tag filtering functionality.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    /**
     * UI state representing the home screen data.
     */
    data class HomeUiState(
        val notes: List<Note> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val selectedTag: Tag? = null,
        val isLoading: Boolean = false
    )

    /**
     * Stage 4: Selected tag for filtering (null = show all notes).
     */
    private val _selectedTagId = MutableStateFlow<Long?>(null)

    /**
     * Reactive state flow combining notes, tags, and filter state.
     * Automatically updates UI when database changes or filter changes.
     *
     * Stage 4 Bug Fix: Added content-based fallback matching to ensure tags
     * are always detected even if database cross-ref is missing.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        noteRepository.getAllNotes(),
        tagRepository.getAllTags(),
        _selectedTagId
    ) { allNotes, allTags, selectedTagId ->
        val filteredNotes = if (selectedTagId == null) {
            allNotes
        } else {
            // Stage 4: Filter notes by selected tag with DUAL INSURANCE
            val selectedTag = allTags.find { it.id == selectedTagId }
            allNotes.filter { note ->
                // Primary: Check if note.tags contains the selected tag ID
                val hasTagById = note.tags.any { it.id == selectedTagId }

                // Fallback: Check content for #tagName (case-insensitive)
                val hasTagByContent = selectedTag?.let { tag ->
                    note.content.contains("#${tag.name}", ignoreCase = true)
                } ?: false

                hasTagById || hasTagByContent
            }
        }

        HomeUiState(
            notes = filteredNotes,
            allTags = allTags,
            selectedTag = allTags.find { it.id == selectedTagId },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * Stage 4: Select a tag for filtering.
     *
     * @param tagId Tag ID to filter by, or null to show all notes
     */
    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }

    /**
     * Stage 4: Clear tag filter (show all notes).
     */
    fun clearTagFilter() {
        _selectedTagId.value = null
    }

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
