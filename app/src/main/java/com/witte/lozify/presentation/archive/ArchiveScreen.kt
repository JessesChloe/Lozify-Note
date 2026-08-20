package com.witte.lozify.presentation.archive

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.domain.model.Note
import com.witte.lozify.presentation.home.NoteCard
import java.time.Duration
import java.time.Instant

/**
 * Trash Screen (Stage 10 & 36: Recycle Bin with Long-Press Action Sheet & Permanent Delete Confirmation).
 *
 * Stage 36 UI Refactor:
 * - Removed unintuitive swipe-to-dismiss gestures.
 * - Added Long-Press (and Tap) on note cards to trigger action sheet.
 * - Added safety confirmation dialog for permanent deletion.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    onNavigateBack: () -> Unit,
    archiveViewModel: ArchiveViewModel = hiltViewModel()
) {
    val archivedNotes by archiveViewModel.archivedNotes.collectAsState()
    val context = LocalContext.current

    // Action sheet state
    var selectedNoteForAction by remember { mutableStateOf<Note?>(null) }
    // Permanent deletion confirmation dialog state
    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "回收站",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F8FA)
                )
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        if (archivedNotes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "回收站空空如也",
                        fontSize = 15.sp,
                        color = Color(0xFF999999),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Trashed notes list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archivedNotes, key = { it.id }) { note ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    selectedNoteForAction = note
                                },
                                onLongClick = {
                                    selectedNoteForAction = note
                                }
                            )
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
                            allNotes = archivedNotes,
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

    // Action Bottom Sheet when clicking/long-pressing a trashed card
    if (selectedNoteForAction != null) {
        val note = selectedNoteForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedNoteForAction = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                // Note snippet preview
                val previewSnippet = note.content.trim().ifEmpty { "[无文本内容]" }
                Text(
                    text = previewSnippet.take(80) + if (previewSnippet.length > 80) "..." else "",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F8FA))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: 还原笔记
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val id = note.id
                            selectedNoteForAction = null
                            archiveViewModel.unarchiveNote(id)
                            Toast.makeText(context, "已还原到笔记列表", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "还原笔记",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "还原笔记",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = "将该卡片恢复到首页瀑布流列表",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)

                // Action 2: 彻底删除
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val target = note
                            selectedNoteForAction = null
                            noteToDeletePermanently = target
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "彻底删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "彻底删除",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "永久销毁该笔记及附件，无法找回",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Safety Confirmation Dialog for Permanent Deletion
    if (noteToDeletePermanently != null) {
        val note = noteToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = {
                Text(
                    text = "彻底删除笔记",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF222222)
                )
            },
            text = {
                Text(
                    text = "此操作将永久销毁该笔记以及关联的所有本地图片，彻底删除后将无法找回。\n\n确定要彻底删除吗？",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = note.id
                        noteToDeletePermanently = null
                        archiveViewModel.deleteNotePermanently(id)
                        Toast.makeText(context, "已彻底删除", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "彻底删除",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { noteToDeletePermanently = null }
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF666666)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
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
