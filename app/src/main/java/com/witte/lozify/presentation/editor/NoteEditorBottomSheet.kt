package com.witte.lozify.presentation.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NoteEditorBottomSheet - Modal bottom sheet for creating/editing notes.
 *
 * Design Specs:
 * - Material3 ModalBottomSheet
 * - Multi-line text input with auto-focus
 * - Cancel button (left) and Save button (right)
 * - IME padding for keyboard handling
 *
 * @param sheetState Bottom sheet state
 * @param onDismiss Callback when sheet is dismissed
 * @param onSave Callback when save button clicked with note content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteContent by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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
                value = noteContent,
                onValueChange = { noteContent = it },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                TextButton(
                    onClick = {
                        noteContent = ""
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
                        if (noteContent.isNotBlank()) {
                            onSave(noteContent.trim())
                            noteContent = ""
                            onDismiss()
                        }
                    },
                    enabled = noteContent.isNotBlank(),
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

    // Auto-focus when sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
