package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextLayoutResult

/**
 * NoteCard component for displaying a single note with expand/collapse functionality.
 *
 * Features:
 * - White background with 12dp rounded corners
 * - Timestamp display in gray
 * - More options menu icon
 * - Auto-collapse for content exceeding 5 lines
 * - Blue "展开" clickable text to expand content
 *
 * @param content The note content text
 * @param timestamp Display timestamp (e.g., "2分钟前")
 * @param onMoreClick Callback when more options icon is clicked
 */
@Composable
fun NoteCard(
    content: String,
    timestamp: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showExpandButton by remember { mutableStateOf(false) }
    val maxCollapsedLines = 5

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Timestamp + More Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timestamp,
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_more),
                        contentDescription = "更多操作",
                        tint = Color(0xFF999999)
                    )
                }
            }

            // Content with expand/collapse logic
            Column {
                Text(
                    text = content,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    lineHeight = 24.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else maxCollapsedLines,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                            showExpandButton = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Show "展开" link only when content has overflow and not expanded
                if (!isExpanded && showExpandButton) {
                    Text(
                        text = "展开",
                        fontSize = 14.sp,
                        color = Color(0xFF4C88FF),
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { isExpanded = true }
                    )
                }
            }
        }
    }
}
