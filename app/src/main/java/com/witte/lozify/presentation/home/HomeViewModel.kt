package com.witte.lozify.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.network.webdav.WebDavSyncManager
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.witte.lozify.core.update.AppUpdateInfo
import com.witte.lozify.core.update.UpdateManager
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
 * Stage 29: Added Pull-to-Sync gesture and Fast-Path sync trigger.
 * Stage 31: Added On-Launch silent version update detection & push dialog.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val preferencesManager: UserPreferencesManager,
    private val syncManager: WebDavSyncManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    /**
     * Stage 21: Sorting order for notes stream.
     */
    enum class NoteSortOrder(val displayName: String) {
        CREATED_DESC("创建时间，从新到旧"),
        CREATED_ASC("创建时间，从旧到新"),
        UPDATED_DESC("更新时间，从新到旧")
    }

    /**
     * UI state representing the home screen data.
     *
     * Stage 5: Added searchQuery for real-time search.
     * Stage 14: Added userStats and heatmapData for drawer dashboard.
     * Stage 17: Added maxCollapseLines.
     * Stage 21: Added sortOrder.
     * Stage 29: Added pullSyncState and pullSyncStatusText.
     */
    data class HomeUiState(
        val notes: List<Note> = emptyList(),
        val allActiveNotes: List<Note> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val pinnedTags: List<Tag> = emptyList(),
        val selectedTag: Tag? = null,
        val isLoading: Boolean = false,
        val searchQuery: String = "",
        val userStats: UserStats = UserStats(),
        val heatmapData: Map<LocalDate, Int> = emptyMap(),
        val calendarTimeZone: String = "",
        val userName: String = "木下",
        val isProUser: Boolean = false,
        val maxCollapseLines: Int = 5,
        val sortOrder: NoteSortOrder = NoteSortOrder.CREATED_DESC,
        val pullSyncState: PullSyncState = PullSyncState.IDLE,
        val pullSyncStatusText: String? = null,
        val totalActiveNotesCount: Int = 0
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
     * Stage 21: Active sort order.
     */
    private val _sortOrder = MutableStateFlow(NoteSortOrder.CREATED_DESC)

    /**
     * Stage 29: Pull to sync states.
     */
    private val _pullSyncState = MutableStateFlow(PullSyncState.IDLE)
    private val _pullSyncStatusText = MutableStateFlow<String?>(null)

    /**
     * Stage 31: On-Launch in-app update prompt.
     */
    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo

    // Stage 59: App Lock state
    val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabled
    val appLockPin: StateFlow<String> = preferencesManager.appLockPin
    val isBiometricEnabled: StateFlow<Boolean> = preferencesManager.isBiometricEnabled

    private data class BaseHomeData(
        val allNotes: List<Note>,
        val allTags: List<Tag>,
        val selectedTagId: Long?,
        val searchQuery: String
    )

    private data class PullSyncData(
        val state: PullSyncState,
        val statusText: String?
    )

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun getResolvedZoneId(tzString: String): ZoneId {
        return if (tzString.isBlank()) {
            ZoneId.systemDefault()
        } else {
            try {
                ZoneId.of(tzString)
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }
        }
    }

    /**
     * Reactive state flow combining notes, tags, filter state, search query, sort order, and pull-sync status.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            noteRepository.getAllNotes(),
            tagRepository.getAllTags(),
            _selectedTagId,
            _searchQuery
        ) { allNotes, allTags, selectedTagId, searchQuery ->
            BaseHomeData(allNotes, allTags, selectedTagId, searchQuery)
        },
        combine(
            preferencesManager.maxCollapseLines,
            preferencesManager.calendarTimeZone,
            preferencesManager.userName,
            preferencesManager.isProUser
        ) { maxLines, timeZone, userName, isPro ->
            Tuple4(maxLines, timeZone, userName, isPro)
        },
        _sortOrder,
        combine(_pullSyncState, _pullSyncStatusText) { state, text ->
            PullSyncData(state, text)
        }
    ) { base, userPrefs, sortOrder, pullSync ->
        val allNotes = base.allNotes
        val allTags = base.allTags.filter { it.usageCount > 0 || it.isPinned }
        val selectedTagId = base.selectedTagId
        val searchQuery = base.searchQuery
        val maxCollapseLines = userPrefs.a
        val calendarTimeZone = userPrefs.b
        val userName = userPrefs.c
        val isProUser = userPrefs.d

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

        // Stage 21: Apply sorting (preserve pinned notes at top)
        filteredNotes = when (sortOrder) {
            NoteSortOrder.CREATED_DESC -> filteredNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt }
            )
            NoteSortOrder.CREATED_ASC -> filteredNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenBy { it.createdAt }
            )
            NoteSortOrder.UPDATED_DESC -> filteredNotes.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
            )
        }

        val zoneId = getResolvedZoneId(calendarTimeZone)

        // Stage 14: Calculate user stats
        val totalNotesCount = allNotes.size
        val totalTagsCount = allTags.size
        val daysCount = if (allNotes.isNotEmpty()) {
            val oldestNoteTime = allNotes.minOfOrNull { it.createdAt } ?: Instant.now()
            val oldestDate = oldestNoteTime.atZone(zoneId).toLocalDate()
            val today = LocalDate.now(zoneId)
            (ChronoUnit.DAYS.between(oldestDate, today) + 1).coerceAtLeast(1).toInt()
        } else {
            1
        }
        val userStats = UserStats(
            notesCount = totalNotesCount,
            tagsCount = totalTagsCount,
            daysCount = daysCount
        )

        // Stage 15 & 57: Calculate heatmap data (note count per day for last 8 weeks)
        val heatmapData = allNotes
            .map { it.createdAt.atZone(zoneId).toLocalDate() }
            .groupingBy { it }
            .eachCount()

        val pinnedTags = allTags.filter { it.isPinned }

        HomeUiState(
            notes = filteredNotes,
            allActiveNotes = allNotes,
            allTags = allTags,
            pinnedTags = pinnedTags,
            selectedTag = allTags.find { it.id == selectedTagId },
            isLoading = false,
            searchQuery = searchQuery,
            userStats = userStats,
            heatmapData = heatmapData,
            calendarTimeZone = calendarTimeZone,
            userName = userName,
            isProUser = isProUser,
            maxCollapseLines = maxCollapseLines,
            sortOrder = sortOrder,
            pullSyncState = pullSync.state,
            pullSyncStatusText = pullSync.statusText,
            totalActiveNotesCount = totalNotesCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onPullDragging() {
        if (_pullSyncState.value == PullSyncState.IDLE) {
            _pullSyncState.value = PullSyncState.PULLING
        }
    }

    fun onPullCanceled() {
        if (_pullSyncState.value == PullSyncState.PULLING) {
            _pullSyncState.value = PullSyncState.IDLE
        }
    }

    fun triggerPullToSync() {
        if (_pullSyncState.value == PullSyncState.SYNCING) return

        viewModelScope.launch {
            _pullSyncState.value = PullSyncState.SYNCING
            _pullSyncStatusText.value = "正在同步中 ···"

            val hasConfig = preferencesManager.webdavServerUrl.value.isNotBlank() &&
                    preferencesManager.webdavUsername.value.isNotBlank() &&
                    preferencesManager.webdavPassword.value.isNotBlank()

            if (!hasConfig) {
                _pullSyncState.value = PullSyncState.ERROR
                _pullSyncStatusText.value = "未配置云同步 (前往设置)"
                kotlinx.coroutines.delay(1000)
                _pullSyncState.value = PullSyncState.IDLE
                _pullSyncStatusText.value = null
                return@launch
            }

            val result = syncManager.performSync(forceFullSync = true) { progress ->
                when (progress.stage) {
                    com.witte.lozify.core.network.webdav.SyncStage.CONNECTING -> {
                        _pullSyncStatusText.value = "正在连接云端 ···"
                    }
                    com.witte.lozify.core.network.webdav.SyncStage.FETCHING_REMOTE,
                    com.witte.lozify.core.network.webdav.SyncStage.MERGING_DATA -> {
                        _pullSyncStatusText.value = "正在比对增量 ···"
                    }
                    com.witte.lozify.core.network.webdav.SyncStage.SYNCING_IMAGES -> {
                        _pullSyncStatusText.value = "正在同步图片 ···"
                    }
                    else -> {}
                }
            }

            if (result.isSuccess) {
                _pullSyncState.value = PullSyncState.COMPLETED
                _pullSyncStatusText.value = if (result.isAlreadyUpToDate) "已是最新数据 ✓" else "同步完成 ✓"
            } else {
                _pullSyncState.value = PullSyncState.ERROR
                _pullSyncStatusText.value = result.errorMessage ?: "同步未完成"
            }

            // Smoothly collapse after 450ms
            kotlinx.coroutines.delay(450)
            _pullSyncState.value = PullSyncState.IDLE
            _pullSyncStatusText.value = null
        }
    }

    /**
     * Stage 31: Silent on-launch update check (triggers update dialog if new version exists).
     */
    fun checkForUpdateOnLaunch() {
        viewModelScope.launch {
            try {
                val result = updateManager.checkForUpdate()
                val info = result.getOrNull()
                if (info != null) {
                    _appUpdateInfo.value = info
                }
            } catch (e: Exception) {
                // Silent fail on launch if network is offline
            }
        }
    }

    fun dismissUpdateDialog() {
        _appUpdateInfo.value = null
    }

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
            tagsCount = allTags.count { it.usageCount > 0 || it.isPinned },
            daysCount = daysCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserStats()
    )

    /**
     * Stage 14 & 57: Dedicated StateFlow for contribution heatmap data (LocalDate -> note count).
     */
    val heatmapData: StateFlow<Map<LocalDate, Int>> = combine(
        noteRepository.getAllNotes(),
        preferencesManager.calendarTimeZone
    ) { allNotes, tzString ->
        val zoneId = getResolvedZoneId(tzString)
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
     * Stage 21: Change notes stream sort order.
     */
    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }

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
