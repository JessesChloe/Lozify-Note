package com.witte.lozify.presentation.pro

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.witte.lozify.core.preferences.UserPreferencesManager

/**
 * ProMembershipScreen - 1:1 Flomo-style dark galaxy theme PRO membership and activation center.
 *
 * Stage 58: Redesign Pro Membership screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProMembershipScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPro by viewModel.isProUser.collectAsState()
    val proPlanType by viewModel.proPlanType.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var showActivationDialog by remember { mutableStateOf(false) }
    var inputLicenseCode by remember { mutableStateOf("") }
    var isAgreeTerms by remember { mutableStateOf(true) }
    var selectedPricingOption by remember { mutableStateOf(1) } // 0 = monthly, 1 = lifetime

    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            title = {
                Text(
                    text = "兑换 / 激活会员",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "请输入在爱发电/面包多购买的 16 位卡密或邀请码：",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = inputLicenseCode,
                        onValueChange = { inputLicenseCode = it.uppercase() },
                        placeholder = { Text("例如：LOZIFY-PRO888-2026", color = Color(0xFF999999), fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00C853),
                            unfocusedBorderColor = Color(0xFFDDDDDD),
                            cursorColor = Color(0xFF00C853)
                        ),
                        shape = RoundedCornerShape(8.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(8.dp)
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

    Scaffold(
        containerColor = Color(0xFF0E1017),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (Rounded translucent)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Brand Logo
                Text(
                    text = "Lozify",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                // Right Text Button
                Text(
                    text = if (isPro) "已开通" else "卡密激活",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPro) Color(0xFF00C853) else Color(0xFFFFD54F),
                    modifier = Modifier.clickable {
                        showActivationDialog = true
                    }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1017))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPro) {
                    // Already VIP status banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E261F),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00C853),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$userName · $proPlanType",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "权益已生效",
                                fontSize = 12.sp,
                                color = Color(0xFF00C853),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Pricing Selector Row: [连续包月 ¥15] [连续包年 ¥99 (立省 45%)]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option 1: Monthly
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPricingOption = 0 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPricingOption == 0) Color(0xFF2A2D3A) else Color(0xFF1A1C25),
                            border = BorderStroke(
                                width = if (selectedPricingOption == 0) 1.5.dp else 1.dp,
                                color = if (selectedPricingOption == 0) Color(0xFFFFD54F) else Color(0xFF333745)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "连续包月 ¥15",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedPricingOption == 0) Color(0xFFFFD54F) else Color.White
                                )
                            }
                        }

                        // Option 2: Lifetime / Annual (Recommended)
                        Box(modifier = Modifier.weight(1.3f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedPricingOption = 1 },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedPricingOption == 1) Color(0xFFFFF3D6) else Color(0xFF1A1C25),
                                border = BorderStroke(
                                    width = if (selectedPricingOption == 1) 1.5.dp else 1.dp,
                                    color = if (selectedPricingOption == 1) Color(0xFFFFC107) else Color(0xFF333745)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "终身买断 ¥99",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedPricingOption == 1) Color(0xFF422C00) else Color.White
                                    )
                                }
                            }

                            // Green Discount Tag
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-8).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00C853))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "立省 45%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Agreement Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        RadioButton(
                            selected = isAgreeTerms,
                            onClick = { isAgreeTerms = !isAgreeTerms },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00C853),
                                unselectedColor = Color(0xFF666666)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已阅读并同意《会员服务协议》和《购买须知》",
                            fontSize = 11.sp,
                            color = Color(0xFF7E8494)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PRO Premium Card (1:1 Flomo 暗黑星空金光卡片)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181B26),
                border = BorderStroke(1.dp, Color(0xFF2E344A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF222738), Color(0xFF141722))
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF3B2F15))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIP 会员",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                            }
                            Text(
                                text = "记录更高效，回顾更从容",
                                fontSize = 13.sp,
                                color = Color(0xFF9EA6BD)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2A3044), thickness = 0.8.dp)

                    // Features Checklist
                    ProFeatureRow(
                        icon = Icons.Outlined.Palette,
                        title = "Flomo 同款 3 套雅致分享模板",
                        desc = "经典白 / 复古桃粉 / 暖木手账"
                    )
                    ProFeatureRow(
                        icon = Icons.Outlined.CloudSync,
                        title = "坚果云 WebDAV 自动双向同步",
                        desc = "无感增量极速同步"
                    )
                    ProFeatureRow(
                        icon = Icons.Outlined.Image,
                        title = "全量多图排版与附件高保真图片导出",
                        desc = "无损超清大图渲染"
                    )
                    ProFeatureRow(
                        icon = Icons.Outlined.Headphones,
                        title = "原生音频卡片播放器与多格式附件",
                        desc = "PDF/Word/ZIP 懒加载"
                    )
                    ProFeatureRow(
                        icon = Icons.Outlined.Lock,
                        title = "私密手势与指纹生物识别密码锁",
                        desc = "本地端到端安全加密"
                    )
                    ProFeatureRow(
                        icon = Icons.Outlined.DateRange,
                        title = "侧边栏 13 周打卡热力图全景统计",
                        desc = "时区自动校准"
                    )
                }
            }

            // 2. Special Offer / Activation Card (对标图三：认证专享优惠)
            Text(
                text = "卡密与优惠兑换",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showActivationDialog = true },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF181B26),
                border = BorderStroke(1.dp, Color(0xFF2E344A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E2615)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CardGiftcard,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "卡密激活 · 兑换会员",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "输入 16 位卡密，即刻开通所有尊享特权",
                                fontSize = 11.sp,
                                color = Color(0xFF8E95A8)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "立即兑换",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFD54F)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFECEFF8)
            )
        }

        Text(
            text = desc,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFFFD54F)
        )
    }
}
