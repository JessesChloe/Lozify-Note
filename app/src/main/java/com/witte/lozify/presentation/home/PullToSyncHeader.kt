package com.witte.lozify.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * State of the Pull-to-Sync header.
 */
enum class PullSyncState {
    IDLE,       // Invisible
    PULLING,    // User pulling, shows note count or release-to-sync prompt
    SYNCING,    // Released past threshold, syncing in progress
    COMPLETED,  // Sync finished, shows success message
    ERROR       // Sync error
}

/**
 * PullToSyncHeader - Frameless, minimalist Flomo-style header displaying note statistics
 * when pulling down, transitioning smoothly to release-to-sync prompt and in-place sync progress.
 *
 * Stage 40: Clean & Minimalist Typography (Frameless, Borderless).
 */
@Composable
fun PullToSyncHeader(
    syncState: PullSyncState,
    notesCount: Int,
    statusText: String?,
    pullOffsetPx: Float,
    triggerThresholdPx: Float,
    modifier: Modifier = Modifier
) {
    val alpha = when (syncState) {
        PullSyncState.IDLE -> 0f
        PullSyncState.PULLING -> (pullOffsetPx / (triggerThresholdPx * 0.35f)).coerceIn(0f, 1f)
        PullSyncState.SYNCING, PullSyncState.COMPLETED, PullSyncState.ERROR -> 1f
    }

    if (alpha <= 0.01f) return

    val isReadyToTrigger = pullOffsetPx >= triggerThresholdPx && syncState == PullSyncState.PULLING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = syncState to isReadyToTrigger,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "pull_sync_content_switch"
            ) { (state, isReady) ->
                when (state) {
                    PullSyncState.PULLING -> {
                        if (isReady) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "松开立即同步坚果云",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00C853),
                                    letterSpacing = 0.3.sp
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "共 $notesCount 条笔记",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF777777),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    PullSyncState.SYNCING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF00C853)
                            )
                            Text(
                                text = statusText ?: "正在同步中 ···",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00C853),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    PullSyncState.COMPLETED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusText ?: "已是最新数据",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    PullSyncState.ERROR -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusText ?: "同步未完成",
                                fontSize = 13.sp,
                                color = Color(0xFF888888)
                            )
                        }
                    }

                    PullSyncState.IDLE -> {
                        Box(modifier = Modifier.size(0.dp))
                    }
                }
            }
        }
    }
}
