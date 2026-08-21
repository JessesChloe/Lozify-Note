package com.witte.lozify.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppLockSettingsDialog - Configures 4-digit PIN and Biometrics for PRO App Lock.
 *
 * Stage 59: App Lock & Biometrics feature.
 */
@Composable
fun AppLockSettingsDialog(
    isAppLockEnabled: Boolean,
    currentPin: String,
    isBiometricEnabled: Boolean,
    isProUser: Boolean,
    onSave: (enabled: Boolean, pin: String, biometric: Boolean) -> Unit,
    onNavigateToPro: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(isAppLockEnabled) }
    var pinInput by remember { mutableStateOf(currentPin) }
    var pinConfirm by remember { mutableStateOf(currentPin) }
    var isBiometric by remember { mutableStateOf(isBiometricEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("私密密码锁与指纹", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
            }
        },
        text = {
            if (!isProUser) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔒 应用密码锁为 Lozify PRO 专属高级隐私特权。升级后可开启数字锁与指纹/人脸识别，防止他人窥屏。",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToPro()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("立即了解 PRO 会员特权", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Toggle Enable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("开启应用启动锁", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF222222))
                            Text("退至后台重新进入时需解锁", fontSize = 12.sp, color = Color(0xFF999999))
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }

                    if (isEnabled) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)

                        // 4-Digit PIN Input
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinInput = it },
                            label = { Text("设置 4~6 位数字 PIN 码") },
                            placeholder = { Text("例如 1234") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = pinConfirm,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinConfirm = it },
                            label = { Text("确认 PIN 码") },
                            placeholder = { Text("再次输入 PIN 码") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Biometric Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("支持指纹 / 人脸识别", fontSize = 14.sp, color = Color(0xFF333333))
                            }
                            Checkbox(
                                checked = isBiometric,
                                onCheckedChange = { isBiometric = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00C853))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isProUser) {
                Button(
                    onClick = {
                        if (isEnabled) {
                            if (pinInput.length < 4) {
                                Toast.makeText(context, "PIN 码至少需要 4 位数字", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (pinInput != pinConfirm) {
                                Toast.makeText(context, "两次输入的 PIN 码不一致", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        onSave(isEnabled, pinInput, isBiometric)
                        Toast.makeText(context, if (isEnabled) "密码锁已启用" else "密码锁已关闭", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("保存设置", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF888888))
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}
