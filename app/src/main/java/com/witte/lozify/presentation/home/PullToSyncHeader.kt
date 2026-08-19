package com.witte.lozify.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * State of the Pull-to-Sync header.
 */
enum class PullSyncState {
    IDLE,       // 0 height, invisible
    PULLING,    // User pulling, shows notes count
    SYNCING,    // Released, syncing in progress
    COMPLETED,  // Sync finished, shows success message
    ERROR       // Sync error
}

/**
 * PullToSyncHeader - Elastic header displaying note statistics when pulling down,
 * transitioning in-place to sync status upon release, and smoothly collapsing upon completion.
 *
 * Stage 29: Pull-to-Sync & Dynamic Stats Header.
 */
@Composable
fun PullToSyncHeader(
    syncState: PullSyncState,
    notesCount: Int,
    statusText: String?,
    modifier: Modifier = Modifier
) {
    val targetHeight: Dp = when (syncState) {
        PullSyncState.IDLE -> 0.dp
        PullSyncState.PULLING, PullSyncState.SYNCING, PullSyncState.COMPLETED, PullSyncState.ERROR -> 44.dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pull_sync_header_height"
    )

    if (animatedHeight > 0.5.dp) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .background(Color(0xFFF7F8FA)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = syncState,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "pull_sync_content_switch"
            ) { state ->
                when (state) {
                    PullSyncState.PULLING -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "共 $notesCount 条笔记",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    PullSyncState.SYNCING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    PullSyncState.COMPLETED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    PullSyncState.ERROR -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
