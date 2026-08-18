package com.witte.lozify.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storage
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
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * BackupRestoreScreen - UI for exporting JSON backups, Markdown ZIP packages, and restoring data.
 *
 * Stage 16: Complete Local Data Lifecycle Management.
 *
 * @param onNavigateBack Callback to return to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    // Listen for status messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // SAF Activity Result Launchers
    val defaultFileNameSuffix = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportJson(it, contentResolver) }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportMarkdownZip(it, contentResolver) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it, contentResolver) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "数据备份与迁移",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Stats Card
                item {
                    DataStatsCard(
                        totalNotes = uiState.totalNotes,
                        totalTags = uiState.totalTags
                    )
                }

                // Section 1: Export Card
                item {
                    ExportCard(
                        onExportJson = {
                            exportJsonLauncher.launch("lozify_backup_$defaultFileNameSuffix.json")
                        },
                        onExportMarkdown = {
                            exportZipLauncher.launch("lozify_markdown_$defaultFileNameSuffix.zip")
                        }
                    )
                }

                // Section 2: Import Card
                item {
                    ImportCard(
                        onImportJson = {
                            importJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFF1A73E8),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "正在处理数据，请稍候...",
                                fontSize = 14.sp,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * DataStatsCard - Displays current database size and storage notice.
 */
@Composable
private fun DataStatsCard(
    totalNotes: Int,
    totalTags: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "本地存储现状",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(count = totalNotes.toString(), label = "条有效笔记")
                StatItem(count = totalTags.toString(), label = "个分类标签")
            }

            Text(
                text = "💡 所有数据均存储在手机本地，换机或刷机前请务必导出全量 JSON 备份文件妥善保存。",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF9FAFB))
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A73E8)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

/**
 * ExportCard - Card with JSON and Markdown export options.
 */
@Composable
private fun ExportCard(
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "数据导出与备份",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            // Option 1: Full JSON Backup
            ActionOptionRow(
                icon = Icons.Default.Backup,
                title = "全量图文 JSON 备份 (.json)",
                description = "完整备份。包含全部笔记正文、图片附件（Base64 编码）、创建时间、置顶状态、分类标签与 @双向链接 索引，可完整恢复。",
                buttonText = "导出 JSON",
                onClick = onExportJson
            )

            HorizontalDivider(color = Color(0xFFF2F2F2), thickness = 0.8.dp)

            // Option 2: Markdown ZIP
            ActionOptionRow(
                icon = Icons.Default.Archive,
                title = "Markdown 图文知识包 (.zip)",
                description = "将每条笔记单独打包为带 YAML 标头的 .md 文件，并同步打包关联的本地图片至 images/ 目录，支持 Obsidian / Logseq / Notion 直接图文浏览。",
                buttonText = "导出 ZIP",
                onClick = onExportMarkdown
            )
        }
    }
}

/**
 * ImportCard - Card for importing JSON backups.
 */
@Composable
private fun ImportCard(
    onImportJson: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "数据恢复与导入",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            ActionOptionRow(
                icon = Icons.Default.Download,
                title = "从备份文件或 Markdown 知识包恢复",
                description = "支持选取 Lozify 全量 JSON 备份文件 (.json) 或 Markdown 图文知识包 (.zip)，数据与图片将智能识别并增量合并还原至本地沙盒中。",
                buttonText = "选择文件恢复",
                buttonColor = Color(0xFF00C853),
                onClick = onImportJson
            )
        }
    }
}

@Composable
private fun ActionOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color = Color(0xFF1A73E8),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Text(
            text = description,
            fontSize = 12.sp,
            color = Color(0xFF777777),
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}
