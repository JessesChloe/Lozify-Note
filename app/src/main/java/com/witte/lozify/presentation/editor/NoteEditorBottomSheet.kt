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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.witte.lozify.core.common.FileUtils
import com.witte.lozify.core.common.MarkdownVisualTransformation
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.common.SmartInputFilter
import com.witte.lozify.domain.model.Note

/**
 * NoteEditorBottomSheet - Flomo-style lightweight editor with floating secondary capsule toolbar.
 *
 * Stage 17: 1:1 Flomo Visual Toolbar & List Functionality Alignment.
 * Stage 43: Generic file attachment upload & chip preview.
 *
 * @param sheetState Bottom sheet state
 * @param viewModel Editor ViewModel for format state management
 * @param onDismiss Callback when sheet is dismissed
 * @param onSave Callback when save button clicked with TextFieldValue, image URIs, and file URIs
 * @param initialContent Optional initial content for editing (null for new note)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorBottomSheet(
    sheetState: SheetState,
    viewModel: EditorViewModel,
    onDismiss: () -> Unit,
    onSave: (TextFieldValue, List<Uri>, List<Uri>) -> Unit,
    initialContent: String? = null,
    allNotes: List<Note> = emptyList(),
    currentNoteId: Long = 0L,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(initialContent) {
        val text = initialContent ?: ""
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
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
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Helper to find #tag query before cursor (Triggers after English, Chinese, punctuation, whitespace, newlines, or start of line)
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
            val query = text.substring(hashIndex + 1, cursorPos)
            return Pair(hashIndex, query)
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

    // File picker launcher (Generic documents/files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            selectedFileUris = (selectedFileUris + uris).distinct()
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
        val result = RichTextUtils.applyToggleableFormatting(
            content = textFieldValue.text,
            selectionStart = textFieldValue.selection.start,
            selectionEnd = textFieldValue.selection.end,
            formatType = formatType
        )

        updateTextWithHistory(
            TextFieldValue(
                text = result.newText,
                selection = if (result.newCursorStart == result.newCursorEnd) {
                    TextRange(result.newCursorStart)
                } else {
                    TextRange(result.newCursorStart, result.newCursorEnd)
                }
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
                                val cleanText = mentionText.replace(Regex("""[\[\]()@\r\n]"""), " ").trim().take(25).ifBlank { "笔记" }
                                val mentionMarkdown = "@[$cleanText](note:$noteId) "
                                updateTextWithHistory(TextFieldValue(text = beforeCursor + mentionMarkdown + afterCursor, selection = TextRange(beforeCursor.length + mentionMarkdown.length)))
                                showNotePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Borderless text input field with real-time Markdown syntax highlighting and cursor tracking
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = ::onValueChange,
                            onTextLayout = { textLayoutResult = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF454545),
                                lineHeight = 22.sp
                            ),
                            visualTransformation = remember { MarkdownVisualTransformation(tagColor = Color(0xFF84A2EE)) },
                            cursorBrush = SolidColor(Color(0xFF00C853)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 130.dp, max = 240.dp)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "现在的想法是...",
                                            color = Color(0xFFB0B0B0),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Stage 41 & 44: Dynamic Follow-Cursor Popup TagPicker
                        if (isTagPickerVisible) {
                            activeTagQuery?.let { queryInfo ->
                                val cursorPos = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
                                val cursorRect = try {
                                    textLayoutResult?.getCursorRect(cursorPos) ?: Rect.Zero
                                } catch (e: Exception) {
                                    Rect.Zero
                                }
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val leftOffsetPx = remember(density) { with(density) { 12.dp.roundToPx() } }

                                Popup(
                                    popupPositionProvider = remember(cursorRect, leftOffsetPx) {
                                        object : PopupPositionProvider {
                                            override fun calculatePosition(
                                                anchorBounds: IntRect,
                                                windowSize: IntSize,
                                                layoutDirection: LayoutDirection,
                                                popupContentSize: IntSize
                                            ): IntOffset {
                                                val cursorLineTopWindowY = anchorBounds.top + cursorRect.top.toInt()
                                                var popupY = cursorLineTopWindowY - popupContentSize.height - 10
                                                // Keep popup fully visible on screen
                                                if (popupY < 40) {
                                                    popupY = 40
                                                }

                                                // Dynamic horizontal follow-cursor logic:
                                                // 1. Far left: clamped to anchorBounds.left (left edge of text field)
                                                // 2. Far right: clamped to anchorBounds.right - popupContentSize.width (right edge)
                                                // 3. Middle: cursorX minus slight left offset (12.dp)
                                                val cursorAbsoluteX = anchorBounds.left + cursorRect.left.toInt()
                                                val targetPopupX = cursorAbsoluteX - leftOffsetPx

                                                val minX = anchorBounds.left
                                                val maxX = (anchorBounds.right - popupContentSize.width).coerceAtLeast(minX)

                                                val popupX = targetPopupX.coerceIn(minX, maxX)

                                                return IntOffset(popupX, popupY)
                                            }
                                        }
                                    },
                                    properties = PopupProperties(
                                        focusable = false,
                                        dismissOnBackPress = true,
                                        dismissOnClickOutside = true
                                    ),
                                    onDismissRequest = { showTagPickerManuallyDismissed = true }
                                ) {
                                    TagPicker(
                                        availableTags = availableTags,
                                        tagQuery = queryInfo.second,
                                        onTagSelected = { tag -> onSelectTag(tag) },
                                        onDismiss = { showTagPickerManuallyDismissed = true },
                                        modifier = Modifier.widthIn(min = 200.dp, max = 260.dp)
                                    )
                                }
                            }
                        }
                    }

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

                    // Stage 43: Generic file attachments preview
                    if (selectedFileUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedFileUris) { uri ->
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val rawName = FileUtils.getFileName(context, uri)
                                val displayName = FileUtils.getDisplayFileName(rawName)
                                val fileSize = FileUtils.formatFileSize(FileUtils.getFileSize(context, uri))
                                val category = FileUtils.getFileCategory(displayName)

                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
                                    border = BorderStroke(0.8.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (category) {
                                                FileUtils.FileCategory.PDF,
                                                FileUtils.FileCategory.DOCUMENT -> Icons.Outlined.Description
                                                else -> Icons.Outlined.AttachFile
                                            },
                                            contentDescription = null,
                                            tint = when (category) {
                                                FileUtils.FileCategory.PDF -> Color(0xFFE53935)
                                                FileUtils.FileCategory.DOCUMENT -> Color(0xFF1E88E5)
                                                FileUtils.FileCategory.SPREADSHEET -> Color(0xFF43A047)
                                                FileUtils.FileCategory.PRESENTATION -> Color(0xFFFB8C00)
                                                FileUtils.FileCategory.ARCHIVE -> Color(0xFFFFB300)
                                                FileUtils.FileCategory.AUDIO -> Color(0xFF8E24AA)
                                                FileUtils.FileCategory.VIDEO -> Color(0xFF00ACC1)
                                                FileUtils.FileCategory.CODE -> Color(0xFF5E35B1)
                                                FileUtils.FileCategory.OTHER -> Color(0xFF757575)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF333333),
                                            maxLines = 1,
                                            modifier = Modifier.widthIn(max = 130.dp)
                                        )
                                        if (fileSize.isNotBlank()) {
                                            Text(
                                                text = fileSize,
                                                fontSize = 10.sp,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedFileUris = selectedFileUris.filter { it != uri }
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "移除附件",
                                                tint = Color(0xFF888888),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
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
                    val hasContent = textFieldValue.text.isNotBlank() || selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()
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
                            onClick = {
                                if (hasContent) {
                                    onSave(textFieldValue, selectedImageUris, selectedFileUris)
                                    textFieldValue = TextFieldValue("")
                                    selectedImageUris = emptyList()
                                    selectedFileUris = emptyList()
                                    viewModel.clearActiveFormats()
                                    viewModel.clearDraft()
                                    onDismiss()
                                }
                            },
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

                        // 4.5. File Attachment (Upload Generic Files - to the left of Undo)
                        IconToolbarButton(
                            icon = Icons.Outlined.AttachFile,
                            contentDescription = "上传文件附件",
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
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