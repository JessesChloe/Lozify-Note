package com.witte.lozify.presentation.stroll

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.common.TagUtils
import com.witte.lozify.domain.model.Note
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * RandomStrollBottomSheet - Interactive modal sheet for serendipitous random note strolling.
 *
 * Stage 59: Random Stroll feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomStrollBottomSheet(
    allNotes: List<Note>,
    onDismiss: () -> Unit,
    onEditNote: (Note) -> Unit = {},
    onShareNote: (Long) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val filesDir = context.filesDir

    var currentNoteIndex by remember(allNotes) {
        mutableStateOf(if (allNotes.isNotEmpty()) (allNotes.indices).random() else -1)
    }

    val currentNote = remember(currentNoteIndex, allNotes) {
        if (currentNoteIndex in allNotes.indices) allNotes[currentNoteIndex] else null
    }

    fun pickNextRandomNote() {
        if (allNotes.size <= 1) return
        var nextIndex: Int
        do {
            nextIndex = (allNotes.indices).random()
        } while (nextIndex == currentNoteIndex && allNotes.size > 1)
        currentNoteIndex = nextIndex
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: [👣 随机漫步] + [🎲 换一篇] + [✕]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👣 随机漫步",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Next Random button
                    Button(
                        onClick = { pickNextRandomNote() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Casino,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "换一篇",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853)
                            )
                        }
                    }

                    // Close icon button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (currentNote == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无笔记可漫步，先去记录一条吧！",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                }
            } else {
                AnimatedContent(
                    targetState = currentNote,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "StrollNoteAnim"
                ) { note ->
                    val cleanText = remember(note.content) { RichTextUtils.stripFormatting(note.content) }
                    val tags = remember(note.content, note.tags) {
                        if (note.tags.isNotEmpty()) note.tags.map { it.name } else TagUtils.extractTags(note.content)
                    }
                    val formattedDate = remember(note.createdAt) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.getDefault())
                            .withZone(ZoneId.systemDefault())
                        formatter.format(note.createdAt)
                    }
                    val imageAttachments = remember(note.attachments) { note.attachments.filter { it.isImage() } }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Date & Tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formattedDate,
                                    fontSize = 12.sp,
                                    color = Color(0xFF999999)
                                )

                                if (tags.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        tags.take(3).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "#$tag",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF00C853),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Content body (scrollable max 300dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = cleanText,
                                        fontSize = 15.sp,
                                        color = Color(0xFF222222),
                                        lineHeight = 24.sp
                                    )

                                    if (imageAttachments.isNotEmpty() && filesDir != null) {
                                        com.witte.lozify.presentation.home.AttachmentGrid(
                                            attachments = imageAttachments,
                                            filesDir = filesDir
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)

                            // Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(cleanText))
                                        Toast.makeText(context, "正文已复制", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("复制", color = Color(0xFF666666), fontSize = 13.sp)
                                }

                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onEditNote(note)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("编辑", color = Color(0xFF666666), fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onShareNote(note.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("分享卡片", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
