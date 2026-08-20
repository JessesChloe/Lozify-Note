package com.witte.lozify.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.core.media.AudioPlaybackStatus
import com.witte.lozify.domain.model.Attachment
import java.io.File

/**
 * AudioAttachmentCard - Compact, lightweight in-app audio player component for note cards.
 *
 * Stage 51:
 * - 1:1 aligned with standard compact file attachment style (height ~40dp, 26dp icon).
 * - Inline play/pause/stop buttons.
 * - Slim bottom line progress bar during playback.
 * - On-demand WebDAV download support.
 */
@Composable
fun AudioAttachmentCard(
    attachment: Attachment,
    filesDir: File,
    modifier: Modifier = Modifier,
    viewModel: AudioAttachmentViewModel = hiltViewModel()
) {
    val localFile = remember(attachment.filePath, filesDir) {
        File(filesDir, attachment.filePath)
    }
    val fileExists = localFile.exists()

    val playbackState by viewModel.playbackState.collectAsState()
    val downloadingStates by viewModel.downloadingStates.collectAsState()

    val isDownloading = downloadingStates.containsKey(attachment.id)
    val downloadProgress = downloadingStates[attachment.id] ?: 0f

    val isCurrentTrack = playbackState.currentFilePath == localFile.absolutePath
    val isPlaying = isCurrentTrack && playbackState.status == AudioPlaybackStatus.PLAYING
    val isPaused = isCurrentTrack && playbackState.status == AudioPlaybackStatus.PAUSED
    val currentPosMs = if (isCurrentTrack) playbackState.currentPositionMs else 0L
    val durationMs = if (isCurrentTrack && playbackState.durationMs > 0) playbackState.durationMs else 0L

    val displayName = attachment.getDisplayName()
    val fileSize = attachment.getFormattedFileSize()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
        border = BorderStroke(0.6.dp, if (isPlaying) Color(0xFFCE93D8) else Color(0xFFEBEBEB)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (fileExists) {
                    viewModel.playOrPause(localFile)
                } else {
                    viewModel.downloadAndPlay(attachment)
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio Icon Container (26dp, matching all other attachments)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3E5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AudioFile,
                        contentDescription = null,
                        tint = Color(0xFF8E24AA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // File Info (Name + Progress / Size)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitleText = when {
                        isDownloading -> "正在从云端下载 ${(downloadProgress * 100).toInt()}%..."
                        !fileExists -> if (fileSize.isNotBlank()) "☁️ 云端音频 · 点击下载 ($fileSize)" else "☁️ 云端音频 · 点击下载"
                        isCurrentTrack && durationMs > 0 -> "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)} · $fileSize"
                        else -> fileSize
                    }

                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            color = if (isDownloading || !fileExists) Color(0xFF8E24AA) else Color(0xFF8E8E93),
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Action Controls
                if (!fileExists) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF8E24AA)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = "下载播放",
                            tint = Color(0xFF8E24AA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Stop button (only visible when playing or paused)
                        if (isPlaying || isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.stop() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "停止播放",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Play / Pause Icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.playOrPause(localFile) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = Color(0xFF8E24AA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Slim Bottom Progress Line (only visible when active track and duration > 0)
            if (fileExists && (isCurrentTrack || isPlaying || isPaused) && durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = Color(0xFF8E24AA),
                    trackColor = Color(0xFFEDE7F6)
                )
            }
        }
    }
}

/**
 * Format milliseconds into MM:SS format string.
 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
