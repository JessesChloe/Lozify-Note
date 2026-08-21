package com.witte.lozify.presentation.home

import com.witte.lozify.domain.model.Note
import com.witte.lozify.presentation.components.LozifyLogo
import com.witte.lozify.presentation.components.ImageLightboxDialog
import java.io.File
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    onNavigateToHelp: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPro: () -> Unit = {},
    onNavigateToDailyReview: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    editorViewModel: EditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val appUpdateInfo by homeViewModel.appUpdateInfo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAppLockEnabled by homeViewModel.isAppLockEnabled.collectAsState()
    val appLockPin by homeViewModel.appLockPin.collectAsState()
    val isBiometricEnabled by homeViewModel.isBiometricEnabled.collectAsState()

    var isLocked by remember {
        mutableStateOf(isAppLockEnabled && appLockPin.isNotEmpty())
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isAppLockEnabled, appLockPin) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                if (isAppLockEnabled && appLockPin.isNotEmpty()) {
                    isLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Stage 31: Silent on-launch update check
    LaunchedEffect(Unit) {
        homeViewModel.checkForUpdateOnLaunch()
    }

    // Stage 5 Refactor: Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Stage 5: Search state
    var isSearchActive by remember { mutableStateOf(false) }

    // Stage 8: Highlight state for @mention navigation
    var highlightedNoteId by remember { mutableStateOf<Long?>(null) }

    // Stage 8 UX Refactor: Note detail dialog state (replaces relations dialog)
    var showNoteDetail by remember { mutableStateOf<Long?>(null) }

    // Stage 16: Fullscreen Image Lightbox State
    var activeLightbox by remember { mutableStateOf<Pair<Int, List<File>>?>(null) }

    // Stage 21: TopBar dropdown menu & sort dialog state
    var showHeaderMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    // Stage 22: Activity & punchcard calendar bottom sheet state
    var showActivityCalendar by remember { mutableStateOf(false) }

    // Editor bottom sheet state
    var showEditor by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteContent by remember { mutableStateOf<String?>(null) }
    var editingNoteAttachments by remember { mutableStateOf<List<com.witte.lozify.domain.model.Attachment>>(emptyList()) }
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Stage 56: Long press action menu & Share card generator state
    var longPressedNote by remember { mutableStateOf<Note?>(null) }
    var sharingNote by remember { mutableStateOf<Note?>(null) }
    var showRandomStrollSheet by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Stage 22: Ensure list scrolls to the brand-new note at index 0 after room emits
    var shouldScrollToTopOnNewNote by remember { mutableStateOf(false) }

    // Stage 40: Flomo-style continuous elastic pull-to-sync physics
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val pullOffset = remember { Animatable(0f) }
    val maxPullDistancePx = remember(density) { with(density) { 160.dp.toPx() } }
    val syncTriggerThresholdPx = remember(density) { with(density) { 85.dp.toPx() } }
    val headerRestingHeightPx = remember(density) { with(density) { 52.dp.toPx() } }

    // Automatic spring return when sync completes or becomes IDLE
    LaunchedEffect(uiState.pullSyncState) {
        when (uiState.pullSyncState) {
            PullSyncState.IDLE -> {
                if (pullOffset.value > 0f) {
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            PullSyncState.SYNCING, PullSyncState.COMPLETED, PullSyncState.ERROR -> {
                // Keep resting open during active sync or result display
                if (pullOffset.value > 0f) {
                    pullOffset.animateTo(
                        targetValue = headerRestingHeightPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
            PullSyncState.PULLING -> {
                // Dragging in progress, handled by gesture
            }
        }
    }

    val nestedScrollConnection = remember(maxPullDistancePx, syncTriggerThresholdPx, headerRestingHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Downward pull at top of the feed
                if (source == NestedScrollSource.Drag &&
                    available.y > 0 &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    uiState.pullSyncState != PullSyncState.SYNCING
                ) {
                    val progress = (pullOffset.value / maxPullDistancePx).coerceIn(0f, 1f)
                    val resistance = (1f - progress * 0.55f) * 0.55f // Elastic rubber-band resistance
                    val newOffset = (pullOffset.value + available.y * resistance).coerceIn(0f, maxPullDistancePx)

                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }

                    if (newOffset > 10f && uiState.pullSyncState == PullSyncState.IDLE) {
                        homeViewModel.onPullDragging()
                    }
                    return Offset(0f, available.y)
                } else if (available.y < 0 && pullOffset.value > 0f && uiState.pullSyncState != PullSyncState.SYNCING) {
                    // Upward scroll while pulled down
                    val consumedY = available.y.coerceAtLeast(-pullOffset.value)
                    val newOffset = (pullOffset.value + consumedY).coerceAtLeast(0f)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    if (newOffset <= 10f && uiState.pullSyncState == PullSyncState.PULLING) {
                        homeViewModel.onPullCanceled()
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag &&
                    available.y > 0 &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    uiState.pullSyncState != PullSyncState.SYNCING
                ) {
                    val progress = (pullOffset.value / maxPullDistancePx).coerceIn(0f, 1f)
                    val resistance = (1f - progress * 0.55f) * 0.55f
                    val newOffset = (pullOffset.value + available.y * resistance).coerceIn(0f, maxPullDistancePx)

                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }

                    if (newOffset > 10f && uiState.pullSyncState == PullSyncState.IDLE) {
                        homeViewModel.onPullDragging()
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (uiState.pullSyncState == PullSyncState.PULLING) {
                    if (pullOffset.value >= syncTriggerThresholdPx) {
                        // Pulled past large threshold: Trigger cloud sync!
                        homeViewModel.triggerPullToSync()
                        pullOffset.animateTo(
                            targetValue = headerRestingHeightPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } else {
                        // Light/Medium pull: Spring back to 0 without triggering sync!
                        homeViewModel.onPullCanceled()
                        pullOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (uiState.pullSyncState == PullSyncState.PULLING) {
                    if (pullOffset.value >= syncTriggerThresholdPx) {
                        homeViewModel.triggerPullToSync()
                        pullOffset.animateTo(
                            targetValue = headerRestingHeightPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } else {
                        homeViewModel.onPullCanceled()
                        pullOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val topNoteId = uiState.notes.firstOrNull()?.id
    val notesCount = uiState.notes.size
    LaunchedEffect(topNoteId, notesCount) {
        if (shouldScrollToTopOnNewNote) {
            shouldScrollToTopOnNewNote = false
            listState.scrollToItem(0, 0)
        }
    }

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
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                    // Secondary insurance: ensure list is pinned at index 0
                    if (editingNoteId == null) {
                        scope.launch {
                            listState.scrollToItem(0, 0)
                        }
                    }
                }
                is EditorViewModel.EditorEvent.SaveError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar("保存失败: ${event.message}")
                    }
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
                pinnedTags = uiState.pinnedTags,
                selectedTag = uiState.selectedTag,
                stats = uiState.userStats,
                dailyCounts = uiState.heatmapData,
                userName = uiState.userName,
                isProUser = uiState.isProUser,
                onTagSelected = { tagId ->
                    homeViewModel.selectTag(tagId)
                },
                onTogglePinTag = { tagId, isPinned ->
                    homeViewModel.togglePinTag(tagId, isPinned)
                },
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToHelp = onNavigateToHelp,
                onNavigateToBackup = onNavigateToBackup,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPro = onNavigateToPro,
                onNavigateToDailyReview = onNavigateToDailyReview,
                onNavigateToRandomStroll = {
                    scope.launch { drawerState.close() }
                    showRandomStrollSheet = true
                },
                onOpenCalendarDetail = {
                    scope.launch {
                        drawerState.close()
                    }
                    showActivityCalendar = true
                },
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
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { homeViewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "搜索笔记内容...",
                                    fontSize = 14.sp,
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
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showHeaderMenu = true }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                if (uiState.selectedTag == null) {
                                    com.witte.lozify.presentation.components.LozifyBrandLogotype(
                                        height = 19.dp,
                                        color = Color(0xFF1C1C1E),
                                        modifier = Modifier.padding(end = 1.dp)
                                    )
                                } else {
                                    Text(
                                        text = "#${uiState.selectedTag?.name}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1C1C1E)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "切换视图与排序",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // 1:1 Flomo 风格顶部气泡下拉菜单
                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false },
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .width(220.dp)
                            ) {
                                // 1. 选择笔记
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "选择笔记",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF222222)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF444444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("批量选择管理功能即将在后续版本推出")
                                        }
                                    }
                                )

                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.5.dp)

                                // 2. 排序方式
                                DropdownMenuItem(
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "排序方式",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF222222)
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                    contentDescription = null,
                                                    tint = Color(0xFFCCCCCC),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = uiState.sortOrder.displayName,
                                                fontSize = 11.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                                            contentDescription = null,
                                            tint = Color(0xFF444444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        showSortDialog = true
                                    }
                                )

                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.5.dp)

                                // 3. 笔记来源
                                DropdownMenuItem(
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "笔记来源",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF222222)
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                                    contentDescription = null,
                                                    tint = Color(0xFFCCCCCC),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            val currentTag = uiState.selectedTag
                                            Text(
                                                text = if (currentTag != null) "#${currentTag.name}" else "全部",
                                                fontSize = 11.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FilterAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF444444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        com.witte.lozify.presentation.components.TwoLineMenuIcon(
                            color = Color(0xFF333333)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                homeViewModel.clearSearch()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "关闭搜索" else "搜索",
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F8FA)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Stage 40: Flomo-style elastic stats and sync card at the top
            if (pullOffset.value > 1f || uiState.pullSyncState != PullSyncState.IDLE) {
                PullToSyncHeader(
                    syncState = uiState.pullSyncState,
                    notesCount = uiState.totalActiveNotesCount,
                    statusText = uiState.pullSyncStatusText,
                    pullOffsetPx = pullOffset.value,
                    triggerThresholdPx = syncTriggerThresholdPx,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // Content area elastically translated down by pullOffset
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pullOffset.value
                    }
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00C853)
                            )
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
                                maxCollapsedLines = uiState.maxCollapseLines,
                                onTogglePinClick = {
                                    homeViewModel.togglePinStatus(note.id, note.isPinned)
                                },
                                onEditClick = {
                                    editingNoteId = note.id
                                    editingNoteContent = note.content
                                    editingNoteAttachments = note.attachments
                                    showEditor = true
                                },
                                onDeleteClick = {
                                    homeViewModel.deleteNote(note.id)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("笔记已删除")
                                    }
                                },
                                onCheckboxToggle = { noteId, newContent ->
                                    homeViewModel.updateNoteContent(noteId, newContent)
                                },
                                onTagClick = { tagName ->
                                    val tag = uiState.allTags.find { it.name == tagName }
                                    tag?.let { homeViewModel.selectTag(it.id) }
                                },
                                onMentionClick = { mentionedNoteId ->
                                    showNoteDetail = mentionedNoteId
                                },
                                onRelationsClick = { relations, title ->
                                    if (relations.isNotEmpty()) {
                                        val targetNoteId = if (title == "出链列表") relations.first().toNoteId else relations.first().fromNoteId
                                        showNoteDetail = targetNoteId
                                    }
                                },
                                outgoingRelations = note.outgoingRelations,
                                incomingRelations = note.incomingRelations,
                                allNotes = uiState.notes,
                                onImageClick = { imgIndex, images ->
                                    activeLightbox = Pair(imgIndex, images)
                                },
                                onLongClick = {
                                    longPressedNote = note
                                },
                                onShareClick = {
                                    sharingNote = note
                                },
                                searchQuery = uiState.searchQuery
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
                editingNoteAttachments = emptyList()
            },
            onSave = { textFieldValue, keptAttachmentIds, imageUris, fileUris ->
                val isNewNote = editingNoteId == null
                if (isNewNote) {
                    shouldScrollToTopOnNewNote = true
                    scope.launch {
                        listState.scrollToItem(0, 0)
                    }
                }
                editorViewModel.saveNote(
                    textFieldValue = textFieldValue,
                    keptExistingAttachmentIds = keptAttachmentIds,
                    imageUris = imageUris,
                    fileUris = fileUris,
                    noteId = editingNoteId
                )
            },
            initialContent = editingNoteContent,
            initialAttachments = editingNoteAttachments,
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

    // Stage 16: Fullscreen Image Lightbox
    activeLightbox?.let { (initialIndex, images) ->
        ImageLightboxDialog(
            images = images,
            initialIndex = initialIndex,
            onDismiss = { activeLightbox = null }
        )
    }

    // Stage 21: Sort Order Selection Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text(
                    text = "选择排序方式",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeViewModel.NoteSortOrder.values().forEach { order ->
                        val isSelected = uiState.sortOrder == order
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent)
                                .clickable {
                                    homeViewModel.setSortOrder(order)
                                    showSortDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00C853) else Color(0xFF333333)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("取消", color = Color(0xFF666666))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Stage 22 & 57: Activity & Punchcard Calendar Detail Bottom Sheet (with timezone support)
    if (showActivityCalendar) {
        ActivityCalendarBottomSheet(
            allNotes = uiState.allActiveNotes.ifEmpty { uiState.notes },
            timeZoneId = uiState.calendarTimeZone,
            onDismiss = { showActivityCalendar = false }
        )
    }

    // Stage 31: On-Launch App Update Dialog
    appUpdateInfo?.let { updateInfo ->
        val context = androidx.compose.ui.platform.LocalContext.current
        com.witte.lozify.presentation.update.AppUpdateDialog(
            updateInfo = updateInfo,
            onDismiss = { homeViewModel.dismissUpdateDialog() },
            onDownloadGitee = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo.downloadUrl))
                context.startActivity(intent)
            },
            onDownloadGithub = {
                val url = updateInfo.githubDownloadUrl ?: updateInfo.downloadUrl
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }
    // Stage 56: Long Press Action Bottom Sheet (Flomo style: 编辑, 分享, 复制, 删除, 取消)
    longPressedNote?.let { note ->
        NoteActionBottomSheet(
            note = note,
            onDismiss = { longPressedNote = null },
            onEdit = {
                editingNoteId = note.id
                editingNoteContent = note.content
                editingNoteAttachments = note.attachments
                showEditor = true
            },
            onShare = {
                sharingNote = note
            },
            onCopy = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(note.content))
                android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                homeViewModel.deleteNote(note.id)
                scope.launch {
                    snackbarHostState.showSnackbar("笔记已删除")
                }
            }
        )
    }

    // Stage 56: Generate Share Card Screen Modal Dialog (3 Flomo-style templates)
    sharingNote?.let { note ->
        val earliestTime = remember(uiState.notes) {
            uiState.notes.minOfOrNull { it.createdAt } ?: note.createdAt
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { sharingNote = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            com.witte.lozify.presentation.share.ShareCardScreen(
                note = note,
                totalNotesCount = uiState.notes.size,
                earliestNoteTimestamp = earliestTime,
                authorName = "Lozify 用户",
                filesDir = context.filesDir,
                onBackClick = { sharingNote = null }
            )
        }
    }

    // Stage 59: Random Stroll Bottom Sheet
    if (showRandomStrollSheet) {
        com.witte.lozify.presentation.stroll.RandomStrollBottomSheet(
            allNotes = uiState.notes,
            onDismiss = { showRandomStrollSheet = false },
            onEditNote = { note ->
                editingNoteId = note.id
                editingNoteContent = note.content
                editingNoteAttachments = note.attachments
                showEditor = true
            },
            onShareNote = { noteId ->
                sharingNote = uiState.notes.find { it.id == noteId }
            }
        )
    }

    // Stage 59: Fullscreen App Lock & Biometrics Overlay
    if (isLocked && isAppLockEnabled && appLockPin.isNotEmpty()) {
        com.witte.lozify.presentation.lock.AppLockOverlayScreen(
            correctPin = appLockPin,
            isBiometricEnabled = isBiometricEnabled,
            onUnlock = { isLocked = false }
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
