package com.witte.lozify.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * NoteEditorBottomSheet - Modal bottom sheet for creating/editing notes.
 *
 * Design Specs:
 * - Material3 ModalBottomSheet
 * - Multi-line text input with auto-focus
 * - Cancel button (left) and Save button (right)
 * - IME padding for keyboard handling
 *
 * Stage 5: Added initialContent parameter for editing existing notes.
 * Stage 6: Added image picker button and thumbnail preview with remove functionality.
 * Stage 7: Added formatting toolbar and formatting menu for rich text editing.
 *
 * @param sheetState Bottom sheet state
 * @param onDismiss Callback when sheet is dismissed
 * @param onSave Callback when save button clicked with note content and image URIs
 * @param initialContent Optional initial content for editing (null for new note)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (String, List<Uri>) -> Unit,
    initialContent: String? = null,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(initialContent) {
        mutableStateOf(TextFieldValue(text = initialContent ?: ""))
    }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showFormattingMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Stage 6: Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUris = selectedImageUris + it
        }
    }

    // Stage 7: Helper function to apply formatting with bounds checking
    // Bug Fix: Removed overly aggressive validation that blocked multiline formatting
    fun applyFormatting(formatType: RichTextUtils.FormatType) {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection

        // Basic bounds validation only
        if (selection.start < 0 || selection.end > currentText.length || selection.start > selection.end) {
            return
        }

        val newText = RichTextUtils.insertFormatting(
            content = currentText,
            selectionStart = selection.start,
            selectionEnd = selection.end,
            formatType = formatType
        )

        // Calculate new cursor position after marker insertion
        val markerLength = when (formatType) {
            RichTextUtils.FormatType.BOLD,
            RichTextUtils.FormatType.UNDERLINE,
            RichTextUtils.FormatType.HIGHLIGHT -> 2 // ** or __ or ==
            RichTextUtils.FormatType.CHECKBOX_UNCHECKED -> 6 // "- [ ] "
            RichTextUtils.FormatType.CHECKBOX_CHECKED -> 6 // "- [x] "
        }

        val newCursorPos = if (selection.start == selection.end) {
            // No selection: place cursor between markers
            selection.start + markerLength
        } else {
            // Has selection: place cursor after closing marker
            selection.end + markerLength * 2
        }

        // Ensure cursor position is within bounds
        val safeCursorPos = newCursorPos.coerceIn(0, newText.length)

        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(safeCursorPos)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Text input field
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                placeholder = {
                    Text(
                        text = "记录此刻的想法...",
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
                    .height(200.dp)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stage 7: Formatting toolbar
            FormattingToolbar(
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
                    applyFormatting(RichTextUtils.FormatType.BOLD)
                },
                onCheckboxClick = {
                    applyFormatting(RichTextUtils.FormatType.CHECKBOX_UNCHECKED)
                },
                onMoreClick = {
                    showFormattingMenu = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stage 6: Image thumbnail preview
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

                            // Remove button
                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris.filter { it != uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                TextButton(
                    onClick = {
                        textFieldValue = TextFieldValue("")
                        selectedImageUris = emptyList()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF666666),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Save button
                Button(
                    onClick = {
                        if (textFieldValue.text.isNotBlank()) {
                            onSave(textFieldValue.text.trim(), selectedImageUris)
                            textFieldValue = TextFieldValue("")
                            selectedImageUris = emptyList()
                            onDismiss()
                        }
                    },
                    enabled = textFieldValue.text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCCCCCC),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(
                        text = "保存",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Stage 7: Formatting menu bottom sheet
    if (showFormattingMenu) {
        FormattingMenu(
            onDismiss = { showFormattingMenu = false },
            onUnderlineClick = {
                applyFormatting(RichTextUtils.FormatType.UNDERLINE)
            },
            onHighlightClick = {
                applyFormatting(RichTextUtils.FormatType.HIGHLIGHT)
            },
            onUndoClick = {
                // MVP: Placeholder
            },
            onRedoClick = {
                // MVP: Placeholder
            }
        )
    }

    // Auto-focus when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
