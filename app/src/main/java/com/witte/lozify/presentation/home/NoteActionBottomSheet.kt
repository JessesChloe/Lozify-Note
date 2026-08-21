package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Note

/**
 * NoteActionBottomSheet - Flomo-style long press action menu for note cards.
 * Provides quick actions: Edit, Share (Generate Card), Copy, Delete, and Cancel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 1. 编辑
            ActionMenuItem(
                text = "编辑",
                textColor = Color(0xFF333333),
                onClick = {
                    onDismiss()
                    onEdit()
                }
            )
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.8.dp)

            // 2. 分享
            ActionMenuItem(
                text = "分享",
                textColor = Color(0xFF333333),
                onClick = {
                    onDismiss()
                    onShare()
                }
            )
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.8.dp)

            // 3. 复制
            ActionMenuItem(
                text = "复制",
                textColor = Color(0xFF333333),
                onClick = {
                    onDismiss()
                    onCopy()
                }
            )
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.8.dp)

            // 4. 删除
            ActionMenuItem(
                text = "删除",
                textColor = Color(0xFFFF4D4F),
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )

            // 间距分隔条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFF7F8FA))
            )

            // 5. 取消
            ActionMenuItem(
                text = "取消",
                textColor = Color(0xFF666666),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun ActionMenuItem(
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
        )
    }
}
