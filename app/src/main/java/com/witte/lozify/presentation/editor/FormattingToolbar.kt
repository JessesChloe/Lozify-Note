package com.witte.lozify.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * FormattingToolbar - Bottom toolbar for rich text editing.
 *
 * Stage 7: Provides quick access to formatting actions.
 *
 * Toolbar Buttons (left to right):
 * 1. # - Insert tag prefix
 * 2. 📷 - Image picker
 * 3. B - Bold formatting
 * 4. ☐ - Insert checkbox
 * 5. ... - More options menu (underline, highlight)
 *
 * @param onTagClick Callback when tag button is clicked
 * @param onImageClick Callback when image button is clicked
 * @param onBoldClick Callback when bold button is clicked
 * @param onCheckboxClick Callback when checkbox button is clicked
 * @param onMoreClick Callback when more options button is clicked
 * @param modifier Modifier for custom styling
 */
@Composable
fun FormattingToolbar(
    onTagClick: () -> Unit,
    onImageClick: () -> Unit,
    onBoldClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag button
            IconButton(onClick = onTagClick) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = "插入标签",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            // Image picker button
            IconButton(onClick = onImageClick) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "添加图片",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Bold button
            IconButton(onClick = onBoldClick) {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = "粗体",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Checkbox button
            IconButton(onClick = onCheckboxClick) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = "插入复选框",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // More options button
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "更多选项",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
