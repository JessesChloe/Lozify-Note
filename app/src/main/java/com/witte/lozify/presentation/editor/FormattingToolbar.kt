package com.witte.lozify.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FormattingToolbar - Bottom toolbar for rich text editing.
 *
 * Stage 7: Provides quick access to formatting actions.
 *
 * Toolbar Buttons (left to right):
 * 1. # - Insert tag prefix (Text button)
 * 2. 📷 - Image picker (Icon)
 * 3. B - Bold formatting (Text button)
 * 4. ☐ - Insert checkbox (Text button)
 * 5. ... - More options menu (Icon)
 *
 * Note: Using Text buttons instead of extended icons to avoid dependency bloat.
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
            // Tag button (#)
            TextButton(onClick = onTagClick) {
                Text(
                    text = "#",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
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

            // Bold button (B)
            TextButton(onClick = onBoldClick) {
                Text(
                    text = "B",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Checkbox button (☐)
            TextButton(onClick = onCheckboxClick) {
                Text(
                    text = "☐",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
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
