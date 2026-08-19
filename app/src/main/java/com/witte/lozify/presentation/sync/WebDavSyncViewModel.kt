package com.witte.lozify.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.network.webdav.SyncProgress
import com.witte.lozify.core.network.webdav.SyncResult
import com.witte.lozify.core.network.webdav.SyncStage
import com.witte.lozify.core.network.webdav.WebDavSyncManager
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.domain.repository.NoteRepository
import com.witte.lozify.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WebDavPreset(val displayName: String, val defaultUrl: String) {
    JIANGUOYUN("坚果云 Nutstore (推荐)", "https://dav.jianguoyun.com/dav/"),
    CUSTOM("自定义 WebDAV 服务器", "")
}

data class WebDavSyncUiState(
    val serverUrl: String = UserPreferencesManager.DEFAULT_WEBDAV_SERVER_URL,
    val username: String = "",
    val password: String = "",
    val remoteDir: String = UserPreferencesManager.DEFAULT_WEBDAV_REMOTE_DIR,
    val autoSync: Boolean = false,
    val lastSyncTime: Long = 0L,
    val isPasswordVisible: Boolean = false,
    val selectedPreset: WebDavPreset = WebDavPreset.JIANGUOYUN,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val isTestSuccess: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgress: SyncProgress? = null,
    val syncResult: SyncResult? = null,
    val localNotesCount: Int = 0,
    val localTagsCount: Int = 0
)

@HiltViewModel
class WebDavSyncViewModel @Inject constructor(
    private val syncManager: WebDavSyncManager,
    private val preferencesManager: UserPreferencesManager,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebDavSyncUiState())
    val uiState: StateFlow<WebDavSyncUiState> = _uiState.asStateFlow()

    init {
        // Load stored preferences
        val storedUrl = preferencesManager.webdavServerUrl.value
        val storedUser = preferencesManager.webdavUsername.value
        val storedPass = preferencesManager.webdavPassword.value
        val storedDir = preferencesManager.webdavRemoteDir.value
        val storedAuto = preferencesManager.webdavAutoSync.value
        val storedLast = preferencesManager.webdavLastSyncTime.value

        val preset = if (storedUrl.contains("jianguoyun.com")) {
            WebDavPreset.JIANGUOYUN
        } else {
            WebDavPreset.CUSTOM
        }

        _uiState.update {
            it.copy(
                serverUrl = storedUrl,
                username = storedUser,
                password = storedPass,
                remoteDir = storedDir,
                autoSync = storedAuto,
                lastSyncTime = storedLast,
                selectedPreset = preset
            )
        }

        // Observe local notes and tags count
        viewModelScope.launch {
            combine(
                noteRepository.getAllNotes(),
                tagRepository.getAllTags()
            ) { notes, tags ->
                val activeNotes = notes.filter { !it.isDeleted }
                Pair(activeNotes.size, tags.size)
            }.collect { (notesCount, tagsCount) ->
                _uiState.update {
                    it.copy(
                        localNotesCount = notesCount,
                        localTagsCount = tagsCount
                    )
                }
            }
        }
    }

    fun onPresetSelected(preset: WebDavPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                serverUrl = if (preset == WebDavPreset.JIANGUOYUN) preset.defaultUrl else it.serverUrl
            )
        }
    }

    fun onServerUrlChanged(url: String) {
        _uiState.update { it.copy(serverUrl = url, testConnectionMessage = null) }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, testConnectionMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, testConnectionMessage = null) }
    }

    fun onRemoteDirChanged(dir: String) {
        _uiState.update { it.copy(remoteDir = dir) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onAutoSyncToggled(enabled: Boolean) {
        preferencesManager.setWebDavAutoSync(enabled)
        _uiState.update { it.copy(autoSync = enabled) }
    }

    fun saveConfig() {
        val s = _uiState.value
        preferencesManager.saveWebDavConfig(
            serverUrl = s.serverUrl,
            username = s.username,
            password = s.password,
            remoteDir = s.remoteDir
        )
    }

    fun testConnection() {
        val s = _uiState.value
        saveConfig()

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionMessage = null) }
            val result = syncManager.testConnection()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "连接测试成功！WebDAV 授权认证正常。",
                        isTestSuccess = true
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "连接失败"
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = errorMsg,
                        isTestSuccess = false
                    )
                }
            }
        }
    }

    fun startSync() {
        saveConfig()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncResult = null,
                    syncProgress = SyncProgress(SyncStage.CONNECTING, 0.05f, "准备开始同步...")
                )
            }

            val result = syncManager.performSync { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncResult = result,
                    lastSyncTime = if (result.isSuccess) System.currentTimeMillis() else it.lastSyncTime
                )
            }
        }
    }

    fun clearConfiguration() {
        preferencesManager.clearWebDavConfig()
        _uiState.update {
            it.copy(
                serverUrl = UserPreferencesManager.DEFAULT_WEBDAV_SERVER_URL,
                username = "",
                password = "",
                remoteDir = UserPreferencesManager.DEFAULT_WEBDAV_REMOTE_DIR,
                autoSync = false,
                lastSyncTime = 0L,
                testConnectionMessage = null,
                syncResult = null
            )
        }
    }
}
