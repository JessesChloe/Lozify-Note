package com.witte.lozify.presentation.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Tag

/**
 * TagEditScreen - Edit tag name and icon (Stage 12).
 *
 * Pixel-perfect replication of Flomo's tag edit interface:
 * - Top navigation: X (close) | "编辑名称和图标" | ✓ (save, green)
 * - Center visual focus: Large # or emoji icon (60sp)
 * - Rounded gray TextField for tag name input
 * - Emoji picker grid: "常用" and "表情 & 人物" sections
 * - Click emoji to replace the # icon
 *
 * @param tag Tag being edited
 * @param onNavigateBack Callback to navigate back
 * @param onSave Callback when user saves (receives newName, newIcon)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditScreen(
    tag: Tag,
    onNavigateBack: () -> Unit,
    onSave: (newName: String, newIcon: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var tagName by remember { mutableStateOf(tag.name) }
    var selectedIcon by remember { mutableStateOf<String?>(tag.icon) } // null = use # symbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "编辑名称和图标",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF999999)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSave(tagName, selectedIcon)
                            onNavigateBack()
                        },
                        enabled = tagName.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存",
                            tint = if (tagName.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFCCCCCC)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Large icon display (# or emoji)
            Text(
                text = selectedIcon ?: "#",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tag name input field
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                placeholder = {
                    Text(
                        text = "输入标签名称",
                        color = Color(0xFFCCCCCC)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Emoji picker sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Section: 常用 (Common)
                Text(
                    text = "常用",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(COMMON_EMOJIS) { emoji ->
                        EmojiItem(
                            emoji = emoji,
                            isSelected = selectedIcon == emoji,
                            onClick = { selectedIcon = emoji }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: 表情 & 人物 (Emoji & People)
                Text(
                    text = "表情 & 人物",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EMOJI_PEOPLE) { emoji ->
                        EmojiItem(
                            emoji = emoji,
                            isSelected = selectedIcon == emoji,
                            onClick = { selectedIcon = emoji }
                        )
                    }
                }
            }
        }
    }
}

/**
 * EmojiItem - Single emoji grid item.
 */
@Composable
private fun EmojiItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
    }
}

// Hardcoded emoji lists (Stage 12 MVP)
private val COMMON_EMOJIS = listOf(
    "📌", "💡", "📝", "📅", "✅", "⭐",
    "❤️", "🔥", "💯", "🎯", "📚", "🏆"
)

private val EMOJI_PEOPLE = listOf(
    "😀", "😂", "😊", "😍", "😎", "🤔",
    "😅", "😇", "🥰", "😘", "😋", "🤗",
    "🙂", "😉", "😌", "😏", "🥳", "😴"
)
