# Stage 15 补充开发日志 - UI 细节打磨、品牌 Logo 防变形与全新 App 图标

**日期**: 2026-08-18  
**阶段**: Stage 15.4 - UI Polish, Anti-distortion Brand Logo & App Launcher Icon  
**状态**: ✅ 已完成

---

## 📋 任务概述

本阶段重点针对产品的视觉细节、交互顺畅度与品牌质感进行精细打磨，全面对标 Flomo 极简圆润的设计标准：

1. **智能输入拦截器 V2 强化 (`SmartInputFilter.kt`)**：
   - 兼容中文输入法拼音提交换行时的智能回车跃出闭合标记；
   - 支持光标在正中间 (`**|**`) 与 4 字符空标记块末尾边缘 (`****|`) 时的原子级连带退格切除；
   - 增强光标就近边界弹斥结界，防止触摸误入定界符中间破坏语法。
2. **瀑布流标签点击 Toggle（反选回所有笔记）与滑动手势精简 (`HomeViewModel.kt`, `HomeScreen.kt`)**：
   - 再次点击已选中的 `#标签` 胶囊时，自动重置筛选回到“全部笔记”；
   - 移除列表卡片外层的 `SwipeToDismissBox` 滑动手势（添加特殊标记保留），消除纵向滑动误触与卡顿，操作统一归拢至卡片菜单。
3. **主页卡片操作图标与菜单全面矢量化 (`NoteCard.kt`)**：
   - 右上角更多按钮替换为标准 `Icons.Default.MoreHoriz`，去除背景底框；
   - 引入 `material-icons-extended` 官方扩展图标库；
   - 将卡片下拉菜单内的 Emoji（📋、✏️、🗑️、📌）全面升级为与侧边栏风格一致的 Material 矢量图标。
4. **防变形圆润品牌 Logo (`LozifyLogo.kt`)**：
   - 引入 `ui-text-google-fonts` 及官方证书 `font_certs.xml`，下载配置 `Nunito` 极粗（`FontWeight.Black`）字体；
   - 使用 `with(LocalDensity.current) { sizeDp.toSp() }` 强制锁定物理尺寸，杜绝系统大字体设置导致的 Logo 拉伸与撑爆；
   - 替换主页 TopBar 与侧边栏 Header。
5. **全新 App 启动图标 (Launcher Icon)**：
   - 基于用户提供的高清 "Lo" 徽标，算法处理去除四角黑边三角，无损保留渐变质感；
   - 生成 Android 8.0+ 自适应图标（`adaptive-icon`）与全分辨率（mdpi ~ xxxhdpi）切图，完成 Manifest 绑定。

---

## 🛠️ 变更与技术要点

### 1. 智能输入拦截器 V2 (`SmartInputFilter.kt`)
- 不再仅依赖单字符增减，针对输入法单次提交多字符含 `\n` 的情况，检测原光标右侧闭合标记，自动移至标记外层换行；
- 在 `MARKER_PAIRS = listOf("**", "==", "__")` 基础上，增加 4 字符边缘退格检测。

### 2. 防变形 Logo (`LozifyLogo.kt`)
```kotlin
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val NunitoFont = GoogleFont("Nunito")
val NunitoFontFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Black)
)
```

### 3. 全套 Android 自适应图标
- `app/src/main/res/values/ic_launcher_background.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`

---

## 📁 变更文件列表

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `app/src/main/java/com/witte/lozify/core/common/SmartInputFilter.kt` | 修改 | 升级 V2 智能输入拦截器 |
| `app/src/main/java/com/witte/lozify/presentation/components/LozifyLogo.kt` | 新增 | 防变形品牌 Logo 组件 (Google Fonts Nunito) |
| `app/src/main/java/com/witte/lozify/presentation/home/HomeViewModel.kt` | 修改 | `selectTag` 支持 Toggle 反选重置筛选 |
| `app/src/main/java/com/witte/lozify/presentation/home/HomeScreen.kt` | 修改 | 隐藏 Swipe 包装，TopAppBar 使用 LozifyLogo |
| `app/src/main/java/com/witte/lozify/presentation/home/DrawerContent.kt` | 修改 | DrawerHeader 使用 LozifyLogo |
| `app/src/main/java/com/witte/lozify/presentation/home/NoteCard.kt` | 修改 | 替换更多图标为 MoreHoriz，菜单全量使用 Material 矢量图标 |
| `app/src/main/res/values/font_certs.xml` | 新增 | Google Fonts 证书配置 |
| `app/src/main/res/values/ic_launcher_background.xml` | 新增 | 自适应图标背景色配置 |
| `app/src/main/res/mipmap-anydpi-v26/` | 新增 | 自适应图标 XML |
| `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` | 新增 | 全套图标切图 |
| `app/src/main/AndroidManifest.xml` | 修改 | 绑定全新 App 图标 |
| `gradle/libs.versions.toml` | 修改 | 增加 `material-icons-extended` 与 `ui-text-google-fonts` |
| `app/build.gradle.kts` | 修改 | 引入扩展图标库与 Google Fonts 依赖 |
| `development-log/2026-08/2026-08-18-stage15-ui-polish.md` | 新增 | 本次开发日志 |

---

## 📌 未完成 / 待规划功能清单 (Backlog)

结合此前开发日志与代码中记录的规划，以下功能已纳入后续规划池，等待后续阶段排期实现：

1. **🏷️ 侧边栏“置顶标签 (Pinned Tags)”持久化与自定义拖拽排序**：
   - 侧边栏已有“置顶标签”分区占位（当前显示“暂无置顶标签”）；
   - 需在数据库 `TagEntity` 中增加 `isPinned` 与 `sortOrder` 字段，并在标签管理界面或长按菜单中提供“设为置顶”操作。
2. **🔄 侧边栏标签排序与筛选逻辑 (`DrawerContent.kt`)**：
   - 侧边栏“全部标签”右侧排序图标点击事件尚未接入（目前为 `TODO: Implement sort/filter logic`），可支持按“名称 A-Z”、“笔记数从多到少”、“最近使用时间”排序。
3. **ℹ️ 帮助中心与新手引导页面 (Help Center)**：
   - 侧边栏底部的“帮助中心”入口尚未连接独立页面或 BottomSheet（目前为 `TODO: Navigate to help center`）。
4. **🔍 全文搜索关键词高亮与滚动定位 (Search Match Highlighting)**：
   - 搜索栏目前仅过滤卡片列表，尚未在卡片正文中对命中关键词添加黄色背景高亮，亦未实现平滑定位到匹配段落。
5. **🖼️ 图片附件大图灯箱预览 (Image Lightbox / Fullscreen Viewer)**：
   - 当前卡片中的图片网格可预览缩略图，但点击后尚未弹出可手势缩放、保存/分享的高清大图查看器。
6. **💾 本地数据全量备份与导入导出 (JSON / Markdown Zip)**：
   - 提供笔记与关联标签的一键导出为 Markdown/JSON 压缩包，以及离线数据恢复能力。
7. **🧪 自动化测试与断言覆盖 (Unit & UI Tests)**：
   - 补充 Markdown 语法高亮、智能拦截器（`SmartInputFilter`）与富文本正则提取的单元测试套件。
