package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.presentation.editor.EditorViewModel
import com.witte.lozify.presentation.editor.NoteEditorBottomSheet
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * HomeScreen - Main feed displaying note cards in waterfall layout.
 *
 * Stage 3: Real database integration with Room + ViewModel.
 * Displays empty state when no notes, otherwise shows note list.
 * Includes editor bottom sheet for creating new notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    editorViewModel: EditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Editor bottom sheet state
    var showEditor by remember { mutableStateOf(false) }
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Listen for editor events
    LaunchedEffect(Unit) {
        editorViewModel.events.collect { event ->
            when (event) {
                is EditorViewModel.EditorEvent.NoteSaved -> {
                    snackbarHostState.showSnackbar("笔记已保存")
                }
                is EditorViewModel.EditorEvent.SaveError -> {
                    snackbarHostState.showSnackbar("保存失败: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lozify",
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("侧边栏 (Stage 5 实现)")
                        }
                    }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = "菜单"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("搜索功能 (Post-MVP)")
                        }
                    }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_search),
                            contentDescription = "搜索"
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
                    showEditor = true
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F8FA),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics widget placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "统计挂件占位区 (Stage 10 实现)",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteCard(
                                content = note.content,
                                timestamp = formatTimestamp(note.createdAt),
                                onMoreClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("更多操作 (Stage 9 实现)")
                                    }
                                }
                            )
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
            onDismiss = {
                showEditor = false
            },
            onSave = { content ->
                editorViewModel.saveNote(content)
            }
        )
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
