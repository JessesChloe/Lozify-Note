package com.witte.lozify.presentation.help

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.presentation.components.LozifyLogo

/**
 * HelpCenterScreen - Comprehensive User Guide and FAQ Screen.
 *
 * Stage 16:
 * Displays formatting cheatsheet, bidirectional linking guide,
 * gesture controls, tag organization tips, and privacy notice.
 *
 * @param onNavigateBack Callback to return to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "帮助中心与指南",
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
            // Brand Hero Card
            item {
                BrandHeroCard()
            }

            // Section 1: Markdown Formatting & Smart Input
            item {
                HelpCard(
                    title = "✍️ 快捷排版与智能拦截",
                    subtitle = "在编辑器中直接输入以下标记即可实时排版："
                ) {
                    GuideItem(
                        syntax = "**加粗文本**",
                        desc = "突出强调核心观点或关键短语"
                    )
                    GuideItem(
                        syntax = "__下划线文本__",
                        desc = "为文本添加精美下划线修饰"
                    )
                    GuideItem(
                        syntax = "==高亮文本==",
                        desc = "使用荧光黄色背景标记重要内容"
                    )
                    GuideItem(
                        syntax = "- [ ] 待办事项",
                        desc = "生成可直接点击勾选的交互清单"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 智能拦截器技巧：\n• 在闭合标记内按回车可直接跃出标记换行。\n• 连续按退格键可连带整组闭合标记一同原子化删除。",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        lineHeight = 19.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F4F8))
                            .padding(10.dp)
                    )
                }
            }

            // Section 2: Bi-directional Linking & Tags
            item {
                HelpCard(
                    title = "🌐 双向网状链接与标签体系",
                    subtitle = "打破单篇笔记孤岛，构建属于你的第二大脑："
                ) {
                    GuideItem(
                        syntax = "#标签名",
                        desc = "将碎片想法按主题归类，卡片中点击胶囊标签可直接筛选瀑布流。"
                    )
                    GuideItem(
                        syntax = "@[笔记引用]",
                        desc = "输入 @ 唤出引用弹窗，在两张卡片间建立双向网络连接。"
                    )
                    GuideItem(
                        syntax = "上下文时间线 (Thread)",
                        desc = "卡片底部出链/入链卡片支持点击呼出时间线对话视图，直观回溯思考脉络。"
                    )
                }
            }

            // Section 3: Lightbox & Gestures
            item {
                HelpCard(
                    title = "🖼️ 大图灯箱与手势交互",
                    subtitle = "沉浸式高清图片浏览体验："
                ) {
                    GuideItem(
                        syntax = "轻触图片",
                        desc = "在瀑布流或时间线中点击缩略图，一键进入全屏纯黑大图灯箱。"
                    )
                    GuideItem(
                        syntax = "双击缩放",
                        desc = "双击图片可在 1.0x 与 2.5x 之间平滑切换放大。"
                    )
                    GuideItem(
                        syntax = "双指捏合 & 左右轻扫",
                        desc = "支持 1.0x~5.0x 自由缩放与平移；单条笔记多图支持左右滑动翻页。"
                    )
                }
            }

            // Section 4: Sidebar Organization & Search
            item {
                HelpCard(
                    title = "🏷️ 侧边栏管理与全文高亮搜索",
                    subtitle = "高效组织与毫秒级知识检索："
                ) {
                    GuideItem(
                        syntax = "置顶标签",
                        desc = "侧边栏标签菜单首项支持“设为置顶”，将核心标签固定在侧边栏顶部。"
                    )
                    GuideItem(
                        syntax = "多维排序与即时过滤",
                        desc = "“全部标签”右上角提供即时搜索框，并支持按“使用频次”、“名称字母 (A-Z)”、“最新创建”随时排序。"
                    )
                    GuideItem(
                        syntax = "全文关键词高亮",
                        desc = "主页顶栏搜索时，正文命中词自动黄色高亮标注并自适应展开卡片。"
                    )
                }
            }

            // Section 5: Local Privacy & Security
            item {
                HelpCard(
                    title = "🛡️ 100% 本地离线与隐私保护",
                    subtitle = "你的想法，只属于你自己："
                ) {
                    Text(
                        text = "• Lozify 采用完全离线的本地 SQLite / Room 数据库架构，所有文字、图片附件与标签关系均存储于设备本地沙盒中。\n• 无需注册登录，无任何隐私上传，无网络亦能极速流畅使用。",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        lineHeight = 20.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * BrandHeroCard - Top hero card displaying Lozify logo and version.
 */
@Composable
private fun BrandHeroCard() {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LozifyLogo(sizeDp = 32.dp)

            Text(
                text = "Lozify · 灵感双链笔记",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Text(
                text = "版本 1.2.0 (Build 2026.08)",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )

            Text(
                text = "随时捕捉灵感闪念，编织属于你的网状第二大脑。",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * HelpCard - Generic styled section card for guide contents.
 */
@Composable
private fun HelpCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF888888),
                lineHeight = 18.sp
            )

            HorizontalDivider(color = Color(0xFFF2F2F2), thickness = 0.8.dp)

            content()
        }
    }
}

/**
 * GuideItem - Single syntax row item.
 */
@Composable
private fun GuideItem(
    syntax: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = syntax,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A73E8)
            )
        }

        Text(
            text = desc,
            fontSize = 13.sp,
            color = Color(0xFF444444),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
