package com.witte.lozify.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Note

/**
 * NotePicker - Inline card for selecting a note to @mention.
 *
 * Stage 8: @Relations feature - allows users to link notes together.
 * Stage 9 Fix: Converted from nested ModalBottomSheet to inline Card to fix display issues.
 *
 * Features:
 * - Inline display (not a nested bottom sheet)
 * - Search bar for filtering notes by content
 * - Scrollable list of all available notes (excludes current note)
 * - Displays note preview (first line or 50 chars)
 * - Returns selected note ID and mention text via callback
 *
 * @param allNotes List of all available notes
 * @param currentNoteId ID of the note being edited (excluded from list)
 * @param onDismiss Callback when close button clicked
 * @param onNoteSelected Callback when a note is selected (noteId, mentionText)
 */
@Composable
fun NotePicker(
    allNotes: List<Note>,
    currentNoteId: Long,
    onDismiss: () -> Unit,
    onNoteSelected: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter notes: exclude current note and apply search
    val filteredNotes = remember(allNotes, currentNoteId, searchQuery) {
        allNotes
            .filter { it.id != currentNoteId } // Prevent self-reference
            .filter { note ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    note.content.contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedByDescending { it.updatedAt } // Most recent first
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择要关联的笔记",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "搜索笔记内容...",
                        color = Color(0xFF999999),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Color(0xFF999999)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除",
                                tint = Color(0xFF999999)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Notes list
            if (filteredNotes.isEmpty()) {
                // Empty state
                Text(
                    text = if (searchQuery.isBlank()) "暂无其他笔记" else "未找到匹配的笔记",
                    color = Color(0xFF999999),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(filteredNotes) { note ->
                        NotePickerItem(
                            note = note,
                            onClick = {
                                // Use clean summary as mention text (max 25 chars, no markdown or brackets)
                                val mentionText = note.getCleanSummary(25)
                                onNoteSelected(note.id, mentionText)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single note item in the picker list.
 */
@Composable
private fun NotePickerItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        // Clean single-line title preview
        Text(
            text = note.getCleanSummary(30),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Clean full content preview (gray, stripped of markdown tags and brackets)
        Text(
            text = note.getCleanSummary(80),
            fontSize = 13.sp,
            color = Color(0xFF999999),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Metadata row (tags count, relations count)
        if (note.hasTags() || note.outgoingRelations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                if (note.hasTags()) {
                    Text(
                        text = "${note.tags.size} 个标签",
                        fontSize = 11.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
                if (note.hasTags() && note.outgoingRelations.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (note.outgoingRelations.isNotEmpty()) {
                    Text(
                        text = "${note.outgoingRelations.size} 个关联",
                        fontSize = 11.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
        }
    }
}
