package com.witte.lozify.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.witte.lozify.core.common.MarkdownVisualTransformation
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.domain.model.Note

/**
 * NoteEditorBottomSheet - Flomo-style lightweight editor.
 *
 * Stage 15 Refactor:
 * - Abolished secondary modal bottom sheet formatting menus.
 * - Added Keyboard Accessory Toolbar docked directly above the IME keyboard.
 * - Live markdown formatting without dismissing keyboard or losing focus.
 * - Built-in Undo / Redo history tracking.
 *
 * @param sheetState Bottom sheet state
 * @param viewModel Editor ViewModel for format state management
 * @param onDismiss Callback when sheet is dismissed
 * @param onSave Callback when save button clicked with TextFieldValue and image URIs
 * @param initialContent Optional initial content for editing (null for new note)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorBottomSheet(
    sheetState: SheetState,
    viewModel: EditorViewModel,
    onDismiss: () -> Unit,
    onSave: (TextFieldValue, List<Uri>) -> Unit,
    initialContent: String? = null,
    allNotes: List<Note> = emptyList(),
    currentNoteId: Long = 0L,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(initialContent) {
        mutableStateOf(TextFieldValue(text = initialContent ?: ""))
    }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showNotePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // History stack for Undo / Redo
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    // Collect activeFormats from ViewModel
    val activeFormats by viewModel.activeFormats.collectAsState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUris = selectedImageUris + it
        }
    }

    // Helper to update text with undo history tracking
    fun updateTextWithHistory(newValue: TextFieldValue) {
        if (newValue.text != textFieldValue.text) {
            undoStack.add(textFieldValue)
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }
        textFieldValue = newValue
    }

    // Normal value change
    fun onValueChange(newValue: TextFieldValue) {
        val cursorPos = newValue.selection.end
        if (cursorPos > 0 && cursorPos <= newValue.text.length) {
            val lastChar = newValue.text[cursorPos - 1]
            if (lastChar == '@') {
                showNotePicker = true
            }
        }
        updateTextWithHistory(newValue)
    }

    // Helper function to apply formatting with bounds checking
    fun applyFormatting(formatType: RichTextUtils.FormatType) {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection

        val min = selection.min
        val max = selection.max

        if (min < 0 || max > currentText.length) {
            return
        }

        val newText = RichTextUtils.insertFormatting(
            content = currentText,
            selectionStart = min,
            selectionEnd = max,
            formatType = formatType
        )

        val markerLength = when (formatType) {
            RichTextUtils.FormatType.BOLD,
            RichTextUtils.FormatType.UNDERLINE,
            RichTextUtils.FormatType.HIGHLIGHT -> 2
            RichTextUtils.FormatType.MENTION -> 0
            RichTextUtils.FormatType.CHECKBOX_UNCHECKED -> 6
            RichTextUtils.FormatType.CHECKBOX_CHECKED -> 6
        }

        val newCursorPos = if (min == max) {
            min + markerLength
        } else {
            max + markerLength * 2
        }

        val safeCursorPos = newCursorPos.coerceIn(0, newText.length)

        updateTextWithHistory(
            TextFieldValue(
                text = newText,
                selection = TextRange(safeCursorPos)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearActiveFormats()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier.wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // NotePicker ABOVE TextField to avoid keyboard occlusion
                if (showNotePicker) {
                    NotePicker(
                        allNotes = allNotes,
                        currentNoteId = currentNoteId,
                        onDismiss = { showNotePicker = false },
                        onNoteSelected = { noteId, mentionText ->
                            val currentText = textFieldValue.text
                            val cursorPos = textFieldValue.selection.start

                            val beforeCursor = if (cursorPos > 0 && currentText.getOrNull(cursorPos - 1) == '@') {
                                currentText.substring(0, maxOf(0, cursorPos - 1))
                            } else {
                                currentText.substring(0, cursorPos)
                            }
                            val afterCursor = currentText.substring(cursorPos)

                            val mentionMarkdown = "@[$mentionText](note:$noteId) "
                            val newText = beforeCursor + mentionMarkdown + afterCursor

                            val newCursorPos = beforeCursor.length + mentionMarkdown.length
                            updateTextWithHistory(
                                TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursorPos)
                                )
                            )

                            showNotePicker = false
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Top action bar: Title & Save send button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialContent != null) "编辑笔记" else "记录想法",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textFieldValue.text.isNotBlank()) {
                                onSave(textFieldValue, selectedImageUris)
                                textFieldValue = TextFieldValue("")
                                selectedImageUris = emptyList()
                                viewModel.clearActiveFormats()
                                onDismiss()
                            }
                        },
                        enabled = textFieldValue.text.isNotBlank(),
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (textFieldValue.text.isNotBlank()) Color(0xFF00C853) else Color(0xFFE0E0E0),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "保存",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Borderless text input field with real-time Markdown syntax highlighting
                TextField(
                    value = textFieldValue,
                    onValueChange = ::onValueChange,
                    visualTransformation = remember { MarkdownVisualTransformation() },
                    placeholder = {
                        Text(
                            text = "现在的想法是...",
                            color = Color(0xFF999999),
                            fontSize = 16.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .focusRequester(focusRequester)
                )

                // Image thumbnail preview
                if (selectedImageUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImageUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0F0F0))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "预览图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp)
                                )

                                IconButton(
                                    onClick = {
                                        selectedImageUris = selectedImageUris.filter { it != uri }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除图片",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)

            // Stage 15: Keyboard Accessory Toolbar (常驻键盘上方工具栏)
            KeyboardAccessoryToolbar(
                activeFormats = activeFormats,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onTagClick = {
                    val currentText = textFieldValue.text
                    val cursorPos = textFieldValue.selection.start
                    val newText = currentText.substring(0, cursorPos) + "#" + currentText.substring(cursorPos)
                    updateTextWithHistory(
                        TextFieldValue(text = newText, selection = TextRange(cursorPos + 1))
                    )
                },
                onMentionClick = {
                    val currentText = textFieldValue.text
                    val cursorPos = textFieldValue.selection.start
                    val newText = currentText.substring(0, cursorPos) + "@" + currentText.substring(cursorPos)
                    updateTextWithHistory(
                        TextFieldValue(text = newText, selection = TextRange(cursorPos + 1))
                    )
                    showNotePicker = true
                },
                onImageClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onBoldClick = {
                    applyFormatting(RichTextUtils.FormatType.BOLD)
                },
                onHighlightClick = {
                    applyFormatting(RichTextUtils.FormatType.HIGHLIGHT)
                },
                onUnderlineClick = {
                    applyFormatting(RichTextUtils.FormatType.UNDERLINE)
                },
                onCheckboxClick = {
                    applyFormatting(RichTextUtils.FormatType.CHECKBOX_UNCHECKED)
                },
                onUndoClick = {
                    if (undoStack.isNotEmpty()) {
                        val prev = undoStack.removeAt(undoStack.lastIndex)
                        redoStack.add(textFieldValue)
                        textFieldValue = prev
                    }
                },
                onRedoClick = {
                    if (redoStack.isNotEmpty()) {
                        val next = redoStack.removeAt(redoStack.lastIndex)
                        undoStack.add(textFieldValue)
                        textFieldValue = next
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Auto-focus when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * KeyboardAccessoryToolbar - Persistent formatting toolbar docked directly above the soft keyboard.
 *
 * Stage 15: Flomo-Style Collapsible Toolbar ("..." Expand/Collapse)
 * - Primary row (bottom): Core actions (#, @, 📷, B, ☑, ...)
 * - Secondary row (top, animated): Extended formatting (H, U, ↩, ↪)
 * - Retains soft keyboard focus and cursor state during expand/collapse transitions
 *
 * @param activeFormats Currently active format locks from ViewModel
 * @param canUndo Whether undo action is available
 * @param canRedo Whether redo action is available
 */
