package com.witte.lozify.presentation.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WebDavSyncScreen - Comprehensive cloud synchronization center with first-class Jianguoyun support.
 *
 * Stage 26: WebDAV Cloud Sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: WebDavSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGuide by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "坚果云 / WebDAV 云同步",
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
            // Section 1: Sync Status Card
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.lastSyncTime > 0) Color(0xFFE8F5E9) else Color(0xFFF0F2F5)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.lastSyncTime > 0) Icons.Outlined.CloudDone else Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = if (uiState.lastSyncTime > 0) Color(0xFF00C853) else Color(0xFF888888),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (uiState.lastSyncTime > 0) "云端数据已同步" else "未同步到云端",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = if (uiState.lastSyncTime > 0) {
                                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(uiState.lastSyncTime))
                                        "上次同步: $dateStr"
                                    } else {
                                        "本地共有 ${uiState.localNotesCount} 条笔记 · ${uiState.localTagsCount} 个标签"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        // Progress Indicator during Sync
                        AnimatedVisibility(visible = uiState.isSyncing) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val progress = uiState.syncProgress?.progress ?: 0f
                                val animatedProgress by animateFloatAsState(targetValue = progress, label = "syncProgress")
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00C853),
                                    trackColor = Color(0xFFE8F5E9)
                                )
                                Text(
                                    text = uiState.syncProgress?.detail ?: "正在同步...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00C853),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Result Message Banner
                        val syncResult = uiState.syncResult
                        if (syncResult != null && !uiState.isSyncing) {
                            val isSuccess = syncResult.isSuccess
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF00C853) else Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isSuccess) {
                                        "同步完成！上传 ${syncResult.uploadedNotes} 条笔记，下载 ${syncResult.downloadedNotes} 条，图文附件已自动同步。"
                                    } else {
                                        syncResult.errorMessage ?: "同步异常"
                                    },
                                    fontSize = 12.sp,
                                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startSync() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                enabled = !uiState.isSyncing && !uiState.isTestingConnection,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (uiState.isSyncing) "正在同步..." else "立即双向同步", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.testConnection() },
                                modifier = Modifier.height(44.dp),
                                enabled = !uiState.isSyncing && !uiState.isTestingConnection,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                            ) {
                                if (uiState.isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF666666)
                                    )
                                } else {
                                    Text(text = "测试连接", fontSize = 13.sp, color = Color(0xFF444444))
                                }
                            }
                        }

                        // Test Connection Result Banner
                        val testMsg = uiState.testConnectionMessage
                        if (testMsg != null && !uiState.isTestingConnection) {
                            val isSuccess = uiState.isTestSuccess
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF00C853) else Color(0xFFE53935),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = testMsg,
                                    fontSize = 12.sp,
                                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Account & Server Configuration Card
            item {
                Text(
                    text = "WebDAV 服务器与账户配置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Preset Selector Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WebDavPreset.values().forEach { preset ->
                                val isSelected = uiState.selectedPreset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color(0xFF00C853) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.onPresetSelected(preset) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF00C853) else Color(0xFF666666)
                                    )
                                }
                            }
                        }

                        // Server URL Field
                        OutlinedTextField(
                            value = uiState.serverUrl,
                            onValueChange = { viewModel.onServerUrlChanged(it) },
                            label = { Text("服务器地址 (WebDAV URL)") },
                            placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00C853),
                                focusedLabelColor = Color(0xFF00C853)
                            )
                        )

                        // Username Field
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            label = { Text("账号 / 邮箱 (Username / Email)") },
                            placeholder = { Text("例如: your_account@163.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00C853),
                                focusedLabelColor = Color(0xFF00C853)
                            )
                        )

                        // Password Field
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChanged(it) },
                            label = { Text("应用授权密码 (App Password)") },
                            placeholder = { Text("坚果云生成的应用授权密码") },
                            singleLine = true,
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                    Icon(
                                        imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (uiState.isPasswordVisible) "隐藏密码" else "显示密码",
                                        tint = Color(0xFF888888)
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    text = "注意: 坚果云请使用【安全设置】生成的专用应用密码，而非网页登录密码",
                                    fontSize = 11.sp,
                                    color = Color(0xFF888888)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00C853),
                                focusedLabelColor = Color(0xFF00C853)
                            )
                        )

                        // Remote Directory Field
                        OutlinedTextField(
                            value = uiState.remoteDir,
                            onValueChange = { viewModel.onRemoteDirChanged(it) },
                            label = { Text("云端存储根目录 (Remote Directory)") },
                            placeholder = { Text("/Lozify/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00C853),
                                focusedLabelColor = Color(0xFF00C853)
                            )
                        )

                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                        // Auto Sync Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "启动应用时自动静默同步",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "打开 Lozify 时自动拉取云端更新并同步本地修改",
                                    fontSize = 11.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = uiState.autoSync,
                                onCheckedChange = { viewModel.onAutoSyncToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853)
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Jianguoyun Setup Guide Card
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGuide = !showGuide },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "如何获取坚果云 WebDAV 应用授权密码？",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                            }
                            Icon(
                                imageVector = if (showGuide) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF888888)
                            )
                        }

                        AnimatedVisibility(visible = showGuide) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GuideStep(step = "1", title = "登录坚果云网页版", desc = "访问 jianguoyun.com 登录你的个人账号。")
                                GuideStep(step = "2", title = "进入安全设置", desc = "点击右上角个人昵称 -> 【账户信息】 -> 切换到【安全设置】选项卡。")
                                GuideStep(step = "3", title = "添加第三方应用授权", desc = "在页面下方找到【第三方应用管理】，点击【添加应用】。")
                                GuideStep(step = "4", title = "生成并填入密码", desc = "名称填写 Lozify，生成专属的授权密码后，复制并填入上方输入框即可。")
                            }
                        }
                    }
                }
            }

            // Section 4: Clear Config
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showClearDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "清空 WebDAV 同步配置", color = Color(0xFFE53935), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Confirm Clear Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空同步配置") },
            text = { Text("确定要清除保存在本机的 WebDAV 账号与授权信息吗？本地笔记不会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearConfiguration()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun GuideStep(step: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF222222))
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF888888), lineHeight = 15.sp)
        }
    }
}
