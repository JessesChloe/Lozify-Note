package com.witte.lozify.presentation.review

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.common.TagUtils
import com.witte.lozify.domain.model.Note
import com.witte.lozify.presentation.components.ImageLightboxDialog
import com.witte.lozify.presentation.home.AttachmentGrid
import com.witte.lozify.presentation.share.ShareCardScreen
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * DailyReviewScreen - Fullscreen interactive time-machine daily review pager.
 * Features ultra-smooth nested LazyColumn scrolling, multi-image AttachmentGrid and lightbox preview.
 *
 * Stage 59: Daily Review feature.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: DailyReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var sharingNote by remember { mutableStateOf<Note?>(null) }
    var activeLightbox by remember { mutableStateOf<Pair<Int, List<File>>?>(null) }

    val cards = uiState.reviewCards
    val totalPages = if (cards.isEmpty()) 1 else cards.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "每日回顾",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = "重温旧灵感，产生新思考",
                            fontSize = 11.sp,
                            color = Color(0xFF888888)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF333333)
                        )
                    }
                },
                actions = {
                    if (cards.isNotEmpty() && pagerState.currentPage < cards.size) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${cards.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cards.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(text = "🌿", fontSize = 48.sp)
                        Text(
                            text = "暂无需要回顾的笔记",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "多记录一些日常碎片与灵感，时光机会在特定的日子为您带来惊喜与回顾。",
                            fontSize = 13.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("返回主页写笔记", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    if (page < cards.size) {
                        val cardItem = cards[page]
                        ReviewCard(
                            item = cardItem,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(RichTextUtils.stripFormatting(cardItem.note.content)))
                                Toast.makeText(context, "已复制笔记内容到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                sharingNote = cardItem.note
                            },
                            onNext = {
                                scope.launch {
                                    pagerState.animateScrollToPage(page + 1)
                                }
                            },
                            onImageClick = { index, files ->
                                activeLightbox = Pair(index, files)
                            }
                        )
                    } else {
                        // Celebration Finished Card
                        ReviewCompletionCard(
                            totalReviewed = cards.size,
                            onFinish = onNavigateBack,
                            onRestart = {
                                scope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Image Lightbox Preview
    activeLightbox?.let { (initialIndex, files) ->
        ImageLightboxDialog(
            images = files,
            initialIndex = initialIndex,
            onDismiss = { activeLightbox = null }
        )
    }

    // Share Card Modal Dialog
    sharingNote?.let { note ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { sharingNote = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ShareCardScreen(
                note = note,
                totalNotesCount = cards.size,
                earliestNoteTimestamp = note.createdAt,
                authorName = "Lozify 用户",
                filesDir = context.filesDir,
                onBackClick = { sharingNote = null }
            )
        }
    }
}

/**
 * ReviewCard - Individual time-machine review card.
 * Uses LazyColumn for silky smooth scrolling on arbitrary long texts and multi-image attachments.
 */
@Composable
private fun ReviewCard(
    item: ReviewCardItem,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onNext: () -> Unit,
    onImageClick: (Int, List<File>) -> Unit
) {
    val note = item.note
    val context = LocalContext.current
    val filesDir = context.filesDir

    val tags = remember(note.content, note.tags) {
        if (note.tags.isNotEmpty()) note.tags.map { it.name } else TagUtils.extractTags(note.content)
    }
    val cleanText = remember(note.content) {
        RichTextUtils.stripFormatting(note.content)
    }

    val formattedDate = remember(note.createdAt) {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())
        formatter.format(note.createdAt)
    }

    val imageAttachments = remember(note.attachments) {
        note.attachments.filter { it.isImage() }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 1. Time Machine Milestone Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3D6))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = item.milestoneBadge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC28416)
                    )
                }

                if (item.daysAgo > 0) {
                    Text(
                        text = "${item.daysAgo} 天前",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Note Tags (if any)
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF0F4FE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color(0xFF84A2EE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. Scrollable Note Body & Images with LazyColumn for high-performance fluid scroll
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Text Content
                if (cleanText.isNotBlank()) {
                    item {
                        Text(
                            text = cleanText,
                            fontSize = 16.sp,
                            color = Color(0xFF222222),
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Images Grid
                if (imageAttachments.isNotEmpty() && filesDir != null) {
                    item {
                        AttachmentGrid(
                            attachments = imageAttachments,
                            filesDir = filesDir,
                            onImageClick = onImageClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Footer info & Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Share Card
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享卡片",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Next Button
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("下一篇", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ReviewCompletionCard - Shown on the last page after reviewing all notes.
 */
@Composable
private fun ReviewCompletionCard(
    totalReviewed: Int,
    onFinish: () -> Unit,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "今日回顾已完成！",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "您已重温了 $totalReviewed 条珍贵的历史灵感与随想。温故而知新，继续保持记录的好习惯吧！",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "完成回顾，回到主页",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRestart,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "再看一遍",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF444444)
                    )
                }
            }
        }
    }
}
