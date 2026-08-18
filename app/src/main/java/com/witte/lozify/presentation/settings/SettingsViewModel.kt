package com.witte.lozify.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val maxCollapseLines: Int = 5,
    val isDraftPersistenceEnabled: Boolean = true
)

/**
 * ViewModel for SettingsScreen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.maxCollapseLines,
        preferencesManager.isDraftPersistenceEnabled
    ) { maxLines, isDraftEnabled ->
        SettingsUiState(
            maxCollapseLines = maxLines,
            isDraftPersistenceEnabled = isDraftEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setMaxCollapseLines(lines: Int) {
        preferencesManager.setMaxCollapseLines(lines)
    }

    fun setDraftPersistenceEnabled(enabled: Boolean) {
        preferencesManager.setDraftPersistenceEnabled(enabled)
    }
}
