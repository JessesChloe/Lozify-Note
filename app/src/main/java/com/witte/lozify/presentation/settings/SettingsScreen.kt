package com.witte.lozify.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * SettingsScreen - Unified settings and preferences center for Lozify.
 *
 * Stage 17: Settings Architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToWebDavSync: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.updateMessage) {
        val msg = uiState.updateMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUpdateMessage()
        }
    }

    if (uiState.updateInfo != null) {
        com.witte.lozify.presentation.update.AppUpdateDialog(
            updateInfo = uiState.updateInfo!!,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownloadGitee = {
                viewModel.openDownloadUrl(context, uiState.updateInfo!!.downloadUrl)
            },
            onDownloadGithub = {
                val ghUrl = uiState.updateInfo?.githubDownloadUrl ?: uiState.updateInfo!!.downloadUrl
                viewModel.openDownloadUrl(context, ghUrl)
            }
        )
    }

    var showTimeZoneDialog by remember { mutableStateOf(false) }
    val timeZoneOptions = remember {
        listOf(
            "" to "跟随系统默认时区 (当前: ${java.time.ZoneId.systemDefault().id})",
            "Asia/Shanghai" to "中国标准时间 / 北京时间 (Asia/Shanghai, UTC+8)",
            "UTC" to "协调世界时 (UTC, GMT+0)",
            "Asia/Tokyo" to "日本标准时间 (Asia/Tokyo, UTC+9)",
            "America/New_York" to "美国东部时间 (America/New_York, UTC-5/UTC-4)",
            "America/Los_Angeles" to "美国太平洋时间 (America/Los_Angeles, UTC-8/UTC-7)",
            "Europe/London" to "欧洲西部时间 (Europe/London, UTC+0/UTC+1)",
            "Europe/Berlin" to "欧洲中部时间 (Europe/Berlin, UTC+1/UTC+2)"
        )
    }

    if (showTimeZoneDialog) {
        AlertDialog(
            onDismissRequest = { showTimeZoneDialog = false },
            title = {
                Text(
                    text = "选择打卡日历时区",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "若跨设备（如真机与模拟器/平板）使用坚果云同步，固定统一时区可确保两端打卡日历的绿色方块完全一致。",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    timeZoneOptions.forEach { (tzId, tzName) ->
                        val isSelected = uiState.calendarTimeZone == tzId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCalendarTimeZone(tzId)
                                    showTimeZoneDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tzName,
                                fontSize = 14.sp,
                                color = if (isSelected) Color(0xFF00C853) else Color(0xFF333333),
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeZoneDialog = false }) {
                    Text("取消", color = Color(0xFF666666))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
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
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Edit & Display Preferences
            item {
                SectionHeader(title = "编辑与显示偏好")
            }

            item {
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Draft Persistence Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "自动暂存未发送草稿",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "意外关闭新建弹窗时，自动保留未发送的文字与图片",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888),
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = uiState.isDraftPersistenceEnabled,
                                onCheckedChange = { viewModel.setDraftPersistenceEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                        // Max Lines Collapse Selector
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "笔记卡片默认折叠行数",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "${uiState.maxCollapseLines} 行",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00C853)
                                )
                            }

                            Text(
                                text = "长文段超过该行数将自动折叠，可在卡片底部点击展开或收起",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )

                            // Quick Selector Tabs: 3, 4, 5, 6, 7, 8
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (3..8).forEach { lines ->
                                    val isSelected = uiState.maxCollapseLines == lines
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                                            )
                                            .clickable { viewModel.setMaxCollapseLines(lines) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$lines",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF00C853) else Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                        // Stage 57: Calendar Timezone Row
                        val currentTzName = timeZoneOptions.find { it.first == uiState.calendarTimeZone }?.second
                            ?: if (uiState.calendarTimeZone.isBlank()) "跟随系统 (${java.time.ZoneId.systemDefault().id})" else uiState.calendarTimeZone
                        SettingsClickableRow(
                            icon = Icons.Outlined.Schedule,
                            title = "打卡日历时区",
                            subtitle = currentTzName,
                            onClick = { showTimeZoneDialog = true }
                        )
                    }
                }
            }

            // Section 2: Storage & Media (Stage 37)
            item {
                SectionHeader(title = "存储与媒体")
            }

            item {
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Image Compression Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "图片智能压缩",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "插入图片时自动进行 2K 视觉无损压缩，大幅节省本地存储与坚果云空间",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888),
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = uiState.isImageCompressionEnabled,
                                onCheckedChange = { viewModel.setImageCompressionEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853)
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Data & Cloud Sync
            item {
                SectionHeader(title = "数据与云同步")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Backup & Restore Entry
                        SettingsClickableRow(
                            icon = Icons.Outlined.Backup,
                            title = "本地数据全量备份与迁移",
                            subtitle = "支持全量 JSON 图文备份与 Markdown 知识库导出",
                            onClick = onNavigateToBackup
                        )

                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                        // WebDAV Cloud Sync Entry
                        SettingsClickableRow(
                            icon = Icons.Outlined.CloudSync,
                            title = "坚果云 / WebDAV 云同步",
                            subtitle = "支持跨设备多端自动备份与双向同步",
                            onClick = onNavigateToWebDavSync
                        )
                    }
                }
            }

            // Section 3: About & Help
            item {
                SectionHeader(title = "关于与帮助")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Help Center Entry
                        SettingsClickableRow(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "帮助中心与使用指南",
                            subtitle = "排版语法、双向网状链接与手势操作说明",
                            onClick = onNavigateToHelp
                        )

                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                        // Version info & Update Checker Entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!uiState.isCheckingUpdate) {
                                        viewModel.checkForUpdate(isManual = true)
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "检查新版本",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF222222)
                                    )
                                    Text(
                                        text = "当前版本: Lozify v${com.witte.lozify.BuildConfig.VERSION_NAME}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }

                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00C853)
                                )
                            } else {
                                Text(
                                    text = "点击检测 ➔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00C853)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF888888),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF555555),
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFECE0))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                color = Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(14.dp)
        )
    }
}
