package com.witte.lozify.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.core.update.AppUpdateInfo
import com.witte.lozify.core.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val maxCollapseLines: Int = 5,
    val isDraftPersistenceEnabled: Boolean = true,
    val isImageCompressionEnabled: Boolean = true,
    val calendarTimeZone: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val updateMessage: String? = null
)

/**
 * ViewModel for SettingsScreen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _updateState = MutableStateFlow(
        Triple<Boolean, AppUpdateInfo?, String?>(false, null, null)
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferencesManager.maxCollapseLines,
            preferencesManager.isDraftPersistenceEnabled,
            preferencesManager.imageCompressionEnabled,
            preferencesManager.calendarTimeZone
        ) { maxLines, isDraftEnabled, isImageCompressionEnabled, timeZone ->
            Tuple4(maxLines, isDraftEnabled, isImageCompressionEnabled, timeZone)
        },
        _updateState
    ) { tuple, updateState ->
        SettingsUiState(
            maxCollapseLines = tuple.a,
            isDraftPersistenceEnabled = tuple.b,
            isImageCompressionEnabled = tuple.c,
            calendarTimeZone = tuple.d,
            isCheckingUpdate = updateState.first,
            updateInfo = updateState.second,
            updateMessage = updateState.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun setCalendarTimeZone(timeZoneId: String) {
        preferencesManager.setCalendarTimeZone(timeZoneId)
    }

    fun setMaxCollapseLines(lines: Int) {
        preferencesManager.setMaxCollapseLines(lines)
    }

    fun setDraftPersistenceEnabled(enabled: Boolean) {
        preferencesManager.setDraftPersistenceEnabled(enabled)
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        preferencesManager.setImageCompressionEnabled(enabled)
    }

    fun checkForUpdate(isManual: Boolean = true) {
        viewModelScope.launch {
            _updateState.update { Triple(true, null, null) }
            val result = updateManager.checkForUpdate()

            if (result.isSuccess) {
                val info = result.getOrNull()
                if (info != null) {
                    _updateState.update { Triple(false, info, null) }
                } else {
                    _updateState.update {
                        Triple(false, null, if (isManual) "已是最新版本 (v${com.witte.lozify.BuildConfig.VERSION_NAME})" else null)
                    }
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "检查更新失败"
                _updateState.update {
                    Triple(false, null, if (isManual) "检查更新失败: $errorMsg" else null)
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateState.update { it.copy(second = null) }
    }

    fun clearUpdateMessage() {
        _updateState.update { it.copy(third = null) }
    }

    fun openDownloadUrl(context: Context, url: String) {
        updateManager.openDownloadUrl(context, url)
    }
}
