package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.presentation.editor.EditorViewModel
import com.witte.lozify.presentation.editor.NoteEditorBottomSheet
import com.witte.lozify.domain.model.NoteRelation
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * HomeScreen - Main feed displaying note cards in waterfall layout.
 *
 * Stage 3: Real database integration with Room + ViewModel.
 * Stage 4: Added tag filtering with horizontal scrollable tag bar.
 * Stage 5: Added real-time search, pin/edit/delete operations.
 * Stage 5 Refactor: Replaced horizontal TagFilterBar with ModalNavigationDrawer.
 * Stage 12: Renamed archive to trash, added tag edit navigation.
 * Displays empty state when no notes, otherwise shows note list.
 * Includes editor bottom sheet for creating/editing notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTrash: () -> Unit = {},
    onNavigateToTagEdit: (Long) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    editorViewModel: EditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Stage 5 Refactor: Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Stage 5: Search state
    var isSearchActive by remember { mutableStateOf(false) }

    // Stage 8: Highlight state for @mention navigation
    var highlightedNoteId by remember { mutableStateOf<Long?>(null) }

    // Stage 8 UX Refactor: Note detail dialog state (replaces relations dialog)
    var showNoteDetail by remember { mutableStateOf<Long?>(null) }

    // Editor bottom sheet state
    var showEditor by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteContent by remember { mutableStateOf<String?>(null) }
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Listen for editor events
    LaunchedEffect(Unit) {
        editorViewModel.events.collect { event ->
            when (event) {
                is EditorViewModel.EditorEvent.NoteSaved -> {
                    val message = if (event.tagCount > 0) {
                        "笔记已保存 (${event.tagCount} 个标签)"
                    } else {
                        "笔记已保存"
                    }
                    snackbarHostState.showSnackbar(message)
                    // Stage 8: Auto-scroll to top after saving new note
                    // Add small delay to wait for Room Flow to update UI
                    if (editingNoteId == null) {
                        scope.launch {
                            kotlinx.coroutines.delay(150)
                            listState.animateScrollToItem(0)
                        }
                    }
                }
                is EditorViewModel.EditorEvent.SaveError -> {
                    snackbarHostState.showSnackbar("保存失败: ${event.message}")
                }
            }
        }
    }

    // Stage 8: Auto-clear highlight after 3 seconds
    LaunchedEffect(highlightedNoteId) {
        highlightedNoteId?.let {
            kotlinx.coroutines.delay(3000)
            highlightedNoteId = null
        }
    }

    // Stage 5 Refactor: Wrap Scaffold with ModalNavigationDrawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                tags = uiState.allTags,
                selectedTag = uiState.selectedTag,
                stats = uiState.userStats,
                dailyCounts = uiState.heatmapData,
                onTagSelected = { tagId ->
                    homeViewModel.selectTag(tagId)
                },
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNavigateToTrash = onNavigateToTrash,
                onEditTag = { tag ->
                    onNavigateToTagEdit(tag.id)
                },
                onRemoveTag = { tagName ->
                    homeViewModel.removeTagFromAllNotes(tagName)
                },
                onDeleteTagAndNotes = { tagId ->
                    homeViewModel.deleteTagAndMoveNotesToTrash(tagId)
                }
            )
        }
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Stage 5: Toggle between title and search input
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { homeViewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "搜索笔记内容...",
                                    fontSize = 16.sp,
                                    color = Color(0xFF999999)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Lozify",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    // Stage 5 Refactor: Hamburger menu icon to open drawer
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "菜单"
                        )
                    }
                },
                actions = {
                    // Stage 5: Search toggle button
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            homeViewModel.clearSearch()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "关闭搜索" else "搜索"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF333333),
                    navigationIconContentColor = Color(0xFF666666),
                    actionIconContentColor = Color(0xFF666666)
                )
            )
        },
        floatingActionButton = {
            LozifyFloatingActionButton(
                onClick = {
                    editingNoteId = null
                    editingNoteContent = null
                    showEditor = true
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F8FA),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stage 5 Refactor: Removed TagFilterBar (replaced by drawer)

            Spacer(modifier = Modifier.height(8.dp))

            // Content area: loading, empty, or note list
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.notes.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(uiState.notes, key = { _, note -> note.id }) { index, note ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            // Right swipe: Toggle pin
                                            homeViewModel.togglePin(note.id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (note.isPinned) "已取消置顶" else "已置顶"
                                                )
                                            }
                                            false // Don't dismiss, reset state
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            // Left swipe: Move to trash (renamed from archive in Stage 12)
                                            homeViewModel.moveToTrash(note.id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("已移至回收站")
                                            }
                                            false // Don't dismiss, reset state
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                },
                                positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF9800) // Orange for pin
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFE57373) // Red for archive
                                        else -> Color.Transparent
                                    }
                                    val icon = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Star
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                        else -> null
                                    }
                                    val alignment = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = alignment
                                    ) {
                                        icon?.let {
                                            Icon(
                                                imageVector = it,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            ) {
                                NoteCard(
                                    noteId = note.id,
                                    content = note.content,
                                    timestamp = formatTimestamp(note.createdAt),
                                    isPinned = note.isPinned,
                                    attachments = note.attachments,
                                    outgoingRelationsCount = note.outgoingRelations.size,
                                    incomingRelationsCount = note.incomingRelations.size,
                                    isHighlighted = note.id == highlightedNoteId,
                                    filesDir = androidx.compose.ui.platform.LocalContext.current.filesDir,
                                    onTogglePinClick = {
                                        homeViewModel.togglePinStatus(note.id, note.isPinned)
                                    },
                                    onEditClick = {
                                        // Stage 5: Open editor with existing content
                                        editingNoteId = note.id
                                        editingNoteContent = note.content
                                        showEditor = true
                                    },
                                    onDeleteClick = {
                                        // Stage 5: Delete note with confirmation
                                        homeViewModel.deleteNote(note.id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("笔记已删除")
                                        }
                                    },
                                    onCheckboxToggle = { noteId, newContent ->
                                        // Stage 7 Bug Fix: Update note content when checkbox is toggled
                                        homeViewModel.updateNoteContent(noteId, newContent)
                                    },
                                    onTagClick = { tagName ->
                                        // Stage 4: Filter by clicked tag
                                        val tag = uiState.allTags.find { it.name == tagName }
                                        tag?.let { homeViewModel.selectTag(it.id) }
                                    },
                                    onMentionClick = { mentionedNoteId ->
                                        // Stage 8 UX Refactor: Show note detail instead of scrolling
                                        showNoteDetail = mentionedNoteId
                                    },
                                    onRelationsClick = { relations: List<NoteRelation>, title: String ->
                                        // Stage 8 UX Refactor: Show first relation's note detail
                                        if (relations.isNotEmpty()) {
                                            val targetNoteId = if (title == "出链列表") relations.first().toNoteId else relations.first().fromNoteId
                                            showNoteDetail = targetNoteId
                                        }
                                    },
                                    outgoingRelations = note.outgoingRelations,
                                    incomingRelations = note.incomingRelations
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Editor bottom sheet
    if (showEditor) {
        NoteEditorBottomSheet(
            sheetState = editorSheetState,
            viewModel = editorViewModel,
            onDismiss = {
                showEditor = false
                editingNoteId = null
                editingNoteContent = null
            },
            onSave = { textFieldValue, imageUris ->
                // Stage 5: Support both create and update
                // Stage 6: Pass image URIs to ViewModel
                // Stage 9 Refactor: Now receives TextFieldValue instead of String
                editorViewModel.saveNote(textFieldValue, imageUris, editingNoteId)
            },
            initialContent = editingNoteContent,
            allNotes = uiState.notes,
            currentNoteId = editingNoteId ?: 0L
        )
    }

    // Stage 13 Refactor: Note detail context thread bottom sheet
    showNoteDetail?.let { noteId ->
        NoteDetailBottomSheet(
            noteId = noteId,
            filesDir = androidx.compose.ui.platform.LocalContext.current.filesDir,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = { showNoteDetail = null },
            onSelectNote = { selectedId ->
                showNoteDetail = selectedId
            }
        )
    }
    }
}

/**
 * Format timestamp to relative time string.
 */
private fun formatTimestamp(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}分钟前"
        duration.toHours() < 24 -> "${duration.toHours()}小时前"
        duration.toDays() < 7 -> "${duration.toDays()}天前"
        else -> {
            val days = duration.toDays()
            when {
                days < 30 -> "${days / 7}周前"
                days < 365 -> "${days / 30}个月前"
                else -> "${days / 365}年前"
            }
        }
    }
}
