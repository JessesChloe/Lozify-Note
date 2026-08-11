package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Tag

/**
 * DrawerContent - Side navigation drawer content for tag filtering.
 *
 * Stage 5 Refactor: Replaced horizontal TagFilterBar with vertical drawer menu.
 * Bug Fix: Uses Material3 ModalDrawerSheet for proper width and rounded corners.
 *
 * Features:
 * - Material3 standard ModalDrawerSheet (auto width constraint + right rounded corners)
 * - Header with app branding ("Lozify")
 * - "全部笔记" global menu item to clear filters
 * - Vertical scrollable tag list with usage counts
 * - Selected tag highlighted in blue background
 * - Click interaction auto-closes drawer
 * - WindowInsets padding for status bar safety
 *
 * @param tags List of all available tags
 * @param selectedTag Currently selected tag (null = show all)
 * @param onTagSelected Callback when a tag is selected (null = show all)
 * @param onCloseDrawer Callback to close the drawer after selection
 */
@Composable
fun DrawerContent(
    tags: List<Tag>,
    selectedTag: Tag?,
    onTagSelected: (Long?) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header Section
            DrawerHeader()

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // Menu Items
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // "全部笔记" - Show All Notes
                item {
                    DrawerMenuItem(
                        label = "全部笔记",
                        count = null,
                        isSelected = selectedTag == null,
                        onClick = {
                            onTagSelected(null)
                            onCloseDrawer()
                        }
                    )
                }

                // Tag List
                items(
                    items = tags,
                    key = { it.id }
                ) { tag ->
                    DrawerMenuItem(
                        label = "#${tag.name}",
                        count = tag.usageCount,
                        isSelected = selectedTag?.id == tag.id,
                        onClick = {
                            onTagSelected(tag.id)
                            onCloseDrawer()
                        }
                    )
                }
            }
        }
    }
}

/**
 * DrawerHeader - Branding header at the top of drawer.
 */
@Composable
private fun DrawerHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Lozify",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

/**
 * DrawerMenuItem - Individual menu item in the drawer.
 *
 * @param label Item label (e.g., "全部笔记", "#工作")
 * @param count Optional usage count to display
 * @param isSelected Whether this item is currently selected
 * @param onClick Callback when item is clicked
 */
@Composable
private fun DrawerMenuItem(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF4C88FF) else Color(0xFF333333)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
        }
    }
}
