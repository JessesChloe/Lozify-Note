package com.witte.lozify.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.witte.lozify.core.common.MarkdownVisualTransformation
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.common.SmartInputFilter
import com.witte.lozify.domain.model.Note

/**
 * NoteEditorBottomSheet - Flomo-style lightweight editor with floating secondary capsule toolbar.
 *
 * Stage 17: 1:1 Flomo Visual Toolbar & List Functionality Alignment.
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
    var isSecondaryCapsuleOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // History stack for Undo / Redo
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    // Collect activeFormats and availableTags from ViewModel
    val activeFormats by viewModel.activeFormats.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()

    var showTagPickerManuallyDismissed by remember { mutableStateOf(false) }

    // Helper to find #tag query before cursor (Triggers after Chinese characters, punctuation, whitespace, newlines, or start of line)
    fun findActiveTagQuery(text: String, cursorPos: Int): Pair<Int, String>? {
        if (cursorPos <= 0 || cursorPos > text.length) return null
        var hashIndex = -1
        for (i in cursorPos - 1 downTo 0) {
            val ch = text[i]
            if (ch == '#') {
                hashIndex = i
                break
            }
            if (ch.isWhitespace() || ch == '\n' || ch == '@') {
                break
            }
        }
        if (hashIndex != -1) {
            val prevChar = if (hashIndex > 0) text[hashIndex - 1] else null
            val isAlphaNumeric = prevChar != null && (prevChar in 'a'..'z' || prevChar in 'A'..'Z' || prevChar in '0'..'9')
            if (!isAlphaNumeric) {
                val query = text.substring(hashIndex + 1, cursorPos)
                return Pair(hashIndex, query)
            }
        }
        return null
    }

    val activeTagQuery = remember(textFieldValue.text, textFieldValue.selection) {
        findActiveTagQuery(textFieldValue.text, textFieldValue.selection.end)
    }
    val isTagPickerVisible = activeTagQuery != null && !showTagPickerManuallyDismissed && availableTags.isNotEmpty()

    // Restore draft if creating a new note and editor is empty
    LaunchedEffect(Unit) {
        if (initialContent == null && textFieldValue.text.isEmpty() && selectedImageUris.isEmpty()) {
            val (draftText, draftUris) = viewModel.getSavedDraft()
            if (draftText.isNotEmpty() || draftUris.isNotEmpty()) {
                textFieldValue = TextFieldValue(text = draftText, selection = TextRange(draftText.length))
                selectedImageUris = draftUris
            }
        }
    }

    // Image picker launcher (Gallery)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val updatedList = selectedImageUris + it
            selectedImageUris = updatedList
            if (initialContent == null) {
                viewModel.saveDraft(textFieldValue.text, updatedList)
            }
        }
    }

    // Helper to update text with undo history and auto-draft
    fun updateTextWithHistory(newValue: TextFieldValue) {
        if (newValue.text != textFieldValue.text) {
            undoStack.add(textFieldValue)
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
            redoStack.clear()

            if (initialContent == null) {
                viewModel.saveDraft(newValue.text, selectedImageUris)
            }
        }
        textFieldValue = newValue
    }

    // Smart value change with input filtering
    fun onValueChange(incomingValue: TextFieldValue) {
        val filteredValue = SmartInputFilter.applySmartInputFilter(textFieldValue, incomingValue)
        val cursorPos = filteredValue.selection.end
        if (cursorPos > 0 && cursorPos <= filteredValue.text.length) {
            val lastChar = filteredValue.text[cursorPos - 1]
            if (lastChar == '@') {
                showNotePicker = true
            }
        }
        showTagPickerManuallyDismissed = false
        updateTextWithHistory(filteredValue)
    }

    // Handle tag autocompletion selection
    fun onSelectTag(tag: com.witte.lozify.domain.model.Tag) {
        val queryInfo = activeTagQuery ?: return
        val hashPos = queryInfo.first
        val currentText = textFieldValue.text
        val cursorPos = textFieldValue.selection.end
        val beforeHash = currentText.substring(0, hashPos)
        val afterCursor = currentText.substring(cursorPos)
        val tagText = "#${tag.name} "
        val newText = beforeHash + tagText + afterCursor
        val newCursorPos = beforeHash.length + tagText.length
        updateTextWithHistory(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursorPos)
            )
        )
    }

    // Helper function to apply formatting
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
            RichTextUtils.FormatType.LIST_UNORDERED -> 2
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
        windowInsets = WindowInsets.ime,
        modifier = modifier.wrapContentHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // Floating NotePicker
                    if (showNotePicker) {
                        NotePicker(
                            allNotes = allNotes,
                            currentNoteId = currentNoteId,
                            onDismiss = { showNotePicker = false },
                            onNoteSelected = { noteId, mentionText ->
                                val currentText = textFieldValue.text
                                val cursorPos = textFieldValue.selection.start
                                val beforeCursor = if (cursorPos > 0 && currentText.getOrNull(cursorPos - 1) == '@') currentText.substring(0, maxOf(0, cursorPos - 1)) else currentText.substring(0, cursorPos)
                                val afterCursor = currentText.substring(cursorPos)
                                val mentionMarkdown = "@[$mentionText](note:$noteId) "
                                updateTextWithHistory(TextFieldValue(text = beforeCursor + mentionMarkdown + afterCursor, selection = TextRange(beforeCursor.length + mentionMarkdown.length)))
                                showNotePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Stage 41: 1:1 Flomo TagPicker (Placed directly on the line above the input field)
                    if (isTagPickerVisible) {
                        activeTagQuery?.let { queryInfo ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TagPicker(
                                    availableTags = availableTags,
                                    tagQuery = queryInfo.second,
                                    onTagSelected = { tag -> onSelectTag(tag) },
                                    onDismiss = { showTagPickerManuallyDismissed = true },
                                    modifier = Modifier.widthIn(min = 250.dp, max = 310.dp)
                                )
                            }
                        }
                    }

                    // Borderless text input field with real-time Markdown syntax highlighting
                    TextField(
                        value = textFieldValue,
                        onValueChange = ::onValueChange,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Color(0xFF454545), lineHeight = 22.sp),
                        visualTransformation = remember { MarkdownVisualTransformation(tagColor = Color(0xFF84A2EE)) },
                        placeholder = { Text(text = "现在的想法是...", color = Color(0xFFB0B0B0), fontSize = 15.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF454545), unfocusedTextColor = Color(0xFF454545),
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF00C853)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 240.dp).focusRequester(focusRequester)
                    )

                    // Image thumbnail preview
                    if (selectedImageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(selectedImageUris) { uri ->
                                Box(modifier = Modifier.size(68.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0F0))) {
                                    AsyncImage(model = uri, contentDescription = "预览图片", contentScale = ContentScale.Crop, modifier = Modifier.size(68.dp))
                                    IconButton(
                                        onClick = {
                                            val updated = selectedImageUris.filter { it != uri }
                                            selectedImageUris = updated
                                            if (initialContent == null) viewModel.saveDraft(textFieldValue.text, updated)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "移除图片", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
                HorizontalDivider(color = Color(0xFFF2F2F2), thickness = 0.8.dp)
                Row(modifier = Modifier.fillMaxWidth().height(52.dp).background(Color.White).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextToolbarButton(text = "#", fontSize = 19.sp, fontWeight = FontWeight.Bold, contentDescription = "插入标签", onClick = {
                            val currentText = textFieldValue.text
                            val cursorPos = textFieldValue.selection.start
                            val newText = currentText.substring(0, cursorPos) + "#" + currentText.substring(cursorPos)
                            updateTextWithHistory(TextFieldValue(text = newText, selection = TextRange(cursorPos + 1)))
                            showTagPickerManuallyDismissed = false
                        })
                        IconToolbarButton(icon = Icons.Outlined.Image, contentDescription = "添加图片", onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                        TextToolbarButton(text = "B", fontSize = 18.sp, fontWeight = FontWeight.Black, contentDescription = "加粗", isActive = activeFormats.contains(RichTextUtils.FormatType.BOLD), onClick = { applyFormatting(RichTextUtils.FormatType.BOLD) })
                        IconToolbarButton(icon = Icons.Outlined.FormatListBulleted, contentDescription = "无序列表", isActive = activeFormats.contains(RichTextUtils.FormatType.LIST_UNORDERED), onClick = { applyFormatting(RichTextUtils.FormatType.LIST_UNORDERED) })
                        IconToolbarButton(icon = Icons.Default.MoreHoriz, contentDescription = "更多格式", isActive = isSecondaryCapsuleOpen, onClick = { isSecondaryCapsuleOpen = !isSecondaryCapsuleOpen })
                    }
                    val hasContent = textFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0FDF4))
                                .clickable { /* Mic feature placeholder */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "语音输入",
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { if (hasContent) { onSave(textFieldValue, selectedImageUris); textFieldValue = TextFieldValue(""); selectedImageUris = emptyList(); viewModel.clearActiveFormats(); viewModel.clearDraft(); onDismiss() } },
                            enabled = hasContent,
                            modifier = Modifier.size(36.dp).background(color = if (hasContent) Color(0xFF00C853) else Color(0xFFEDEDED), shape = CircleShape)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "发送保存", tint = if (hasContent) Color.White else Color(0xFFAAAAAA), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Flomo Style Floating Secondary Capsule Toolbar (Floats stably above primary row)
            androidx.compose.animation.AnimatedVisibility(
                visible = isSecondaryCapsuleOpen,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 58.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEFEFEF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Highlight
                        IconToolbarButton(
                            icon = Icons.Outlined.BorderColor,
                            contentDescription = "高亮",
                            isActive = activeFormats.contains(RichTextUtils.FormatType.HIGHLIGHT),
                            onClick = { applyFormatting(RichTextUtils.FormatType.HIGHLIGHT) }
                        )

                        // 2. Underline
                        IconToolbarButton(
                            icon = Icons.Outlined.FormatUnderlined,
                            contentDescription = "下划线",
                            isActive = activeFormats.contains(RichTextUtils.FormatType.UNDERLINE),
                            onClick = { applyFormatting(RichTextUtils.FormatType.UNDERLINE) }
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFFE0E0E0))
                        )

                        // 3. Mention (@)
                        TextToolbarButton(
                            text = "@",
                            fontSize = 17.sp,
                            contentDescription = "提及笔记",
                            onClick = {
                                val currentText = textFieldValue.text
                                val cursorPos = textFieldValue.selection.start
                                val newText = currentText.substring(0, cursorPos) + "@" + currentText.substring(cursorPos)
                                updateTextWithHistory(
                                    TextFieldValue(text = newText, selection = TextRange(cursorPos + 1))
                                )
                                showNotePicker = true
                            }
                        )

                        // 4. Camera (Photo)
                        IconToolbarButton(
                            icon = Icons.Outlined.PhotoCamera,
                            contentDescription = "拍照",
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFFE0E0E0))
                        )

                        // 5. Undo
                        IconToolbarButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤销",
                            enabled = undoStack.isNotEmpty(),
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    redoStack.add(textFieldValue)
                                    textFieldValue = prev
                                }
                            }
                        )

                        // 6. Redo
                        IconToolbarButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "重做",
                            enabled = redoStack.isNotEmpty(),
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    undoStack.add(textFieldValue)
                                    textFieldValue = next
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Auto-focus when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TextToolbarButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    isActive: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFE8F5E9) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = when {
                !enabled -> Color(0xFFCCCCCC)
                isActive -> Color(0xFF00C853)
                else -> Color(0xFF444444)
            }
        )
    }
}

@Composable
private fun IconToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFE8F5E9) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> Color(0xFFCCCCCC)
                isActive -> Color(0xFF00C853)
                else -> Color(0xFF444444)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}