package com.witte.lozify.presentation.lock

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppLockOverlayScreen - Fullscreen biometric & PIN code lock interceptor.
 * Safe native Android biometrics implementation without crashing or theme requirements.
 *
 * Stage 59: App Lock & Biometrics feature.
 */
@Composable
fun AppLockOverlayScreen(
    correctPin: String,
    isBiometricEnabled: Boolean,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun triggerBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle("解锁 Lozify")
                    .setSubtitle("验证指纹或面容以访问您的便签")
                    .setNegativeButton("使用 PIN 码", context.mainExecutor) { _, _ -> }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    context.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onUnlock()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            isError = true
                        }
                    }
                )
            } catch (e: Throwable) {
                // If biometric hardware or permission is unavailable, fallback smoothly to PIN
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled) {
            triggerBiometric()
        }
    }

    fun handleKeyPress(key: String) {
        if (inputPin.length < 6) {
            val newPin = inputPin + key
            inputPin = newPin
            isError = false
            if (newPin.length == correctPin.length) {
                if (newPin == correctPin) {
                    onUnlock()
                } else {
                    isError = true
                    Toast.makeText(context, "PIN 码错误，请重试", Toast.LENGTH_SHORT).show()
                    inputPin = ""
                }
            }
        }
    }

    fun handleDelete() {
        if (inputPin.isNotEmpty()) {
            inputPin = inputPin.dropLast(1)
            isError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10121A))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 36.dp, horizontal = 32.dp)
        ) {
            // Header: Lock Icon + App Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Lozify 私密记事",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (isError) "PIN 码错误，请重新输入" else "请输入安全 PIN 码解锁",
                    fontSize = 13.sp,
                    color = if (isError) Color(0xFFFF5252) else Color(0xFF888888)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = if (correctPin.isNotEmpty()) correctPin.length else 4
                    repeat(dotCount) { index ->
                        val isFilled = index < inputPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Color(0xFF00C853)
                                    else if (isError) Color(0xFFFF5252)
                                    else Color(0xFF33394D)
                                )
                        )
                    }
                }
            }

            // Keypad (3x4 grid)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "bio" -> {
                                    if (isBiometricEnabled) {
                                        IconButton(
                                            onClick = { triggerBiometric() },
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "指纹解锁",
                                                tint = Color(0xFF00C853),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                "del" -> {
                                    IconButton(
                                        onClick = { handleDelete() },
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "删除",
                                            tint = Color(0xFFCCCCCC),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2333))
                                            .clickable { handleKeyPress(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
