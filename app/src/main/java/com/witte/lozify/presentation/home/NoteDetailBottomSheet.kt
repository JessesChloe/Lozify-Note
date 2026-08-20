package com.witte.lozify.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.domain.model.Note
import com.witte.lozify.presentation.components.ImageLightboxDialog
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * NoteDetailBottomSheet - Option B: Single-Card Focus with Progressive Relations.
 *
 * Stage 47 Refactor (Flomo/Atomic Card Alignment):
 * - Displays the focused note with 100% full content fidelity (rich text, tags, attachments, files).
 * - Replaced heavy 3-layer thread timeline with lightweight bottom relation capsules (出链 ↖ / 反链 🔗).
 * - Smooth in-place switching: tapping any relation chip smoothly navigates to that note in-sheet.
 *
 * @param noteId ID of the central focused note
 * @param filesDir Application files directory for image/file attachments
 * @param sheetState ModalBottomSheet state for dismiss control
 * @param onDismiss Callback when sheet is dismissed
 * @param onSelectNote Callback when user clicks another relation note
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var currentViewingNoteId by remember { mutableStateOf(noteId) }

    LaunchedEffect(noteId) {
        currentViewingNoteId = noteId
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
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
                    val note = thread.mainNote
                    val scrollState = rememberScrollState()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            // Top Bar: Timestamp + Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatRelativeTime(note.createdAt),
                                        fontSize = 12.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                    if (note.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "置顶",
                                            tint = Color(0xFF4C88FF),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val plainText = RichTextUtils.stripFormatting(note.content)
                                            clipboardManager.setText(AnnotatedString(plainText))
                                            Toast.makeText(context, "内容已复制", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "复制内容",
                                            tint = Color(0xFF888888),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "关闭",
                                            tint = Color(0xFF888888),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Note Body with Rich Text & Tags
                            val parsed = remember(note.content) {
                                RichTextUtils.parseRichText(note.content, tagColor = Color(0xFF84A2EE))
                            }

                            val inlineContent = remember(parsed.tags) {
                                buildMap {
                                    parsed.tags.forEach { tag ->
                                        put(
                                            "tag_$tag",
                                            InlineTextContent(
                                                Placeholder(
                                                    width = RichTextUtils.calculateTagBadgeWidth(tag),
                                                    height = 20.sp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFEBF3FF))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "#$tag",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF4C88FF),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }

                            Text(
                                text = parsed.annotatedString,
                                inlineContent = inlineContent,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF333333),
                                    lineHeight = 22.sp
                                ),
                                onTextLayout = { layoutResult: TextLayoutResult ->
                                    textLayoutResultState = layoutResult
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(parsed.annotatedString) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                textLayoutResultState?.let { layoutResult ->
                                                    val charOffset = layoutResult.getOffsetForPosition(offset)
                                                    val url = RichTextUtils.getUrlAtOffset(parsed.annotatedString, charOffset)
                                                    if (url != null) {
                                                        try {
                                                            val fullUrl = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                                                                url
                                                            } else {
                                                                "https://$url"
                                                            }
                                                            uriHandler.openUri(fullUrl)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )

                            // Image Attachments
                            val imageAttachments = note.attachments.filter { it.isImage() }
                            if (imageAttachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AttachmentGrid(
                                    attachments = imageAttachments,
                                    filesDir = filesDir,
                                    onImageClick = { index, images ->
                                        activeLightbox = Pair(index, images)
                                    }
                                )
                            }

                            // Generic File Attachments
                            val fileAttachments = note.attachments.filter { it.isFile() }
                            if (fileAttachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FileAttachmentList(
                                    attachments = fileAttachments,
                                    filesDir = filesDir ?: context.filesDir
                                )
                            }

                            // Progressive Relations Section (出链 ↖ / 反链 🔗)
                            val outgoing = note.outgoingRelations
                            val incoming = note.incomingRelations

                            if (outgoing.isNotEmpty() || incoming.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Outgoing Mentions (引用的笔记 ↖)
                                if (outgoing.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF8E8E93)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .graphicsLayer(rotationZ = 45f)
                                            )
                                        }
                                        Text(
                                            text = "引用的笔记 (${outgoing.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF888888)
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        outgoing.forEach { mention ->
                                            val targetNote = thread.children.find { it.id == mention.toNoteId }
                                            val title = targetNote?.getCleanSummary(30)
                                                ?: mention.mentionText.ifBlank { "关联笔记" }

                                            RelationChip(
                                                title = title,
                                                icon = {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4C88FF),
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .graphicsLayer(rotationZ = 45f)
                                                    )
                                                },
                                                onClick = {
                                                    currentViewingNoteId = mention.toNoteId
                                                    viewModel.loadNoteThread(mention.toNoteId)
                                                    onSelectNote?.invoke(mention.toNoteId)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (outgoing.isNotEmpty() && incoming.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Incoming Backlinks (被引用的笔记 🔗)
                                if (incoming.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF8E8E93)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp)
                                            )
                                        }
                                        Text(
                                            text = "被引用的笔记 (${incoming.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF888888)
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        incoming.forEach { relation ->
                                            val sourceNote = thread.parents.find { it.id == relation.fromNoteId }
                                            val title = sourceNote?.getCleanSummary(30)
                                                ?: "关联笔记 #${relation.fromNoteId}"

                                            RelationChip(
                                                title = title,
                                                icon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Link,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4C88FF),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                },
                                                onClick = {
                                                    currentViewingNoteId = relation.fromNoteId
                                                    viewModel.loadNoteThread(relation.fromNoteId)
                                                    onSelectNote?.invoke(relation.fromNoteId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Image Lightbox
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
 * Clickable capsule chip for linked relations.
 */
@Composable
private fun RelationChip(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F4F7))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Format timestamp to relative time string for detail view.
 */
private fun formatRelativeTime(instant: Instant): String {
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
