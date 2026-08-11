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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.witte.lozify.domain.model.NoteRelation
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
 * Stage 7 Bug Fix: Added interactive checkbox support - clicking checkbox updates note content.
 * Stage 8: Added @mention click support and relation count display.
 *
 * @param noteId The note ID for checkbox updates
 * @param content The note content text
 * @param timestamp Display timestamp (e.g., "2分钟前")
 * @param isPinned Whether the note is pinned
 * @param attachments List of image attachments to display
 * @param outgoingRelationsCount Number of outgoing relations (notes this note mentions)
 * @param incomingRelationsCount Number of incoming relations (notes that mention this note)
 * @param filesDir Application files directory for resolving image paths
 * @param onTogglePinClick Callback when pin/unpin is clicked
 * @param onEditClick Callback when edit is clicked
 * @param onDeleteClick Callback when delete is clicked
 * @param onCheckboxToggle Callback when checkbox is toggled (noteId, newContent)
 * @param onTagClick Optional callback when a tag is clicked (tag name without #)
 * @param onMentionClick Optional callback when a mention is clicked (note ID)
 */
@Composable
fun NoteCard(
    noteId: Long,
    content: String,
    timestamp: String,
    isPinned: Boolean = false,
    attachments: List<Attachment> = emptyList(),
    outgoingRelationsCount: Int = 0,
    incomingRelationsCount: Int = 0,
    outgoingRelations: List<NoteRelation> = emptyList(),
    incomingRelations: List<NoteRelation> = emptyList(),
    isHighlighted: Boolean = false,
    filesDir: File? = null,
    onTogglePinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCheckboxToggle: (Long, String) -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    onMentionClick: ((Long) -> Unit)? = null,
    onRelationsClick: ((List<NoteRelation>, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showExpandButton by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val maxCollapsedLines = 5

    // Stage 7: Parse content into lines and separate checkbox items from regular content
    val contentLines = remember(content) {
        content.split("\n")
    }

    val checkboxItems = remember(content) {
        contentLines.mapIndexedNotNull { index, line ->
            val checkboxMatch = Regex("""^- \[([ x])\] (.+)$""").find(line)
            checkboxMatch?.let {
                CheckboxItem(
                    lineIndex = index,
                    isChecked = it.groupValues[1] == "x",
                    text = it.groupValues[2]
                )
            }
        }
    }

    val nonCheckboxContent = remember(content) {
        contentLines.filterIndexed { index, line ->
            !line.matches(Regex("""^- \[([ x])\] .+$"""))
        }.joinToString("\n")
    }

    // Build AnnotatedString for non-checkbox content
    val annotatedContent = remember(nonCheckboxContent) {
        RichTextUtils.buildAnnotatedStringWithFormatting(
            content = nonCheckboxContent,
            tagColor = Color(0xFF4C88FF),
            onTagClick = onTagClick,
            onMentionClick = onMentionClick
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlighted) Color(0xFFE3F2FD) else Color.White)
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
                // Stage 7 Bug Fix: Render interactive checkboxes
                checkboxItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { isChecked ->
                                // Toggle checkbox in content
                                val newContent = contentLines.mapIndexed { index, line ->
                                    if (index == item.lineIndex) {
                                        if (isChecked) {
                                            line.replace("- [ ]", "- [x]")
                                        } else {
                                            line.replace("- [x]", "- [ ]")
                                        }
                                    } else {
                                        line
                                    }
                                }.joinToString("\n")
                                onCheckboxToggle(noteId, newContent)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF00C853),
                                uncheckedColor = Color(0xFF9CA3AF)
                            )
                        )
                        Text(
                            text = item.text,
                            fontSize = 16.sp,
                            color = Color(0xFF333333),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Render non-checkbox content with formatting
                if (nonCheckboxContent.isNotBlank()) {
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
                            // Stage 4: Handle tag clicks if callback provided
                            onTagClick?.let { callback ->
                                RichTextUtils.getTagAtOffset(annotatedContent, offset)?.let { tagName ->
                                    callback(tagName)
                                }
                            }
                            // Stage 8: Handle @mention clicks if callback provided
                            onMentionClick?.let { callback ->
                                RichTextUtils.getMentionAtOffset(annotatedContent, offset)?.let { noteId ->
                                    callback(noteId)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

            // Stage 8: Display relation counts at the bottom (clickable)
            val totalRelations = outgoingRelationsCount + incomingRelationsCount
            if (totalRelations > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (outgoingRelationsCount > 0) {
                        Text(
                            text = "🔗 $outgoingRelationsCount 个引用",
                            fontSize = 11.sp,
                            color = Color(0xFF4C88FF),
                            modifier = Modifier
                                .clickable {
                                    onRelationsClick?.invoke(outgoingRelations, "出链列表")
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                    if (incomingRelationsCount > 0) {
                        Text(
                            text = "🔙 $incomingRelationsCount 个反链",
                            fontSize = 11.sp,
                            color = Color(0xFF4C88FF),
                            modifier = Modifier
                                .clickable {
                                    onRelationsClick?.invoke(incomingRelations, "反链列表")
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a checkbox item in note content.
 *
 * @param lineIndex Index of the line in original content
 * @param isChecked Whether the checkbox is checked
 * @param text Text content after the checkbox marker
 */
private data class CheckboxItem(
    val lineIndex: Int,
    val isChecked: Boolean,
    val text: String
)
