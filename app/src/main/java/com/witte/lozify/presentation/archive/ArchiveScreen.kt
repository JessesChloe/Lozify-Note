package com.witte.lozify.presentation.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.domain.model.Note
import com.witte.lozify.presentation.home.NoteCard
import java.time.Duration
import java.time.Instant

/**
 * Archive Screen (Stage 10).
 *
 * Displays archived notes with swipe actions:
 * - Right swipe: Restore to home
 * - Left swipe: Delete permanently
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    archiveViewModel: ArchiveViewModel = hiltViewModel()
) {
    val archivedNotes by archiveViewModel.archivedNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("归档箱") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (archivedNotes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "归档箱空空如也",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Archived notes list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archivedNotes, key = { it.id }) { note ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            when (dismissValue) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    // Right swipe: Restore
                                    archiveViewModel.unarchiveNote(note.id)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    // Left swipe: Delete permanently
                                    archiveViewModel.deleteNotePermanently(note.id)
                                    false
                                }
                                else -> false
                            }
                        },
                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val color = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for restore
                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE57373) // Red for delete
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }
                            ) {
                                Icon(
                                    imageVector = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Refresh
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        else -> Icons.Default.Delete
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    ) {
                        NoteCard(
                            noteId = note.id,
                            content = note.content,
                            timestamp = formatTimestamp(note.createdAt),
                            isPinned = note.isPinned,
                            attachments = note.attachments,
                            outgoingRelationsCount = note.outgoingRelations.size,
                            incomingRelationsCount = note.incomingRelations.size,
                            outgoingRelations = note.outgoingRelations,
                            incomingRelations = note.incomingRelations,
                            onTogglePinClick = {},
                            onEditClick = {},
                            onDeleteClick = {},
                            onCheckboxToggle = { _, _ -> },
                            hideOperations = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp to relative time string.
 */
private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}分钟前"
        duration.toHours() < 24 -> "${duration.toHours()}小时前"
        duration.toDays() < 7 -> "${duration.toDays()}天前"
        else -> {
            val days = duration.toDays()
            when {
                days < 30 -> "${days / 7}周前"
                days < 365 -> "${days / 30}个月前"
                else -> "${days / 365}年前"
            }
        }
    }
}
