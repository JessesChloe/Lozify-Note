package com.witte.lozify.presentation.home

import com.witte.lozify.presentation.components.LozifyLogo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
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
 * TagSortOrder - Sorting criteria for sidebar tag list.
 */
enum class TagSortOrder(val title: String) {
    USAGE_DESC("使用频次 (最多优先)"),
    NAME_ASC("名称字母 (A-Z)"),
    RECENT("最新创建")
}

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
    pinnedTags: List<Tag> = emptyList(),
    selectedTag: Tag?,
    stats: UserStats = UserStats(),
    dailyCounts: Map<LocalDate, Int> = emptyMap(),
    onTagSelected: (Long?) -> Unit,
    onCloseDrawer: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onTogglePinTag: (Long, Boolean) -> Unit = { _, _ -> },
    onEditTag: (Tag) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onDeleteTagAndNotes: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tagSearchQuery by remember { mutableStateOf("") }
    var isTagSearchVisible by remember { mutableStateOf(false) }
    var currentSortOrder by remember { mutableStateOf(TagSortOrder.USAGE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredAndSortedTags = remember(tags, tagSearchQuery, currentSortOrder) {
        val trimmed = tagSearchQuery.trim()
        val filtered = if (trimmed.isEmpty()) tags else tags.filter { it.name.contains(trimmed, ignoreCase = true) }
        when (currentSortOrder) {
            TagSortOrder.USAGE_DESC -> filtered.sortedWith(compareByDescending<Tag> { it.usageCount }.thenBy { it.name })
            TagSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            TagSortOrder.RECENT -> filtered.sortedByDescending { it.id }
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 310.dp)
            .fillMaxWidth(0.82f),
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header Section (with horizontal padding)
            Box(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
                DrawerHeader(
                    onNavigateToSettings = onNavigateToSettings,
                    onCloseDrawer = onCloseDrawer
                )
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

                // "置顶标签" Section
                item {
                    DrawerSectionTitle(title = "置顶标签")
                }
                if (pinnedTags.isEmpty()) {
                    item {
                        Text(
                            text = "暂无置顶标签",
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = pinnedTags,
                        key = { "pinned_${it.id}" }
                    ) { tag ->
                        DrawerTagItem(
                            tag = tag,
                            isSelected = selectedTag?.id == tag.id,
                            onClick = {
                                onTagSelected(tag.id)
                                onCloseDrawer()
                            },
                            onTogglePin = { isPinned ->
                                onTogglePinTag(tag.id, isPinned)
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
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // "全部标签" Section with Search and Sort Controls
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全部标签",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFB0B0B0),
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search toggle button
                            IconButton(
                                onClick = {
                                    isTagSearchVisible = !isTagSearchVisible
                                    if (!isTagSearchVisible) {
                                        tagSearchQuery = ""
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "搜索标签",
                                    tint = if (isTagSearchVisible || tagSearchQuery.isNotEmpty()) Color(0xFF1A73E8) else Color(0xFFCCCCCC),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Sort menu button
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "排序筛选",
                                        tint = if (currentSortOrder != TagSortOrder.USAGE_DESC) Color(0xFF1A73E8) else Color(0xFFCCCCCC),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    TagSortOrder.values().forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = order.title,
                                                    fontSize = 13.sp,
                                                    color = if (currentSortOrder == order) Color(0xFF1A73E8) else Color(0xFF333333),
                                                    fontWeight = if (currentSortOrder == order) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            trailingIcon = {
                                                if (currentSortOrder == order) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "已选中",
                                                        tint = Color(0xFF1A73E8),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                currentSortOrder = order
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tag Search Input Bar (when visible)
                if (isTagSearchVisible) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F6F8))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = tagSearchQuery,
                                onValueChange = { tagSearchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF222222)
                                ),
                                decorationBox = { innerTextField ->
                                    if (tagSearchQuery.isEmpty()) {
                                        Text(
                                            text = "过滤标签...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF999999)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (tagSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { tagSearchQuery = "" },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清空",
                                        tint = Color(0xFF999999),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tag List (Filtered and Sorted)
                if (filteredAndSortedTags.isEmpty()) {
                    item {
                        Text(
                            text = if (tagSearchQuery.isNotBlank()) "未找到相关标签" else "暂无标签",
                            fontSize = 13.sp,
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)
                        )
                    }
                } else {
                    items(
                        items = filteredAndSortedTags,
                        key = { it.id }
                    ) { tag ->
                        DrawerTagItem(
                            tag = tag,
                            isSelected = selectedTag?.id == tag.id,
                            onClick = {
                                onTagSelected(tag.id)
                                onCloseDrawer()
                            },
                            onTogglePin = { isPinned ->
                                onTogglePinTag(tag.id, isPinned)
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
                        onNavigateToHelp()
                        onCloseDrawer()
                    }
                )
            }
        }
    }
}

/**
 * DrawerHeader - Minimal branding header with settings gear on top right.
 */
@Composable
private fun DrawerHeader(
    onNavigateToSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LozifyLogo(sizeDp = 26.dp)

        IconButton(
            onClick = {
                onNavigateToSettings()
                onCloseDrawer()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "设置",
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2C2C2E)
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9E9E9E),
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
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * DrawerAllNotesItem - "全部笔记" Flomo-styled highlighted card.
 */
@Composable
private fun DrawerAllNotesItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF00C853) else Color(0xFFF7F8FA)
    val textColor = if (isSelected) Color.White else Color(0xFF333333)
    val iconColor = if (isSelected) Color.White else Color(0xFF666666)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = "全部笔记",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = iconColor.copy(alpha = 0.8f),
            modifier = Modifier.size(12.dp)
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
    onTogglePin: (Boolean) -> Unit = {},
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
        // Left: # prefix + tag name + optional pin badge
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

            if (tag.isPinned) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "已置顶",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(12.dp)
                )
            }
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
                // Pin/Unpin
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (tag.isPinned) "取消置顶" else "设为置顶",
                            tint = if (tag.isPinned) Color(0xFFFF9800) else Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = if (tag.isPinned) "取消置顶" else "设为置顶",
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onTogglePin(!tag.isPinned)
                    }
                )

                // Edit name and icon
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑名称和图标",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    },
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
