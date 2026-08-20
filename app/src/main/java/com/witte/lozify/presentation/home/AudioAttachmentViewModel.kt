package com.witte.lozify.presentation.home

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.witte.lozify.core.common.FileUtils
import com.witte.lozify.core.media.AudioPlayerManager
import com.witte.lozify.core.network.webdav.AttachmentDownloadManager
import com.witte.lozify.domain.model.Attachment
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Audio Attachment playback and on-demand downloading.
 *
 * Stage 48: Connects AudioPlayerManager and AttachmentDownloadManager to UI cards.
 */
@HiltViewModel
class AudioAttachmentViewModel @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager,
    private val downloadManager: AttachmentDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val playbackState = audioPlayerManager.playbackState
    val downloadingStates: StateFlow<Map<Long, Float>> = downloadManager.downloadingStates

    fun playOrPause(file: File) {
        audioPlayerManager.playOrPause(file)
    }

    fun stop() {
        audioPlayerManager.stop()
    }

    fun seekTo(positionMs: Long) {
        audioPlayerManager.seekTo(positionMs)
    }

    fun downloadAndPlay(attachment: Attachment) {
        viewModelScope.launch {
            Toast.makeText(context, "正在从云端下载音频...", Toast.LENGTH_SHORT).show()
            val result = downloadManager.downloadAttachment(attachment)
            if (result.isSuccess) {
                val downloadedFile = result.getOrThrow()
                audioPlayerManager.playOrPause(downloadedFile)
            } else {
                val error = result.exceptionOrNull()?.message ?: "下载失败"
                Toast.makeText(context, "下载音频失败: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun downloadAndOpenFile(attachment: Attachment) {
        viewModelScope.launch {
            Toast.makeText(context, "正在从云端下载附件...", Toast.LENGTH_SHORT).show()
            val result = downloadManager.downloadAttachment(attachment)
            if (result.isSuccess) {
                val downloadedFile = result.getOrThrow()
                FileUtils.openFileWithSystemApp(context, downloadedFile, attachment.mimeType)
            } else {
                val error = result.exceptionOrNull()?.message ?: "下载失败"
                Toast.makeText(context, "下载附件失败: $error", Toast.LENGTH_LONG).show()
            }
        }
    }
}
