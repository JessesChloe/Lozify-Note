package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextLayoutResult
import coil.compose.AsyncImage
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.domain.model.Attachment
import java.io.File

/**
 * NoteCard component for displaying a single note with expand/collapse functionality.
 *
 * Features:
 * - White background with 12dp rounded corners
 * - Timestamp display in gray
 * - More options dropdown menu (pin/edit/delete)
 * - Auto-collapse for content exceeding 5 lines
 * - Blue "展开" clickable text to expand content
 *
 * Stage 4: Now supports #tag blue highlighting using RichTextUtils.
 * Stage 5: Added isPinned indicator, onEditClick, onDeleteClick, onTogglePinClick callbacks.
 * Stage 6: Added image attachments rendering in 3-column grid below content.
 * Stage 7: Enhanced with full rich text formatting support (bold, underline, highlight, checkbox).
 *
 * @param content The note content text
 * @param timestamp Display timestamp (e.g., "2分钟前")
 * @param isPinned Whether the note is pinned
 * @param attachments List of image attachments to display
 * @param filesDir Application files directory for resolving image paths
 * @param onTogglePinClick Callback when pin/unpin is clicked
 * @param onEditClick Callback when edit is clicked
 * @param onDeleteClick Callback when delete is clicked
 * @param onTagClick Optional callback when a tag is clicked (tag name without #)
 */
@Composable
fun NoteCard(
    content: String,
    timestamp: String,
    isPinned: Boolean = false,
    attachments: List<Attachment> = emptyList(),
    filesDir: File? = null,
    onTogglePinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showExpandButton by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val maxCollapsedLines = 5

    // Stage 7: Build AnnotatedString with full rich text formatting
    val annotatedContent = remember(content) {
        RichTextUtils.buildAnnotatedStringWithFormatting(
            content = content,
            tagColor = Color(0xFF4C88FF),
            onTagClick = onTagClick
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Pinned indicator + Timestamp + More Icon with Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Stage 5: Pin indicator
                    if (isPinned) {
                        Text(
                            text = "📌",
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = timestamp,
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_more),
                            contentDescription = "更多操作",
                            tint = Color(0xFF999999)
                        )
                    }

                    // Stage 5: Dropdown menu for card operations
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = if (isPinned) "📌" else "📍")
                                    Text(text = if (isPinned) "取消置顶" else "置顶")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onTogglePinClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "✏️")
                                    Text(text = "编辑")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🗑️")
                                    Text(text = "删除")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Content with tag highlighting and expand/collapse logic
            Column {
                // Stage 4: Use ClickableText for tag interactions
                ClickableText(
                    text = annotatedContent,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        lineHeight = 24.sp
                    ),
                    maxLines = if (isExpanded) Int.MAX_VALUE else maxCollapsedLines,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                            showExpandButton = true
                        }
                    },
                    onClick = { offset ->
                        // Handle tag clicks if callback provided
                        onTagClick?.let { callback ->
                            RichTextUtils.getTagAtOffset(annotatedContent, offset)?.let { tagName ->
                                callback(tagName)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Show "展开" link only when content has overflow and not expanded
                if (!isExpanded && showExpandButton) {
                    Text(
                        text = "展开",
                        fontSize = 14.sp,
                        color = Color(0xFF4C88FF),
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { isExpanded = true }
                    )
                }
            }

            // Stage 6: Image attachments in 3-column grid (non-scrolling)
            if (attachments.isNotEmpty() && filesDir != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Chunk attachments into rows of 3
                    attachments.chunked(3).forEach { rowAttachments ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowAttachments.forEach { attachment ->
                                val imageFile = File(filesDir, attachment.filePath)
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = "附件图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            // Fill empty slots with spacers to maintain grid alignment
                            repeat(3 - rowAttachments.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
