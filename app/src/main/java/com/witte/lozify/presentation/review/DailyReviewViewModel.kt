package com.witte.lozify.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.domain.model.Note
import com.witte.lozify.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ReviewCardItem(
    val note: Note,
    val milestoneBadge: String,
    val formattedMilestoneDate: String,
    val daysAgo: Long
)

data class DailyReviewUiState(
    val reviewCards: List<ReviewCardItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * DailyReviewViewModel - Generates time-machine milestone and serendipitous notes for daily review.
 *
 * Stage 59: Daily Review feature.
 */
@HiltViewModel
class DailyReviewViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyReviewUiState())
    val uiState: StateFlow<DailyReviewUiState> = _uiState.asStateFlow()

    init {
        loadDailyReviewNotes()
    }

    fun loadDailyReviewNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val tzString = preferencesManager.calendarTimeZone.value
            val zoneId = if (tzString.isBlank()) {
                ZoneId.systemDefault()
            } else {
                try {
                    ZoneId.of(tzString)
                } catch (e: Exception) {
                    ZoneId.systemDefault()
                }
            }

            val today = LocalDate.now(zoneId)
            val allNotes = noteRepository.getAllNotes().first()

            if (allNotes.isEmpty()) {
                _uiState.update { DailyReviewUiState(reviewCards = emptyList(), isLoading = false) }
                return@launch
            }

            val milestoneMatches = mutableListOf<ReviewCardItem>()
            val remainingNotes = mutableListOf<Note>()

            allNotes.forEach { note ->
                val noteDate = note.createdAt.atZone(zoneId).toLocalDate()
                val daysAgo = ChronoUnit.DAYS.between(noteDate, today)

                if (daysAgo in 360..370) {
                    milestoneMatches.add(ReviewCardItem(note, "📅 1 年前的今天", noteDate.toString(), daysAgo))
                } else if (daysAgo in 175..185) {
                    milestoneMatches.add(ReviewCardItem(note, "⏳ 半年前的今天", noteDate.toString(), daysAgo))
                } else if (daysAgo in 87..93) {
                    milestoneMatches.add(ReviewCardItem(note, "🍂 3 个月前的今天", noteDate.toString(), daysAgo))
                } else if (daysAgo in 28..32) {
                    milestoneMatches.add(ReviewCardItem(note, "🌱 1 个月前的今天", noteDate.toString(), daysAgo))
                } else if (daysAgo in 6..8) {
                    milestoneMatches.add(ReviewCardItem(note, "💡 1 周前的今天", noteDate.toString(), daysAgo))
                } else if (daysAgo > 0) {
                    remainingNotes.add(note)
                }
            }

            // Shuffle and pick to ensure at least 3-5 notes
            val selectedCards = mutableListOf<ReviewCardItem>()
            selectedCards.addAll(milestoneMatches.distinctBy { it.note.id })

            if (selectedCards.size < 4 && remainingNotes.isNotEmpty()) {
                val needCount = (4 - selectedCards.size).coerceAtMost(remainingNotes.size)
                val randomPicks = remainingNotes.shuffled().take(needCount)
                randomPicks.forEach { note ->
                    val noteDate = note.createdAt.atZone(zoneId).toLocalDate()
                    val daysAgo = ChronoUnit.DAYS.between(noteDate, today)
                    val badge = when {
                        daysAgo > 300 -> "✨ 很久以前的思考"
                        daysAgo > 60 -> "✨ 往日随想"
                        daysAgo > 14 -> "🌿 不久前的记录"
                        else -> "💫 近期灵感"
                    }
                    selectedCards.add(ReviewCardItem(note, badge, noteDate.toString(), daysAgo))
                }
            }

            // Fallback for brand new users with few notes: just use whatever exists
            if (selectedCards.isEmpty()) {
                allNotes.take(3).forEach { note ->
                    val noteDate = note.createdAt.atZone(zoneId).toLocalDate()
                    val daysAgo = ChronoUnit.DAYS.between(noteDate, today)
                    selectedCards.add(ReviewCardItem(note, "🌟 灵感回顾", noteDate.toString(), daysAgo))
                }
            }

            _uiState.update {
                DailyReviewUiState(
                    reviewCards = selectedCards,
                    isLoading = false
                )
            }
        }
    }
}
