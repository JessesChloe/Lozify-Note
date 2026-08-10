package com.witte.lozify.presentation.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                icon = Icons.Default.FormatUnderlined,
                label = "下划线",
                description = "为选中文字添加下划线",
                onClick = {
                    onUnderlineClick()
                    onDismiss()
                }
            )

            // Highlight option
            FormattingMenuItem(
                icon = Icons.Default.Highlight,
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
                icon = Icons.Default.Undo,
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
                icon = Icons.Default.Redo,
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
 * @param icon Menu item icon
 * @param label Primary label text
 * @param description Secondary description text
 * @param onClick Click callback
 * @param enabled Whether the item is clickable
 */
@Composable
private fun FormattingMenuItem(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) androidx.compose.ui.graphics.Color.Unspecified
                   else androidx.compose.ui.graphics.Color.Gray
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (enabled) androidx.compose.ui.graphics.Color.Unspecified
                        else androidx.compose.ui.graphics.Color.Gray
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}
