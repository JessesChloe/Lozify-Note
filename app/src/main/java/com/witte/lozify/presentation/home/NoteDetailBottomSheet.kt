package com.witte.lozify.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.witte.lozify.presentation.components.ImageLightboxDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.domain.model.Note
import java.io.File

/**
 * NoteDetailBottomSheet - Context Thread Timeline View (上下文时间线视图).
 *
 * Stage 13 Refactor (Flomo/Twitter-style Thread View):
 * - Displays a connected timeline of notes: Parents (Backlinks) -> Main Focus Note -> Children (Outgoing Mentions)
 * - Renders a vertical timeline linking cards like a thread
 * - Highlights the focused main note with prominent accent styling
 *
 * @param noteId ID of the central focused note
 * @param filesDir Application files directory for image attachments
 * @param sheetState ModalBottomSheet state for dismiss control
 * @param onDismiss Callback when sheet is dismissed
 * @param onSelectNote Callback when user clicks another note in the thread to switch focus
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailBottomSheet(
    noteId: Long,
    filesDir: File?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectNote: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeLightbox by remember { mutableStateOf<Pair<Int, List<File>>?>(null) }

    LaunchedEffect(noteId) {
        viewModel.loadNoteThread(noteId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF7F8FA),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header: Title + Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "上下文时间线",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(2f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "关闭",
                        tint = Color(0xFF666666)
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1A73E8),
                            strokeWidth = 3.dp
                        )
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "未知错误",
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                    }
                }
                uiState.thread != null -> {
                    val thread = uiState.thread!!
                    val threadItems = buildList {
                        // 1. Parent notes (referencing main note)
                        thread.parents.forEachIndexed { index, parentNote ->
                            add(ThreadDisplayItem(
                                note = parentNote,
                                type = ThreadItemType.PARENT,
                                label = if (thread.parents.size > 1) "上级引用 · ${index + 1}" else "上级引用"
                            ))
                        }
                        // 2. Focused main note
                        add(ThreadDisplayItem(
                            note = thread.mainNote,
                            type = ThreadItemType.MAIN,
                            label = "当前焦点笔记"
                        ))
                        // 3. Child notes (referenced by main note)
                        thread.children.forEachIndexed { index, childNote ->
                            add(ThreadDisplayItem(
                                note = childNote,
                                type = ThreadItemType.CHILD,
                                label = if (thread.children.size > 1) "衍生提及 · ${index + 1}" else "衍生提及"
                            ))
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(threadItems, key = { _, item -> "${item.type}_${item.note.id}" }) { index, item ->
                            val isFirst = (index == 0)
                            val isLast = (index == threadItems.size - 1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                // Left: Timeline connector line and dot
                                TimelineIndicator(
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    type = item.type,
                                    modifier = Modifier.fillMaxHeight()
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Right: Note content card with header label
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (isLast) 24.dp else 16.dp)
                                ) {
                                    // Node label header
                                    when (item.type) {
                                        ThreadItemType.MAIN -> {
                                            Row(
                                                modifier = Modifier.padding(bottom = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFE8F0FE))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "🎯 当前焦点笔记",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1A73E8)
                                                    )
                                                }
                                            }
                                        }
                                        ThreadItemType.PARENT -> {
                                            Text(
                                                text = "🔗 ${item.label}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF757575),
                                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                            )
                                        }
                                        ThreadItemType.CHILD -> {
                                            Text(
                                                text = "↳ ${item.label}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF1A73E8),
                                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                            )
                                        }
                                    }

                                    // Card Body
                                    val isMain = item.type == ThreadItemType.MAIN
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isMain) Color(0xFFF9FBFF) else Color.White)
                                            .border(
                                                border = if (isMain) {
                                                    BorderStroke(1.5.dp, Color(0xFF4C88FF))
                                                } else {
                                                    BorderStroke(1.dp, Color(0xFFE8EAED))
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .then(
                                                if (!isMain && onSelectNote != null) {
                                                    Modifier.clickable { onSelectNote(item.note.id) }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        NoteCard(
                                            noteId = item.note.id,
                                            content = item.note.content,
                                            timestamp = formatRelativeTime(item.note.createdAt),
                                            isPinned = item.note.isPinned,
                                            attachments = item.note.attachments,
                                            outgoingRelationsCount = 0,
                                            incomingRelationsCount = 0,
                                            isHighlighted = isMain,
                                            filesDir = filesDir,
                                            onTogglePinClick = { /* Read-only in thread */ },
                                            onEditClick = { /* Read-only in thread */ },
                                            onDeleteClick = { /* Read-only in thread */ },
                                            onCheckboxToggle = { _, _ -> /* Read-only */ },
                                            onTagClick = null,
                                            onMentionClick = { clickedNoteId ->
                                                onSelectNote?.invoke(clickedNoteId)
                                            },
                                            onRelationsClick = null,
                                            outgoingRelations = emptyList(),
                                            incomingRelations = emptyList(),
                                            onImageClick = { index, images ->
                                                activeLightbox = Pair(index, images)
                                            },
                                            hideOperations = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stage 16: Fullscreen Image Lightbox
        activeLightbox?.let { (initialIndex, images) ->
            ImageLightboxDialog(
                images = images,
                initialIndex = initialIndex,
                onDismiss = { activeLightbox = null }
            )
        }
    }
}

/**
 * Timeline rail indicator with connector lines and styled node dot.
 */
@Composable
private fun TimelineIndicator(
    isFirst: Boolean,
    isLast: Boolean,
    type: ThreadItemType,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(22.dp)
            .fillMaxHeight()
    ) {
        val centerX = size.width / 2f
        val dotCenterY = 32.dp.toPx() // Vertically aligned with top area of the card

        val lineColor = Color(0xFFDCE2E8)
        val strokeWidth = 2.dp.toPx()

        // Top vertical line
        if (!isFirst) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, dotCenterY - 8.dp.toPx()),
                strokeWidth = strokeWidth
            )
        }

        // Bottom vertical line
        if (!isLast) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, dotCenterY + 8.dp.toPx()),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Node dot
        when (type) {
            ThreadItemType.MAIN -> {
                // Glowing outer ring
                drawCircle(
                    color = Color(0xFFE8F0FE),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, dotCenterY)
                )
                // Solid center dot
                drawCircle(
                    color = Color(0xFF1A73E8),
                    radius = 4.5.dp.toPx(),
                    center = Offset(centerX, dotCenterY)
                )
            }
            ThreadItemType.PARENT -> {
                drawCircle(
                    color = Color(0xFF9E9E9E),
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, dotCenterY)
                )
            }
            ThreadItemType.CHILD -> {
                drawCircle(
                    color = Color(0xFF4C88FF),
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, dotCenterY)
                )
            }
        }
    }
}

private enum class ThreadItemType {
    PARENT,
    MAIN,
    CHILD
}

private data class ThreadDisplayItem(
    val note: Note,
    val type: ThreadItemType,
    val label: String
)

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
