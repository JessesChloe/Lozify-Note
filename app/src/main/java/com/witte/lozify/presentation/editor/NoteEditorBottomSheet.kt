package com.witte.lozify.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.witte.lozify.core.common.RichTextUtils

/**
 * NoteEditorBottomSheet - Flomo-style lightweight editor.
 *
 * Stage 9 Refactor: Complete redesign for Flomo-level UX.
 *
 * Design Philosophy:
 * - Adaptive height (wrapContentHeight) - only ~50% screen, no fullscreen takeover
 * - Borderless input field with "现在的想法是..." placeholder
 * - Bottom toolbar redesign:
 *   - Left: Minimal icon buttons (#, 📷, B, ...) at 20dp size, no background
 *   - Right: Circular send icon button (paper plane style)
 * - WYSIWYG rich text engine with format locking (activeFormats state)
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
    allNotes: List<com.witte.lozify.domain.model.Note> = emptyList(),
    currentNoteId: Long = 0L,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(initialContent) {
        mutableStateOf(TextFieldValue(text = initialContent ?: ""))
    }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showFormattingMenu by remember { mutableStateOf(false) }
    var showNotePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Collect activeFormats from ViewModel
    val activeFormats by viewModel.activeFormats.collectAsState()

    // Stage 6: Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUris = selectedImageUris + it
        }
    }

    // Normal value change - no interception needed
    // Cursor is already positioned inside markers by toggleFormat
    fun onValueChange(newValue: TextFieldValue) {
        // Detect @ trigger for mention picker
        val cursorPos = newValue.selection.end
        if (cursorPos > 0 && cursorPos <= newValue.text.length) {
            val lastChar = newValue.text[cursorPos - 1]
            if (lastChar == '@') {
                showNotePicker = true
            }
        }

        // Just update normally - no wrapping needed
        textFieldValue = newValue
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

        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(safeCursorPos)
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearActiveFormats()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier.wrapContentHeight() // Key: Adaptive height, not fullscreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Borderless text input field
            TextField(
                value = textFieldValue,
                onValueChange = ::onValueChange,
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
                    .height(150.dp)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Image thumbnail preview
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0F0F0))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "预览图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp)
                            )

                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris.filter { it != uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "移除图片",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Bottom toolbar: Flomo-style minimal layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Minimal function icons
                FlomoToolbar(
                    activeFormats = activeFormats,
                    onTagClick = {
                        // Insert # at cursor position
                        val currentText = textFieldValue.text
                        val cursorPos = textFieldValue.selection.start
                        val newText = currentText.substring(0, cursorPos) + "#" + currentText.substring(cursorPos)
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(cursorPos + 1)
                        )
                    },
                    onImageClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onBoldClick = {
                        val selection = textFieldValue.selection
                        if (selection.start == selection.end) {
                            // No selection: toggle format lock with cursor positioning
                            val isBoldActive = activeFormats.contains(RichTextUtils.FormatType.BOLD)

                            if (!isBoldActive) {
                                // Lock ON: Insert **** and move cursor to middle
                                val currentText = textFieldValue.text
                                val cursorPos = textFieldValue.selection.start
                                val newText = currentText.substring(0, cursorPos) + "****" + currentText.substring(cursorPos)
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(cursorPos + 2)  // Cursor between **|**
                                )
                                viewModel.toggleFormat(RichTextUtils.FormatType.BOLD)
                            } else {
                                // Lock OFF: Move cursor right 2 positions to jump out
                                val currentText = textFieldValue.text
                                val cursorPos = textFieldValue.selection.start
                                val newCursorPos = (cursorPos + 2).coerceIn(0, currentText.length)
                                textFieldValue = TextFieldValue(
                                    text = currentText,
                                    selection = TextRange(newCursorPos)
                                )
                                viewModel.toggleFormat(RichTextUtils.FormatType.BOLD)
                            }
                        } else {
                            // Has selection: apply formatting immediately
                            applyFormatting(RichTextUtils.FormatType.BOLD)
                        }
                    },
                    onMoreClick = {
                        showFormattingMenu = true
                    }
                )

                // Right: Circular send button
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
                        .size(40.dp)
                        .background(
                            color = if (textFieldValue.text.isNotBlank()) Color(0xFF00C853) else Color(0xFFCCCCCC),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "保存",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Formatting menu bottom sheet
    if (showFormattingMenu) {
        FormattingMenu(
            onDismiss = { showFormattingMenu = false },
            onUnderlineClick = {
                val selection = textFieldValue.selection
                if (selection.start == selection.end) {
                    // No selection: toggle format lock with cursor positioning
                    val isUnderlineActive = activeFormats.contains(RichTextUtils.FormatType.UNDERLINE)

                    if (!isUnderlineActive) {
                        // Lock ON: Insert ____ and move cursor to middle
                        val currentText = textFieldValue.text
                        val cursorPos = textFieldValue.selection.start
                        val newText = currentText.substring(0, cursorPos) + "____" + currentText.substring(cursorPos)
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(cursorPos + 2)  // Cursor between __|__
                        )
                        viewModel.toggleFormat(RichTextUtils.FormatType.UNDERLINE)
                    } else {
                        // Lock OFF: Move cursor right 2 positions to jump out
                        val currentText = textFieldValue.text
                        val cursorPos = textFieldValue.selection.start
                        val newCursorPos = (cursorPos + 2).coerceIn(0, currentText.length)
                        textFieldValue = TextFieldValue(
                            text = currentText,
                            selection = TextRange(newCursorPos)
                        )
                        viewModel.toggleFormat(RichTextUtils.FormatType.UNDERLINE)
                    }
                } else {
                    applyFormatting(RichTextUtils.FormatType.UNDERLINE)
                }
            },
            onHighlightClick = {
                val selection = textFieldValue.selection
                if (selection.start == selection.end) {
                    // No selection: toggle format lock with cursor positioning
                    val isHighlightActive = activeFormats.contains(RichTextUtils.FormatType.HIGHLIGHT)

                    if (!isHighlightActive) {
                        // Lock ON: Insert ==== and move cursor to middle
                        val currentText = textFieldValue.text
                        val cursorPos = textFieldValue.selection.start
                        val newText = currentText.substring(0, cursorPos) + "====" + currentText.substring(cursorPos)
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(cursorPos + 2)  // Cursor between ==|==
                        )
                        viewModel.toggleFormat(RichTextUtils.FormatType.HIGHLIGHT)
                    } else {
                        // Lock OFF: Move cursor right 2 positions to jump out
                        val currentText = textFieldValue.text
                        val cursorPos = textFieldValue.selection.start
                        val newCursorPos = (cursorPos + 2).coerceIn(0, currentText.length)
                        textFieldValue = TextFieldValue(
                            text = currentText,
                            selection = TextRange(newCursorPos)
                        )
                        viewModel.toggleFormat(RichTextUtils.FormatType.HIGHLIGHT)
                    }
                } else {
                    applyFormatting(RichTextUtils.FormatType.HIGHLIGHT)
                }
            },
            onCheckboxClick = {
                applyFormatting(RichTextUtils.FormatType.CHECKBOX_UNCHECKED)
            },
            onUndoClick = {
                // MVP: Placeholder
            },
            onRedoClick = {
                // MVP: Placeholder
            }
        )
    }

    // Note picker for @mentions
    if (showNotePicker) {
        val notePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NotePicker(
            sheetState = notePickerSheetState,
            allNotes = allNotes,
            currentNoteId = currentNoteId,
            onDismiss = { showNotePicker = false },
            onNoteSelected = { noteId, mentionText ->
                val currentText = textFieldValue.text
                val cursorPos = textFieldValue.selection.start

                val beforeCursor = currentText.substring(0, maxOf(0, cursorPos - 1))
                val afterCursor = currentText.substring(cursorPos)

                val mentionMarkdown = "@[$mentionText](note:$noteId) "
                val newText = beforeCursor + mentionMarkdown + afterCursor

                val newCursorPos = beforeCursor.length + mentionMarkdown.length
                textFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPos)
                )

                showNotePicker = false
            }
        )
    }

    // Auto-focus when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberModalBottomSheetState(skipPartiallyExpanded: Boolean = false): SheetState {
    return androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
}

/**
 * FlomoToolbar - Minimal left-aligned toolbar with format lock indicators.
 *
 * Stage 9 Refactor: Replaces FormattingToolbar with Flomo-style minimal design.
 *
 * Features:
 * - 20dp icon size (not 24dp - more subtle)
 * - No background for icons (borderless)
 * - Selected state: circular background highlight when format is locked
 *
 * @param activeFormats Currently active format locks from ViewModel
 * @param onTagClick Callback for # button
 * @param onImageClick Callback for image button
 * @param onBoldClick Callback for bold button
 * @param onMoreClick Callback for more options button
 */
