package com.witte.lozify.presentation.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Tag

/**
 * TagPicker - 1:1 Flomo-styled floating tag suggestion popover.
 *
 * Stage 41: Real-time #tag autocompletion popover.
 * Appears when user inputs '#' in the editor, providing a smooth list of existing active tags.
 *
 * @param availableTags List of active tags to select from
 * @param tagQuery Current search filter typed after '#' (e.g. "ta" for "#ta")
 * @param onTagSelected Callback when a tag is clicked
 * @param onDismiss Callback when dismissed
 */
@Composable
fun TagPicker(
    availableTags: List<Tag>,
    tagQuery: String,
    onTagSelected: (Tag) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter tags in real-time by tagQuery
    val filteredTags = remember(availableTags, tagQuery) {
        if (tagQuery.isBlank()) {
            availableTags
        } else {
            availableTags.filter { it.name.contains(tagQuery, ignoreCase = true) }
        }
    }

    if (filteredTags.isEmpty()) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(0.6.dp, Color(0xFFEBEBEB))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(filteredTags, key = { it.id }) { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "# ${tag.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222),
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