@Composable
fun KeyboardAccessoryToolbar(
    activeFormats: Set<RichTextUtils.FormatType> = emptySet(),
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onTagClick: () -> Unit,
    onMentionClick: () -> Unit,
    onImageClick: () -> Unit,
    onBoldClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
    ) {
        // Secondary Row (Extended Tools: H, U, Undo, Redo)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(
                    text = "H",
                    fontWeight = FontWeight.Bold,
                    contentDescription = "高亮",
                    isActive = activeFormats.contains(RichTextUtils.FormatType.HIGHLIGHT),
                    onClick = onHighlightClick
                )

                ToolbarIconButton(
                    text = "U",
                    fontWeight = FontWeight.Medium,
                    contentDescription = "下划线",
                    isActive = activeFormats.contains(RichTextUtils.FormatType.UNDERLINE),
                    onClick = onUnderlineClick
                )

                ToolbarIconButton(
                    text = "↩",
                    contentDescription = "撤销",
                    enabled = canUndo,
                    onClick = onUndoClick
                )

                ToolbarIconButton(
                    text = "↪",
                    contentDescription = "重做",
                    enabled = canRedo,
                    onClick = onRedoClick
                )
            }
        }

        if (isExpanded) {
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
        }

        // Primary Row (Core Tools: #, @, 📷, B, ☑, ...)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(
                text = "#",
                contentDescription = "插入标签",
                onClick = onTagClick
            )

            ToolbarIconButton(
                text = "@",
                contentDescription = "提及笔记",
                onClick = onMentionClick
            )

            ToolbarIconButton(
                text = "📷",
                contentDescription = "添加图片",
                onClick = onImageClick
            )

            ToolbarIconButton(
                text = "B",
                fontWeight = FontWeight.Bold,
                contentDescription = "加粗",
                isActive = activeFormats.contains(RichTextUtils.FormatType.BOLD),
                onClick = onBoldClick
            )

            ToolbarIconButton(
                text = "☑",
                contentDescription = "待办事项框",
                onClick = onCheckboxClick
            )

            // More / Collapse toggle button ("...") right next to todo icon
            ToolbarIconButton(
                text = "...",
                contentDescription = if (isExpanded) "收起更多格式" else "展开更多格式",
                isActive = isExpanded,
                onClick = { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    isActive: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) Color(0xFFE8F0FE) else Color.Transparent
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = fontWeight,
            color = when {
                !enabled -> Color(0xFFCCCCCC)
                isActive -> Color(0xFF1A73E8)
                else -> Color(0xFF555555)
            }
        )
    }
}