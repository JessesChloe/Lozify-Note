package com.witte.lozify.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Note
import java.io.File

/**
 * NoteDetailBottomSheet - Half-screen modal displaying full note content.
 *
 * Stage 8 UX Enhancement: Replaces scroll-to-list interaction with direct note preview.
 *
 * Features:
 * - Displays complete note content in a bottom sheet
 * - Reuses NoteCard component for consistent rendering
 * - Hides card operations (pin/edit/delete) to prevent infinite nesting
 * - Includes close button with keyboard arrow down icon
 *
 * @param note The note to display
 * @param filesDir Application files directory for image attachments
 * @param sheetState ModalBottomSheet state for dismiss control
 * @param onDismiss Callback when sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailBottomSheet(
    note: Note,
    filesDir: File?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Title + Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "笔记详情",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "关闭",
                        tint = Color(0xFF666666)
                    )
                }
            }

            // Note content (reuse NoteCard but hide operations)
            NoteCard(
                noteId = note.id,
                content = note.content,
                timestamp = formatRelativeTime(note.createdAt),
                isPinned = note.isPinned,
                attachments = note.attachments,
                outgoingRelationsCount = 0,  // Hide relations in detail view
                incomingRelationsCount = 0,
                isHighlighted = false,
                filesDir = filesDir,
                onTogglePinClick = { /* No-op: hide operations */ },
                onEditClick = { /* No-op: hide operations */ },
                onDeleteClick = { /* No-op: hide operations */ },
                onCheckboxToggle = { _, _ -> /* No-op: read-only */ },
                onTagClick = null,  // Disable tag click in detail view
                onMentionClick = null,  // Disable mention click to prevent infinite nesting
                onRelationsClick = null,  // Hide relations
                outgoingRelations = emptyList(),
                incomingRelations = emptyList(),
                hideOperations = true  // New parameter to hide dropdown menu
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Format timestamp to relative time string for detail view.
 */
private fun formatRelativeTime(instant: java.time.Instant): String {
    val now = java.time.Instant.now()
    val duration = java.time.Duration.between(instant, now)

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
