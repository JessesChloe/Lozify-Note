package com.witte.lozify.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.domain.model.NoteThread
import com.witte.lozify.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for NoteDetailBottomSheet (Thread View).
 */
data class NoteDetailUiState(
    val isLoading: Boolean = true,
    val thread: NoteThread? = null,
    val error: String? = null
)

/**
 * ViewModel managing the thread/context chain for NoteDetailBottomSheet.
 *
 * Stage 13: Loads parent notes (backlinks), focused main note, and child notes (outgoing mentions).
 */
@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadNoteThread(noteId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = NoteDetailUiState(isLoading = true)
            noteRepository.getNoteThread(noteId)
                .catch { e ->
                    _uiState.value = NoteDetailUiState(
                        isLoading = false,
                        error = e.message ?: "加载笔记链路失败"
                    )
                }
                .collect { thread ->
                    if (thread != null) {
                        _uiState.value = NoteDetailUiState(
                            isLoading = false,
                            thread = thread
                        )
                    } else {
                        _uiState.value = NoteDetailUiState(
                            isLoading = false,
                            error = "目标笔记不存在或已删除"
                        )
                    }
                }
        }
    }
}