@Composable
private fun FlomoToolbar(
    activeFormats: Set<RichTextUtils.FormatType>,
    onTagClick: () -> Unit,
    onImageClick: () -> Unit,
    onBoldClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag button (#)
        MinimalIconButton(
            text = "#",
            isActive = false,
            onClick = onTagClick
        )

        // Image picker button (📷)
        MinimalIconButton(
            text = "📷",
            isActive = false,
            onClick = onImageClick
        )

        // Bold button (B) - shows active state
        MinimalIconButton(
            text = "B",
            isActive = activeFormats.contains(RichTextUtils.FormatType.BOLD),
            onClick = onBoldClick
        )

        // More options button (...)
        MinimalIconButton(
            text = "...",
            isActive = false,
            onClick = onMoreClick
        )
    }
}

/**
 * MinimalIconButton - Flomo-style minimal icon button.
 *
 * Design:
 * - 20dp text size (subtle, not prominent)
 * - No border or background by default
 * - When active (format locked): light blue circular background
 *
 * @param text Icon text (e.g., "B", "#", "📷")
 * @param isActive Whether this format is currently locked
 * @param onClick Click callback
 */
@Composable
private fun MinimalIconButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                color = if (isActive) Color(0xFFE3F2FD) else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = if (isActive) Color(0xFF4C88FF) else Color(0xFF666666)
        )
    }
}