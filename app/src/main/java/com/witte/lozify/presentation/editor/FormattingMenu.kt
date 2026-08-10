package com.witte.lozify.presentation.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FormattingMenu - More options menu for advanced formatting.
 *
 * Stage 7: Bottom sheet with additional formatting actions.
 *
 * Menu Items:
 * - Underline: Apply __text__ formatting
 * - Highlight: Apply ==text== formatting (yellow background)
 * - Undo: Revert last change (MVP: placeholder)
 * - Redo: Reapply undone change (MVP: placeholder)
 *
 * Note: Using Text icons instead of extended material icons to avoid dependency bloat.
 *
 * @param onDismiss Callback when menu is dismissed
 * @param onUnderlineClick Callback when underline is selected
 * @param onHighlightClick Callback when highlight is selected
 * @param onUndoClick Callback when undo is selected
 * @param onRedoClick Callback when redo is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormattingMenu(
    onDismiss: () -> Unit,
    onUnderlineClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "格式化选项",
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            HorizontalDivider()

            // Underline option
            FormattingMenuItem(
                iconText = "U",
                label = "下划线",
                description = "为选中文字添加下划线",
                onClick = {
                    onUnderlineClick()
                    onDismiss()
                }
            )

            // Highlight option
            FormattingMenuItem(
                iconText = "H",
                label = "高亮",
                description = "添加黄色背景高亮",
                onClick = {
                    onHighlightClick()
                    onDismiss()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Undo option (MVP: placeholder)
            FormattingMenuItem(
                iconText = "↶",
                label = "撤销",
                description = "正在开发中",
                onClick = {
                    onUndoClick()
                    onDismiss()
                },
                enabled = false
            )

            // Redo option (MVP: placeholder)
            FormattingMenuItem(
                iconText = "↷",
                label = "重做",
                description = "正在开发中",
                onClick = {
                    onRedoClick()
                    onDismiss()
                },
                enabled = false
            )
        }
    }
}

/**
 * FormattingMenuItem - Single item in formatting menu.
 *
 * @param iconText Text to display as icon (e.g., "U" for underline)
 * @param label Primary label text
 * @param description Secondary description text
 * @param onClick Click callback
 * @param enabled Whether the item is clickable
 */
@Composable
private fun FormattingMenuItem(
    iconText: String,
    label: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text icon (replaces Material Icon)
        Text(
            text = iconText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.Black else Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (enabled) Color.Unspecified else Color.Gray
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
