package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Tag

/**
 * TagFilterBar - Horizontal scrollable tag filter component.
 *
 * Stage 4: Allows users to filter notes by clicking on tags.
 *
 * Features:
 * - Horizontal scrolling LazyRow of tag chips
 * - "全部" (All) chip to clear filter
 * - Selected tag highlighted in blue
 * - Unselected tags in light gray
 * - Displays tag name and usage count
 *
 * @param tags List of all available tags
 * @param selectedTag Currently selected tag (null = show all)
 * @param onTagSelected Callback when a tag is selected (null = show all)
 */
@Composable
fun TagFilterBar(
    tags: List<Tag>,
    selectedTag: Tag?,
    onTagSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // "全部" chip to show all notes
        item {
            TagChip(
                label = "全部",
                count = null,
                isSelected = selectedTag == null,
                onClick = { onTagSelected(null) }
            )
        }

        // Individual tag chips
        items(
            items = tags,
            key = { it.id }
        ) { tag ->
            TagChip(
                label = tag.name,
                count = tag.usageCount,
                isSelected = selectedTag?.id == tag.id,
                onClick = { onTagSelected(tag.id) }
            )
        }
    }
}

/**
 * TagChip - Individual tag chip component.
 *
 * @param label Tag name to display
 * @param count Optional usage count to display
 * @param isSelected Whether this chip is currently selected
 * @param onClick Callback when chip is clicked
 */
@Composable
private fun TagChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF4C88FF) else Color(0xFFF0F0F0)
    val textColor = if (isSelected) Color.White else Color(0xFF666666)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count != null && count > 0) {
                "#$label ($count)"
            } else {
                if (label == "全部") label else "#$label"
            },
            fontSize = 14.sp,
            color = textColor
        )
    }
}
