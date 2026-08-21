package com.witte.lozify.presentation.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * SettingsScreen - 1:1 Flomo-style redesigned unified settings center.
 *
 * Stage 58: Redesign Settings screen matching Flomo UI architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToWebDavSync: () -> Unit,
    onNavigateToPro: () -> Unit = {},
    onOpenCalendarDetail: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    var showActivationDialog by remember { mutableStateOf(false) }
    var inputLicenseCode by remember { mutableStateOf("") }

    var showPreferencesDialog by remember { mutableStateOf(false) }
    var showTimeZoneDialog by remember { mutableStateOf(false) }
    var showAppLockDialog by remember { mutableStateOf(false) }

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

    // 1. Edit User Name Dialog
    if (showAppLockDialog) {
        AppLockSettingsDialog(
            isAppLockEnabled = uiState.isAppLockEnabled,
            currentPin = uiState.appLockPin,
            isBiometricEnabled = uiState.isBiometricEnabled,
            isProUser = uiState.isProUser,
            onSave = { enabled, pin, biometric ->
                viewModel.setAppLock(enabled, pin, biometric)
            },
            onNavigateToPro = onNavigateToPro,
            onDismiss = { showAppLockDialog = false }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(text = "修改用户昵称", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text("请输入昵称 (如: 木下)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C853),
                        cursorColor = Color(0xFF00C853)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(inputName)
                        showEditNameDialog = false
                        Toast.makeText(context, "昵称修改成功", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("保存", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("取消", color = Color(0xFF888888))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Activation Code Dialog
    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            title = {
                Text(text = "输入会员激活码", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "请输入在爱发电/面包多购买的 16 位卡密：",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                    OutlinedTextField(
                        value = inputLicenseCode,
                        onValueChange = { inputLicenseCode = it.uppercase() },
                        placeholder = { Text("例如：LOZIFY-PRO888-2026", color = Color(0xFF999999)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00C853),
                            cursorColor = Color(0xFF00C853)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = viewModel.activateLicenseCode(inputLicenseCode)
                        if (res.isSuccess) {
                            Toast.makeText(context, res.getOrNull(), Toast.LENGTH_LONG).show()
                            showActivationDialog = false
                            inputLicenseCode = ""
                        } else {
                            Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "激活失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("立即激活", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivationDialog = false }) {
                    Text("取消", color = Color(0xFF888888))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Time Zone Dialog
    if (showTimeZoneDialog) {
        AlertDialog(
            onDismissRequest = { showTimeZoneDialog = false },
            title = {
                Text(text = "选择打卡日历时区", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
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

    // 4. Preferences Dialog (折叠行数 + 图片压缩 + 暂存草稿)
    if (showPreferencesDialog) {
        AlertDialog(
            onDismissRequest = { showPreferencesDialog = false },
            title = {
                Text(text = "偏好设置", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Max Lines Collapse
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "默认折叠行数", fontSize = 14.sp, color = Color(0xFF333333))
                            Text(text = "${uiState.maxCollapseLines} 行", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (3..8).forEach { lines ->
                                val isSelected = uiState.maxCollapseLines == lines
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                                        .clickable { viewModel.setMaxCollapseLines(lines) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$lines",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF00C853) else Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Draft Persistence Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "自动暂存未发送草稿", fontSize = 14.sp, color = Color(0xFF333333))
                        Switch(
                            checked = uiState.isDraftPersistenceEnabled,
                            onCheckedChange = { viewModel.setDraftPersistenceEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Image Compression Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "图片智能压缩 (2K)", fontSize = 14.sp, color = Color(0xFF333333))
                        Switch(
                            checked = uiState.isImageCompressionEnabled,
                            onCheckedChange = { viewModel.setImageCompressionEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Calendar Time Zone Entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPreferencesDialog = false
                                showTimeZoneDialog = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "打卡日历时区", fontSize = 14.sp, color = Color(0xFF333333))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFBBBBBB),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPreferencesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("完成", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Bar: [✕] Floating Button + Center "设置" Title (1:1 Flomo 图二)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating circular close button
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .clickable { onNavigateBack() },
                        color = Color.White,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "设置",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222),
                        modifier = Modifier.offset(x = (-19).dp) // Center align title with left button
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 2. User Profile Card with PRO Badge & "会员权益" Green Pill Button (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = uiState.userName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (uiState.isProUser) Color(0xFF00C853) else Color(0xFF9E9E9E))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isProUser) "PRO" else "PRO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isProUser) "终身尊享会员特权已生效" else "升级会员以享受 12+ 专属功能",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }

                        // Green Pill Button: "会员权益"
                        Button(
                            onClick = onNavigateToPro,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "会员权益",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Four Quick Grid Tiles: 记录统计 | 认知地图 | 偏好设置 | API & 云同步 (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlomoQuickTile(
                            icon = Icons.Outlined.DateRange,
                            label = "记录统计",
                            onClick = onOpenCalendarDetail
                        )
                        FlomoQuickTile(
                            icon = Icons.Outlined.Backup,
                            label = "备份导出",
                            onClick = onNavigateToBackup
                        )
                        FlomoQuickTile(
                            icon = Icons.Outlined.Tune,
                            label = "偏好设置",
                            onClick = { showPreferencesDialog = true }
                        )
                        FlomoQuickTile(
                            icon = Icons.Outlined.Code,
                            label = "API & 同步",
                            onClick = onNavigateToWebDavSync
                        )
                    }
                }
            }

            // 4. Group 1: 帮助与反馈 & 免费领会员 (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FlomoSettingsRow(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "帮助与反馈",
                            onClick = onNavigateToHelp
                        )
                        HorizontalDivider(color = Color(0xFFF7F8FA), thickness = 1.dp)
                        FlomoSettingsRow(
                            icon = Icons.Outlined.CardGiftcard,
                            title = "免费领会员 / 卡密兑换",
                            hasRedDot = true,
                            onClick = {
                                inputLicenseCode = ""
                                showActivationDialog = true
                            }
                        )
                    }
                }
            }

            // 5. Group 2: 账号、偏好与安全锁 (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FlomoSettingsRow(
                            icon = Icons.Outlined.Badge,
                            title = "用户昵称与资料",
                            subtitle = uiState.userName,
                            onClick = {
                                inputName = uiState.userName
                                showEditNameDialog = true
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF7F8FA), thickness = 1.dp)
                        FlomoSettingsRow(
                            icon = Icons.Outlined.Settings,
                            title = "偏好设置",
                            subtitle = "折叠行数 / 时区 / 压缩",
                            onClick = { showPreferencesDialog = true }
                        )
                        HorizontalDivider(color = Color(0xFFF7F8FA), thickness = 1.dp)
                        FlomoSettingsRow(
                            icon = Icons.Outlined.Lock,
                            title = "密码锁与指纹",
                            tagBadge = "限免体验",
                            subtitle = if (uiState.isAppLockEnabled) "已启用 (PIN/指纹)" else "已关闭",
                            onClick = { showAppLockDialog = true }
                        )
                    }
                }
            }

            // 6. Group 3: 数据导出与云同步 (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FlomoSettingsRow(
                            icon = Icons.Outlined.Description,
                            title = "导出 / 导入",
                            subtitle = "全量 JSON 备份与 Markdown 导出",
                            onClick = onNavigateToBackup
                        )
                        HorizontalDivider(color = Color(0xFFF7F8FA), thickness = 1.dp)
                        FlomoSettingsRow(
                            icon = Icons.Outlined.CloudSync,
                            title = "坚果云 / WebDAV 云同步",
                            subtitle = "跨设备多端增量自动同步",
                            onClick = onNavigateToWebDavSync
                        )
                    }
                }
            }

            // 7. Group 4: 关于我们与更新检查 (1:1 Flomo 图二)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FlomoSettingsRow(
                            icon = Icons.Outlined.Info,
                            title = "关于我们与检查更新",
                            subtitle = "v${com.witte.lozify.BuildConfig.VERSION_NAME} (点击检测最新版本)",
                            onClick = {
                                if (!uiState.isCheckingUpdate) {
                                    viewModel.checkForUpdate(isManual = true)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Quick Tile Component (1:1 Flomo 4 宫格快捷磁贴)
 */
@Composable
private fun FlomoQuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF222222),
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF444444)
        )
    }
}

/**
 * Standard Flomo Settings Menu Item Row
 */
@Composable
private fun FlomoSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    hasRedDot: Boolean = false,
    tagBadge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF333333),
                modifier = Modifier.size(20.dp)
            )

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

                if (hasRedDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252))
                    )
                }

                if (tagBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEEEEEE))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tagBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
