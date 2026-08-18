package com.witte.lozify.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.model.UserStats
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for HomeScreen.
 *
 * Manages note list state and operations like pin/delete.
 * Observes Room database via Flow for reactive updates.
 *
 * Stage 4: Added tag filtering functionality.
 * Stage 14: Added user achievement and statistics tracking.
 * Stage 17: Connected UserPreferencesManager for dynamic card collapse lines.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    /**
     * UI state representing the home screen data.
     *
     * Stage 5: Added searchQuery for real-time search.
     * Stage 14: Added userStats and heatmapData for drawer dashboard.
     * Stage 17: Added maxCollapseLines.
     */
    data class HomeUiState(
        val notes: List<Note> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val pinnedTags: List<Tag> = emptyList(),
        val selectedTag: Tag? = null,
        val isLoading: Boolean = false,
        val searchQuery: String = "",
        val userStats: UserStats = UserStats(),
        val heatmapData: Map<LocalDate, Int> = emptyMap(),
        val maxCollapseLines: Int = 5
    )

    /**
     * Stage 4: Selected tag for filtering (null = show all notes).
     */
    private val _selectedTagId = MutableStateFlow<Long?>(null)

    /**
     * Stage 5: Search query for real-time content filtering.
     */
    private val _searchQuery = MutableStateFlow("")

    /**
     * Reactive state flow combining notes, tags, filter state, and search query.
     * Automatically updates UI when database changes, filter changes, or search query changes.
     *
     * Stage 4 Bug Fix: Added content-based fallback matching to ensure tags
     * are always detected even if database cross-ref is missing.
     * Stage 5: Added real-time search filtering.
     * Stage 14: Computes userStats and heatmapData (daily note counts).
     * Stage 16: Computes pinnedTags for drawer quick access.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        noteRepository.getAllNotes(),
        tagRepository.getAllTags(),
        _selectedTagId,
        _searchQuery,
        preferencesManager.maxCollapseLines
    ) { allNotes, allTags, selectedTagId, searchQuery, maxCollapseLines ->
        // Stage 4: Filter by tag
        var filteredNotes = if (selectedTagId == null) {
            allNotes
        } else {
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

        // Stage 5: Filter by search query (case-insensitive)
        if (searchQuery.isNotBlank()) {
            filteredNotes = filteredNotes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }

        // Stage 14: Calculate user stats
        val totalNotesCount = allNotes.size
        val totalTagsCount = allTags.size
        val daysCount = if (allNotes.isNotEmpty()) {
            val oldestNoteTime = allNotes.minOfOrNull { it.createdAt } ?: Instant.now()
            val oldestDate = oldestNoteTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            (ChronoUnit.DAYS.between(oldestDate, today) + 1).coerceAtLeast(1).toInt()
        } else {
            1
        }
        val userStats = UserStats(
            notesCount = totalNotesCount,
            tagsCount = totalTagsCount,
            daysCount = daysCount
        )

        // Stage 15: Calculate heatmap data (note count per day for last 8 weeks)
        val heatmapData = allNotes
            .map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .groupingBy { it }
            .eachCount()

        val pinnedTags = allTags.filter { it.isPinned }

        HomeUiState(
            notes = filteredNotes,
            allTags = allTags,
            pinnedTags = pinnedTags,
            selectedTag = allTags.find { it.id == selectedTagId },
            isLoading = false,
            searchQuery = searchQuery,
            userStats = userStats,
            heatmapData = heatmapData,
            maxCollapseLines = maxCollapseLines
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * Stage 14: Dedicated StateFlow for user achievement statistics.
     */
    val userStats: StateFlow<UserStats> = combine(
        noteRepository.getAllNotes(),
        tagRepository.getAllTags()
    ) { allNotes, allTags ->
        val zoneId = ZoneId.systemDefault()
        val earliestNote = allNotes.minOfOrNull { it.createdAt }
        val daysCount = if (earliestNote != null) {
            val startDate = earliestNote.atZone(zoneId).toLocalDate()
            val today = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, today) + 1
            maxOf(1, daysBetween.toInt())
        } else {
            1
        }
        UserStats(
            notesCount = allNotes.size,
            tagsCount = allTags.size,
            daysCount = daysCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserStats()
    )

    /**
     * Stage 14: Dedicated StateFlow for contribution heatmap data (LocalDate -> note count).
     */
    val heatmapData: StateFlow<Map<LocalDate, Int>> = noteRepository.getAllNotes()
        .combine(MutableStateFlow(Unit)) { allNotes, _ ->
            val zoneId = ZoneId.systemDefault()
            allNotes
                .groupBy { it.createdAt.atZone(zoneId).toLocalDate() }
                .mapValues { it.value.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /**
     * Expose allTags for navigation and tag edit screen.
     */
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Stage 4: Select a tag for filtering, or toggle off if already selected.
     *
     * @param tagId Tag ID to filter by, or null to show all notes
     */
    fun selectTag(tagId: Long?) {
        if (tagId != null && _selectedTagId.value == tagId) {
            _selectedTagId.value = null // Toggle back to all notes
        } else {
            _selectedTagId.value = tagId
        }
    }

    /**
     * Stage 4: Clear tag filter (show all notes).
     */
    fun clearTagFilter() {
        _selectedTagId.value = null
    }

    /**
     * Stage 5: Update search query for real-time filtering.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Stage 5: Clear search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
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
     * Stage 9: Toggle pin status (simplified for swipe gesture).
     */
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            val noteFlow = noteRepository.getNoteById(noteId)
            noteFlow.firstOrNull()?.let { currentNote ->
                noteRepository.togglePinStatus(noteId, !currentNote.isPinned)
            }
        }
    }

    /**
     * Stage 9: Move note to trash (renamed from archive in Stage 12).
     */
    fun moveToTrash(noteId: Long) {
        viewModelScope.launch {
            noteRepository.toggleTrashStatus(noteId, true)
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

    /**
     * Stage 12 & 13: Rename tag across all notes using regex replacement and update icon.
     *
     * @param tagId Tag ID to rename
     * @param oldName Current tag name without # prefix
     * @param newName New tag name without # prefix
     * @param newIcon Optional new emoji icon (or null for default #)
     */
    fun renameTag(tagId: Long, oldName: String, newName: String, newIcon: String? = null) {
        viewModelScope.launch {
            tagRepository.renameTagInAllNotes(tagId, oldName, newName, newIcon)
        }
    }

    /**
     * Stage 12: Remove tag from all notes (regex replace #tagName with empty).
     *
     * @param tagName Tag name without # prefix
     */
    fun removeTagFromAllNotes(tagName: String) {
        viewModelScope.launch {
            tagRepository.removeTagFromAllNotes(tagName)
        }
    }

    /**
     * Stage 12: Delete tag and move all associated notes to trash.
     *
     * @param tagId Tag ID to delete
     */
    fun deleteTagAndMoveNotesToTrash(tagId: Long) {
        viewModelScope.launch {
            tagRepository.deleteTagAndMoveNotesToTrash(tagId)
        }
    }

    /**
     * Stage 16: Toggle pin status of a tag.
     *
     * @param tagId Tag ID to toggle
     * @param isPinned Target pin status
     */
    fun togglePinTag(tagId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            tagRepository.togglePinTag(tagId, isPinned)
        }
    }

    /**
     * Stage 7 Bug Fix: Update note content (for checkbox toggles).
     *
     * @param noteId ID of the note to update
     * @param newContent New content text
     */
    fun updateNoteContent(noteId: Long, newContent: String) {
        viewModelScope.launch {
            val noteFlow = noteRepository.getNoteById(noteId)
            noteFlow.firstOrNull()?.let { currentNote ->
                val updatedNote = currentNote.copy(
                    content = newContent,
                    updatedAt = java.time.Instant.now()
                )
                noteRepository.updateNote(updatedNote)
            }
        }
    }
}
