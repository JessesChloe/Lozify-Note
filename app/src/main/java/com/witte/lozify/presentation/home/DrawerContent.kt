package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Tag
import com.witte.lozify.domain.model.UserStats
import java.time.LocalDate

/**
 * DrawerContent - Flomo-inspired side navigation drawer.
 *
 * Stage 11 Major Refactor: Pixel-perfect Flomo design replication.
 * Stage 12 Update: Added bottom system entries (Trash, Help Center), tag operations integration.
 * Stage 14 Update: Added top achievement panel and GitHub-style contribution heatmap calendar.
 *
 * Design System:
 * - Pure white background (#FFFFFF), no heavy shadows
 * - Top achievement panel: notes count, tags count, days count
 * - Contribution heatmap: 52-week punchcard activity grid
 * - Section titles: 12sp, very light warm gray (#B0B0B0), non-bold
 * - Tag text: 15sp, deep charcoal (#222222), medium weight
 * - Selected state: subtle light gray background, NO blue rounded blocks
 * - Generous padding for breathing room
 *
 * @param tags List of all available tags
 * @param selectedTag Currently selected tag (null = show all)
 * @param stats User stats for achievement panel
 * @param dailyCounts Daily note counts map for contribution heatmap
 * @param onTagSelected Callback when a tag is selected (null = show all)
 * @param onCloseDrawer Callback to close the drawer after selection
 * @param onNavigateToTrash Callback to navigate to trash screen
 * @param onEditTag Callback when user wants to edit a tag
 * @param onRemoveTag Callback when user removes tag from all notes
 * @param onDeleteTagAndNotes Callback when user deletes tag and moves notes to trash
 */
@Composable
fun DrawerContent(
    tags: List<Tag>,
    selectedTag: Tag?,
    stats: UserStats = UserStats(),
    dailyCounts: Map<LocalDate, Int> = emptyMap(),
    onTagSelected: (Long?) -> Unit,
    onCloseDrawer: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    onEditTag: (Tag) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onDeleteTagAndNotes: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header Section (with horizontal padding)
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
                DrawerHeader()
            }

            // Stage 14: Achievement Panel (个人数据中心 / 统计面板)
            DrawerStatsPanel(
                stats = stats,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = Color(0xFFF0F0F0),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stage 14: Contribution Heatmap Section (打卡日历)
            Text(
                text = "打卡日历",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            ContributionHeatmap(
                dailyCounts = dailyCounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = Color(0xFFF0F0F0),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content (tags list) - takes remaining space
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // "全部笔记" - Show All Notes (always at top, no section)
                item {
                    DrawerAllNotesItem(
                        isSelected = selectedTag == null,
                        onClick = {
                            onTagSelected(null)
                            onCloseDrawer()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // "置顶标签" Section (placeholder for future pinned tags feature)
                item {
                    DrawerSectionTitle(title = "置顶标签")
                }
                item {
                    Text(
                        text = "暂无置顶标签",
                        fontSize = 13.sp,
                        color = Color(0xFFCCCCCC),
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
                    )
                }

                // "全部标签" Section
                item {
                    DrawerSectionTitle(
                        title = "全部标签",
                        showSortIcon = true,
                        onSortClick = {
                            // TODO: Implement sort/filter logic in future
                        }
                    )
                }

                // Tag List
                items(
                    items = tags,
                    key = { it.id }
                ) { tag ->
                    DrawerTagItem(
                        tag = tag,
                        isSelected = selectedTag?.id == tag.id,
                        onClick = {
                            onTagSelected(tag.id)
                            onCloseDrawer()
                        },
                        onEditClick = {
                            onEditTag(tag)
                        },
                        onRemoveTag = {
                            onRemoveTag(tag.name)
                        },
                        onDeleteTagAndNotes = {
                            onDeleteTagAndNotes(tag.id)
                        }
                    )
                }
            }

            // Bottom System Entries (fixed at bottom)
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Trash Entry
                DrawerSystemEntry(
                    icon = Icons.Default.Delete,
                    label = "回收站",
                    onClick = {
                        onNavigateToTrash()
                        onCloseDrawer()
                    }
                )

                // Help Center Entry
                DrawerSystemEntry(
                    icon = Icons.Default.Info,
                    label = "帮助中心",
                    onClick = {
                        // TODO: Navigate to help center
                    }
                )
            }
        }
    }
}

/**
 * DrawerHeader - Minimal branding header.
 */
@Composable
private fun DrawerHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Lozify",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222)
        )
    }
}

