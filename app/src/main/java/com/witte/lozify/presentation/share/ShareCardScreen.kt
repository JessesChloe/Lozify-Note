package com.witte.lozify.presentation.share

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.witte.lozify.core.common.FileUtils
import com.witte.lozify.core.common.RichTextUtils
import com.witte.lozify.core.common.ShareCardExporter
import com.witte.lozify.core.common.TagUtils
import com.witte.lozify.domain.model.Attachment
import com.witte.lozify.domain.model.Note
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Share Card Template Options.
 */
enum class ShareTemplateType(val title: String) {
    CLASSIC_WHITE("纯白极简"),
    VINTAGE_PEACH("暖粉复古"),
    WARM_JOURNAL("暖阳便签")
}

/**
 * ShareCardScreen - Generates high-fidelity shareable note card images with 3 Flomo-style templates.
 * Uses a bottom sheet / sliding drawer for template switching that covers the action bar when opened.
 */
@Composable
fun ShareCardScreen(
    note: Note,
    totalNotesCount: Int = 1,
    earliestNoteTimestamp: Instant = note.createdAt,
    authorName: String = "Lozify 用户",
    filesDir: File? = null,
    onBackClick: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(ShareTemplateType.CLASSIC_WHITE) }
    var showTemplateSelector by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var composeViewRef by remember { mutableStateOf<ComposeView?>(null) }

    // Calculate days active
    val daysCount = remember(earliestNoteTimestamp) {
        val diff = Duration.between(earliestNoteTimestamp, Instant.now()).toDays()
        diff.coerceAtLeast(1).toInt()
    }

    // Format note dates
    val formattedDate = remember(note.createdAt) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()).withZone(ZoneId.systemDefault())
        formatter.format(note.createdAt)
    }
    val formattedDotDate = remember(note.createdAt) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault()).withZone(ZoneId.systemDefault())
        formatter.format(note.createdAt)
    }

    // Filter image attachments and file attachments
    val imageAttachments = remember(note.attachments) {
        note.attachments.filter { it.isImage() }
    }
    val fileAttachments = remember(note.attachments) {
        note.attachments.filter { it.isFile() }
    }

    // Extract tags
    val tags = remember(note.content, note.tags) {
        if (note.tags.isNotEmpty()) {
            note.tags.map { it.name }
        } else {
            TagUtils.extractTags(note.content)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF7F8FA),
            topBar = {
                // 1. 顶部标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF333333)
                        )
                    }

                    Text(
                        text = "生成分享图",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }
            },
            bottomBar = {
                // 2. 默认底部操作栏 (Flomo 经典圆角半窗栏，4个圆形功能按钮)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomActionButton(
                                icon = Icons.Outlined.Layers,
                                label = "更换模板",
                                iconTint = Color(0xFF333333),
                                onClick = { showTemplateSelector = true }
                            )

                            BottomActionButton(
                                icon = Icons.Outlined.FileDownload,
                                label = "保存图片",
                                iconTint = Color(0xFF333333),
                                onClick = {
                                    scope.launch {
                                        try {
                                            val view = composeViewRef
                                            if (view != null) {
                                                val bitmap = ShareCardExporter.captureViewToBitmap(view)
                                                ShareCardExporter.saveBitmapToGallery(context, bitmap)
                                            } else {
                                                Toast.makeText(context, "正在准备预览...", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "导出失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )

                            BottomActionButton(
                                icon = Icons.Outlined.Share,
                                label = "系统分享",
                                iconTint = Color(0xFF00C853),
                                circleBgColor = Color(0xFFE8F5E9),
                                onClick = {
                                    scope.launch {
                                        try {
                                            val view = composeViewRef
                                            if (view != null) {
                                                val bitmap = ShareCardExporter.captureViewToBitmap(view)
                                                ShareCardExporter.shareBitmap(context, bitmap)
                                            } else {
                                                Toast.makeText(context, "正在准备预览...", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "分享失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )

                            BottomActionButton(
                                icon = Icons.Outlined.ContentCopy,
                                label = "复制文字",
                                iconTint = Color(0xFF333333),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(note.content))
                                    Toast.makeText(context, "文字已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 底部空白缓冲行 (32.dp 充裕安全缓冲，彻底避开系统手势黑条)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        ) { innerPadding ->
            // 3. 中部画板预览区 (支持滚动，完全适应 innerPadding，绝不溢出)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 核心捕捉容器：AndroidView 结合 ComposeView 进行超清渲染与 Bitmap 捕捉
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color(0x22000000)
                            )
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    composeViewRef = this
                                    setContent {
                                        ShareCardContent(
                                            templateType = selectedTemplate,
                                            note = note,
                                            tags = tags,
                                            formattedDate = formattedDate,
                                            formattedDotDate = formattedDotDate,
                                            authorName = authorName,
                                            totalNotesCount = totalNotesCount,
                                            daysCount = daysCount,
                                            imageAttachments = imageAttachments,
                                            fileAttachments = fileAttachments,
                                            filesDir = filesDir
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                composeViewRef = view
                                view.setContent {
                                    ShareCardContent(
                                        templateType = selectedTemplate,
                                        note = note,
                                        tags = tags,
                                        formattedDate = formattedDate,
                                        formattedDotDate = formattedDotDate,
                                        authorName = authorName,
                                        totalNotesCount = totalNotesCount,
                                        daysCount = daysCount,
                                        imageAttachments = imageAttachments,
                                        fileAttachments = fileAttachments,
                                        filesDir = filesDir
                                    )
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 4. “更换模板”底部抽屉 (对标 Flomo 图二：从底部滑出覆盖原有操作栏，含缩略图与确定按钮)
        AnimatedVisibility(
            visible = showTemplateSelector,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showTemplateSelector = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showTemplateSelector,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // 模板卡片横向选择器
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TemplateThumbnail(
                            title = "纯白极简",
                            bgColor = Color.White,
                            accentColor = Color(0xFF84A2EE),
                            isSelected = selectedTemplate == ShareTemplateType.CLASSIC_WHITE,
                            onClick = { selectedTemplate = ShareTemplateType.CLASSIC_WHITE },
                            modifier = Modifier.weight(1f)
                        )
                        TemplateThumbnail(
                            title = "暖粉复古",
                            bgColor = Color(0xFFFBE9E2),
                            accentColor = Color(0xFF9E5E4E),
                            isSelected = selectedTemplate == ShareTemplateType.VINTAGE_PEACH,
                            onClick = { selectedTemplate = ShareTemplateType.VINTAGE_PEACH },
                            modifier = Modifier.weight(1f)
                        )
                        TemplateThumbnail(
                            title = "暖阳便签",
                            bgColor = Color(0xFFFFFDF0),
                            accentColor = Color(0xFFD49520),
                            isSelected = selectedTemplate == ShareTemplateType.WARM_JOURNAL,
                            onClick = { selectedTemplate = ShareTemplateType.WARM_JOURNAL },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.8.dp)

                    // 居中“确定”按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTemplateSelector = false }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }

                    // 抽屉底部空白缓冲行
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

/**
 * Renders the chosen template inside the capture container.
 */
@Composable
private fun ShareCardContent(
    templateType: ShareTemplateType,
    note: Note,
    tags: List<String>,
    formattedDate: String,
    formattedDotDate: String,
    authorName: String,
    totalNotesCount: Int,
    daysCount: Int,
    imageAttachments: List<Attachment>,
    fileAttachments: List<Attachment>,
    filesDir: File?
) {
    when (templateType) {
        ShareTemplateType.CLASSIC_WHITE -> {
            ClassicWhiteTemplate(
                note = note,
                tags = tags,
                formattedDate = formattedDate,
                authorName = authorName,
                totalNotesCount = totalNotesCount,
                daysCount = daysCount,
                imageAttachments = imageAttachments,
                fileAttachments = fileAttachments,
                filesDir = filesDir
            )
        }
        ShareTemplateType.VINTAGE_PEACH -> {
            VintagePeachTemplate(
                note = note,
                tags = tags,
                formattedDate = formattedDotDate,
                authorName = authorName,
                totalNotesCount = totalNotesCount,
                daysCount = daysCount,
                imageAttachments = imageAttachments,
                fileAttachments = fileAttachments,
                filesDir = filesDir
            )
        }
        ShareTemplateType.WARM_JOURNAL -> {
            WarmJournalTemplate(
                note = note,
                tags = tags,
                formattedDate = formattedDotDate,
                authorName = authorName,
                totalNotesCount = totalNotesCount,
                imageAttachments = imageAttachments,
                fileAttachments = fileAttachments,
                filesDir = filesDir
            )
        }
    }
}

/**
 * 模板 1: 纯白极简 (Classic White) - 对标 Flomo 经典纯白卡片与绿色打卡点阵热力图
 */
@Composable
private fun ClassicWhiteTemplate(
    note: Note,
    tags: List<String>,
    formattedDate: String,
    authorName: String,
    totalNotesCount: Int,
    daysCount: Int,
    imageAttachments: List<Attachment>,
    fileAttachments: List<Attachment>,
    filesDir: File?
) {
    val cleanText = remember(note.content) {
        RichTextUtils.stripFormatting(note.content)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Header: Date + Brand Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Lozify",
                    fontSize = 13.sp,
                    color = Color(0xFFBDBDBD),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tag badges (if any)
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
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

            // Note Content
            Text(
                text = cleanText,
                fontSize = 15.sp,
                color = Color(0xFF333333),
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal
            )

            // Images (All images rendered as large full-width cards with software bitmap support)
            if (imageAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareImageAttachmentList(attachments = imageAttachments, filesDir = filesDir)
            }

            // File Attachments (Documents, Audio, Archives, etc.)
            if (fileAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareFileAttachmentList(
                    attachments = fileAttachments,
                    templateType = ShareTemplateType.CLASSIC_WHITE,
                    filesDir = filesDir
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer: Author & Stats + Heatmap matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = authorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalNotesCount MEMOS · $daysCount DAYS",
                        fontSize = 10.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Normal
                    )
                }

                // Flomo 风格 5x5 像素级绿色打卡热力图矩阵
                FlomoHeatmapMatrix()
            }
        }
    }
}

/**
 * 模板 2: 暖粉复古 (Vintage Warm Peach) - 对标 Flomo 优雅暖桃粉与居中复古修饰
 */
@Composable
private fun VintagePeachTemplate(
    note: Note,
    tags: List<String>,
    formattedDate: String,
    authorName: String,
    totalNotesCount: Int,
    daysCount: Int,
    imageAttachments: List<Attachment>,
    fileAttachments: List<Attachment>,
    filesDir: File?
) {
    val cleanText = remember(note.content) {
        RichTextUtils.stripFormatting(note.content)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E2)),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFF2D5CB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Header: Vintage Centered Date with Circles and Lines
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFC49A8B), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.8.dp)
                        .background(Color(0xFFC49A8B))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF9E5E4E),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.8.dp)
                        .background(Color(0xFFC49A8B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFC49A8B), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag badges
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3D5C8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color(0xFF9E5E4E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Note Content
            Text(
                text = cleanText,
                fontSize = 15.sp,
                color = Color(0xFF4A352F),
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal
            )

            // Images
            if (imageAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareImageAttachmentList(attachments = imageAttachments, filesDir = filesDir)
            }

            // File Attachments
            if (fileAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareFileAttachmentList(
                    attachments = fileAttachments,
                    templateType = ShareTemplateType.VINTAGE_PEACH,
                    filesDir = filesDir
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer: Centered Author, Stats and Divider
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = authorName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8D675A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalNotesCount MEMOS · $daysCount DAYS",
                    fontSize = 10.sp,
                    color = Color(0xFFB89387)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(Color(0xFFE2BAAD))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lozify",
                    fontSize = 11.sp,
                    color = Color(0xFFA87C6F),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 模板 3: 暖阳便签 (Warm Journal / Yellow) - 对标 Flomo 米黄羊皮纸质感与手账风落款
 */
@Composable
private fun WarmJournalTemplate(
    note: Note,
    tags: List<String>,
    formattedDate: String,
    authorName: String,
    totalNotesCount: Int,
    imageAttachments: List<Attachment>,
    fileAttachments: List<Attachment>,
    filesDir: File?
) {
    val cleanText = remember(note.content) {
        RichTextUtils.stripFormatting(note.content)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF0)),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFF0E5CE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            // Header: < Lozify brand + amber glyph symbols (# ⊙ ✦)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹ Lozify",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD49520)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "#", fontSize = 13.sp, color = Color(0xFFD49520), fontWeight = FontWeight.Bold)
                    Text(text = "⊙", fontSize = 13.sp, color = Color(0xFFD49520))
                    Text(text = "✦", fontSize = 13.sp, color = Color(0xFFD49520))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag badges
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF3D6))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color(0xFFC28416),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Note Content
            Text(
                text = cleanText,
                fontSize = 15.sp,
                color = Color(0xFF3C362A),
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal
            )

            // Images
            if (imageAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareImageAttachmentList(attachments = imageAttachments, filesDir = filesDir)
            }

            // File Attachments
            if (fileAttachments.isNotEmpty() && filesDir != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ShareFileAttachmentList(
                    attachments = fileAttachments,
                    templateType = ShareTemplateType.WARM_JOURNAL,
                    filesDir = filesDir
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer: Right-aligned signature and stats
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "— $authorName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3C362A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedDate / $totalNotesCount MEMOS",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8270)
                )
            }
        }
    }
}

/**
 * Flomo 风格像素级 5x5 打卡点阵热力图
 */
@Composable
private fun FlomoHeatmapMatrix() {
    val matrixColors = listOf(
        listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFE8F5E9), Color(0xFF81C784), Color(0xFFE8F5E9)),
        listOf(Color(0xFFC8E6C9), Color(0xFFE8F5E9), Color(0xFF00C853), Color(0xFFC8E6C9), Color(0xFF81C784)),
        listOf(Color(0xFFE8F5E9), Color(0xFF81C784), Color(0xFFC8E6C9), Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
        listOf(Color(0xFF81C784), Color(0xFFC8E6C9), Color(0xFFE8F5E9), Color(0xFF00C853), Color(0xFFE8F5E9)),
        listOf(Color(0xFFE8F5E9), Color(0xFF00C853), Color(0xFF81C784), Color(0xFFC8E6C9), Color(0xFF00C853))
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        matrixColors.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                row.forEach { dotColor ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

/**
 * Image attachment list for share cards.
 * Renders all images as full-width natural cards, disabling hardware bitmaps
 * to prevent "Software rendering doesn't support hardware bitmaps" on canvas capture.
 */
@Composable
private fun ShareImageAttachmentList(
    attachments: List<Attachment>,
    filesDir: File
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        attachments.forEach { attachment ->
            val imageFile = File(filesDir, attachment.filePath)
            if (imageFile.exists()) {
                val imageRequest = remember(attachment.filePath) {
                    ImageRequest.Builder(context)
                        .data(imageFile)
                        .allowHardware(false) // Fix "Software rendering doesn't support hardware bitmaps"
                        .crossfade(false)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

/**
 * File attachment list for share cards (Documents, Audio, Zip, Video, etc.).
 * Displays clean badges with file icons, names, and sizes matching the template theme.
 */
@Composable
private fun ShareFileAttachmentList(
    attachments: List<Attachment>,
    templateType: ShareTemplateType,
    filesDir: File
) {
    val theme = when (templateType) {
        ShareTemplateType.CLASSIC_WHITE -> ShareColorTheme(
            cardBgColor = Color(0xFFF7F8FA),
            borderColor = Color(0xFFEEEEEE),
            textColor = Color(0xFF333333),
            badgeBgColor = Color(0xFFE8F5E9),
            badgeTextColor = Color(0xFF2E7D32)
        )
        ShareTemplateType.VINTAGE_PEACH -> ShareColorTheme(
            cardBgColor = Color(0xFFF5DDD3),
            borderColor = Color(0xFFE5C0B3),
            textColor = Color(0xFF4A352F),
            badgeBgColor = Color(0xFFEBD1C6),
            badgeTextColor = Color(0xFF9E5E4E)
        )
        ShareTemplateType.WARM_JOURNAL -> ShareColorTheme(
            cardBgColor = Color(0xFFF9F2DD),
            borderColor = Color(0xFFE9DCBD),
            textColor = Color(0xFF3C362A),
            badgeBgColor = Color(0xFFF2E6C7),
            badgeTextColor = Color(0xFFC28416)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            val localFile = File(filesDir, attachment.filePath)
            val fileSizeStr = if (localFile.exists()) {
                val sizeBytes = localFile.length()
                if (sizeBytes < 1024) "$sizeBytes B"
                else if (sizeBytes < 1024 * 1024) String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
                else String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
            } else {
                attachment.getFormattedFileSize()
            }

            val category = FileUtils.getFileCategory(attachment.filePath, attachment.mimeType)
            val (icon, categoryLabel) = when (category) {
                FileUtils.FileCategory.AUDIO -> Pair(Icons.Outlined.AudioFile, "音频")
                FileUtils.FileCategory.PDF -> Pair(Icons.Outlined.PictureAsPdf, "PDF")
                FileUtils.FileCategory.DOCUMENT -> Pair(Icons.Outlined.Description, "文档")
                FileUtils.FileCategory.SPREADSHEET -> Pair(Icons.Outlined.TableChart, "表格")
                FileUtils.FileCategory.PRESENTATION -> Pair(Icons.Outlined.Slideshow, "演示")
                FileUtils.FileCategory.ARCHIVE -> Pair(Icons.Outlined.FolderZip, "压缩包")
                FileUtils.FileCategory.VIDEO -> Pair(Icons.Outlined.VideoFile, "视频")
                FileUtils.FileCategory.CODE -> Pair(Icons.Outlined.Code, "代码")
                FileUtils.FileCategory.OTHER -> Pair(Icons.Outlined.AttachFile, "附件")
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardBgColor),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, theme.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.badgeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = theme.badgeTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = attachment.getDisplayName(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[$categoryLabel]",
                                fontSize = 11.sp,
                                color = theme.badgeTextColor,
                                fontWeight = FontWeight.Normal
                            )
                            if (fileSizeStr.isNotBlank()) {
                                Text(
                                    text = "·  $fileSizeStr",
                                    fontSize = 11.sp,
                                    color = theme.textColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ShareColorTheme(
    val cardBgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color
)

/**
 * 模板缩略图卡片 (带选中绿勾指示)
 */
@Composable
private fun TemplateThumbnail(
    title: String,
    bgColor: Color,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF00C853) else Color(0xFFE2E2E2),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(8.dp)
        ) {
            // Mock mini layout lines
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .background(accentColor.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .background(Color(0xFFCCCCCC))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(3.dp)
                        .background(Color(0xFFDDDDDD))
                )
            }

            // Green selected checkmark badge at bottom right
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color(0xFF00C853) else Color(0xFF555555)
        )
    }
}

/**
 * 底部圆形操作按钮 (Flomo 经典风格：圆形灰底/绿底背景 + 居中图标 + 贴近文字标注)
 */
@Composable
private fun BottomActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color(0xFF333333),
    circleBgColor: Color = Color(0xFFF7F8FA),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(circleBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666)
        )
    }
}
