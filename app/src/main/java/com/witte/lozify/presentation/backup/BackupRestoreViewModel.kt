package com.witte.lozify.presentation.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.backup.BackupManager
import com.witte.lozify.core.backup.ImportResult
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

/**
 * UI State for BackupRestoreScreen.
 */
data class BackupUiState(
    val totalNotes: Int = 0,
    val totalTags: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

/**
 * ViewModel for managing data backup, Markdown export, and JSON restoration.
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
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
                        totalNotes = notesCount,
                        totalTags = tagsCount
                    )
                }
            }
        }
    }

    /**
     * Export all notes and tags to JSON backup file via SAF Uri.
     */
    fun exportJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupManager.exportBackupJson(outputStream)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "全量 JSON 备份已成功导出！",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "导出失败: ${e.localizedMessage ?: "写入文件出错"}",
                        isError = true
                    )
                }
            }
        }
    }

    /**
     * Export notes into Markdown ZIP archive via SAF Uri.
     */
    fun exportMarkdownZip(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupManager.exportMarkdownArchive(outputStream)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Markdown 笔记压缩包已成功导出！",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "导出失败: ${e.localizedMessage ?: "压缩打包出错"}",
                        isError = true
                    )
                }
            }
        }
    }

    /**
     * Import and restore data from a JSON backup file.
     */
    fun importBackup(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                val result = contentResolver.openInputStream(uri)?.use { inputStream ->
                    backupManager.importBackupJson(inputStream)
                } ?: ImportResult(isSuccess = false, errorMessage = "无法读取所选文件")

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "恢复成功！已导入 ${result.notesImported} 条笔记，${result.tagsImported} 个标签。",
                            isError = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.errorMessage ?: "导入解析失败",
                            isError = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "导入出错: ${e.localizedMessage ?: "未知错误"}",
                        isError = true
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