/**
 * DrawerStatsPanel - Achievement and milestone dashboard in drawer.
 *
 * Stage 14: Personal Data Center
 * Displays:
 * 1. 笔记数 (Notes Count)
 * 2. 标签数 (Tags Count)
 * 3. 记录天数 (Days of Record)
 */
@Composable
private fun DrawerStatsPanel(
    stats: UserStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerStatItem(
            value = stats.notesCount.toString(),
            label = "条笔记",
            modifier = Modifier.weight(1f)
        )
        DrawerStatItem(
            value = stats.tagsCount.toString(),
            label = "个标签",
            modifier = Modifier.weight(1f)
        )
        DrawerStatItem(
            value = stats.daysCount.toString(),
            label = "天记录",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DrawerStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF888888),
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * DrawerSectionTitle - Section header with optional sort icon.
 */
@Composable
private fun DrawerSectionTitle(
    title: String,
    showSortIcon: Boolean = false,
    onSortClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFB0B0B0),
            letterSpacing = 0.5.sp
        )

        if (showSortIcon) {
            IconButton(
                onClick = onSortClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "排序筛选",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * DrawerAllNotesItem - "全部笔记" special menu item.
 */
@Composable
private fun DrawerAllNotesItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFF5F5F5) else Color.Transparent
    val textColor = Color(0xFF222222)
    val textWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = "全部笔记",
            fontSize = 15.sp,
            fontWeight = textWeight,
            color = textColor
        )
    }
}

/**
 * DrawerTagItem - Individual tag item with # prefix or emoji and ... menu.
 *
 * Layout (left to right):
 * 1. Black # prefix icon or emoji (if custom icon exists)
 * 2. Tag name (takes remaining space)
 * 3. Three-dot menu icon (light gray)
 *
 * Stage 12: Added emoji icon support and dangerous operation callbacks.
 */
@Composable
private fun DrawerTagItem(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onRemoveTag: () -> Unit,
    onDeleteTagAndNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val backgroundColor = if (isSelected) Color(0xFFF5F5F5) else Color.Transparent
    val textColor = Color(0xFF222222)
    val textWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: # prefix + tag name
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // # prefix icon or emoji
            Text(
                text = tag.icon ?: "#",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            // Tag name
            Text(
                text = tag.name,
                fontSize = 15.sp,
                fontWeight = textWeight,
                color = textColor
            )
        }

        // Right: ... menu icon
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "标签操作",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                // Pin/Unpin (placeholder for future feature)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "设为置顶",
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        android.widget.Toast.makeText(
                            context,
                            "置顶功能即将上线",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Edit name and icon
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "编辑名称和图标",
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Remove tag only (light red)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "仅移除标签",
                            fontSize = 14.sp,
                            color = Color(0xFFE57373)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onRemoveTag()
                    }
                )

                // Delete tag and notes (light red with confirmation dialog)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "删除标签和笔记",
                            fontSize = 14.sp,
                            color = Color(0xFFE57373)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    // Dangerous Operation Confirmation AlertDialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "确定删除该标签及相关笔记？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF222222)
                )
            },
            text = {
                Text(
                    text = "该标签将被彻底移除，且带有此标签的所有笔记都会被移入回收站。此操作不可逆！",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteTagAndNotes()
                    }
                ) {
                    Text(
                        text = "确认删除",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF666666)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * DrawerSystemEntry - Bottom system entry item (Trash, Help Center).
 */
@Composable
private fun DrawerSystemEntry(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
    }
}
