package com.witte.lozify.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
 * AudioAttachmentCard - In-app local audio player component for note cards.
 *
 * Stage 48:
 * - Dedicated music player interface with track title, file size, and timestamp.
 * - Play / Pause and Stop controls.
 * - Real-time progress bar with interactive seek scrubbing.
 * - On-demand WebDAV downloading support.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7FC)),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPlaying) Color(0xFFD1C4E9) else Color(0xFFECE6F2)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Icon + Title/Subtitle + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Music Note Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEDE7F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF8E24AA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title & Duration / Status Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitleText = when {
                        isDownloading -> "正在从云端下载 ${(downloadProgress * 100).toInt()}%..."
                        !fileExists -> "☁️ 云端音频 · 点击下载播放 ($fileSize)"
                        isCurrentTrack && durationMs > 0 -> "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)} · $fileSize"
                        else -> fileSize
                    }

                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = if (isDownloading || !fileExists) Color(0xFF8E24AA) else Color(0xFF888888),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Control Buttons
                if (!fileExists) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF8E24AA)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEDE7F6))
                                .clickable { viewModel.downloadAndPlay(attachment) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = "下载播放",
                                    tint = Color(0xFF8E24AA),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "下载",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF8E24AA)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Stop button (only visible when playing or paused)
                        if (isPlaying || isPaused) {
                            IconButton(
                                onClick = { viewModel.stop() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "停止播放",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Play / Pause Circle Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8E24AA))
                                .clickable { viewModel.playOrPause(localFile) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Progress Bar & Scrubber (visible when active or playing)
            if (fileExists && (isCurrentTrack || isPlaying || isPaused) && durationMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { newPos ->
                        viewModel.seekTo(newPos.toLong())
                    },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF8E24AA),
                        activeTrackColor = Color(0xFF8E24AA),
                        inactiveTrackColor = Color(0xFFE1D5EE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosMs),
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                    Text(
                        text = formatDuration(durationMs),
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                }
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
