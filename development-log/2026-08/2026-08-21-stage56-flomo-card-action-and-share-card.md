# Stage 56: 卡片内容点击展开、长按操作半窗与 Flomo 风格分享图生成系统

**日期**: 2026-08-21  
**阶段**: Stage 56  
**类型**: 功能增强 / 交互重构 / 视觉优化  

---

## 1. 需求与背景

1. **卡片折叠交互优化**：
   - 之前卡片折叠截断时只能点击右下角“展开全部”文字，现需支持直接点击超长卡片正文区域也可快速展开全部内容，保持链接、标签、多选框独立交互。
2. **长按卡片操作半窗（对标 Flomo）**：
   - 长按首页笔记卡片任意位置，弹出精简操作半窗，提供【编辑】、【分享】、【复制】、【删除】和【取消】。
3. **Flomo 风格分享图生成与导出系统**：
   - 100% 纯 Jetpack Compose 代码自适应渲染 3 套高保真模板（无外部图片切图依赖，超清矢量绘制）：
     1. **纯白极简 (Classic White)**：日期、Logo、标签、作者与 5x5 像素级绿色打卡热力图矩阵。
     2. **暖粉复古 (Vintage Warm Peach)**：雅致暖粉色调、圆环横线日期修饰、复古红褐标签与居中落款。
     3. **暖阳便签 (Warm Journal / Yellow)**：暖黄羊皮纸质感、金黄色 `< Lozify` 与 `# ⊙ ✦` 符号、日记风斜落款。
4. **两阶段底部抽屉式交互与安全边距**：
   - 默认主界面底部常驻圆角半窗操作栏（更换模板、保存图片、系统分享、复制文字）；
   - 点击“更换模板”后抽屉从底部滑出覆盖原有操作栏，提供模板缩略图与【确定】按钮；
   - 底部配置 32dp 充裕空白缓冲行，彻底避开系统手势黑条遮挡。

---

## 2. 核心架构与技术实现

### 2.1 卡片内容点击展开 (`NoteCard.kt`)
- 在富文本 `Text` 上挂载 `pointerInput(parsedRichText.annotatedString, effectiveExpanded, hasVisualOverflow)`；
- 采用 `detectTapGestures` 识别单次点击，判断命中非超链接/非标签区域且处于折叠溢出态时触发 `isExpanded = true`；
- 支持长按手势（`onLongPress`）以及卡片根容器 `combinedClickable`，无缝唤起长按操作半窗。

### 2.2 长按操作半窗 (`NoteActionBottomSheet.kt`)
- 采用 Material3 `ModalBottomSheet`，包含编辑、分享、复制、删除（红色高危强调）、取消选项；
- 在卡片右上角 `...` 下拉菜单中同步提供【生成分享图】直达入口。

### 2.3 分享图页面与 3 套自适应模板 (`ShareCardScreen.kt`)
- 采用 `Scaffold` 顶层布局与 `innerPadding` 动态约束，中间预览画板支持垂直滚动，绝不挤压或遮挡底部栏；
- 抽屉采用 `slideInVertically` + `fadeIn` 动效覆盖，提供模板缩略图实时预览与【确定】关闭交互；
- 图标与文字间距精细化调整为 `2.dp`，底部注入 `32.dp` 缓冲距离，完美避让 Android 导航手势横条。

### 2.4 超清位图测量与导出 (`ShareCardExporter.kt`)
- `captureViewToBitmap`：采用 `MeasureSpec.UNSPECIFIED` 对 ComposeView 离屏测量与绘制，支持超长卡片完整截取为高清 Bitmap；
- `saveBitmapToGallery`：适配 Android 10+ Scoped Storage，通过 `MediaStore.Images` 保存至相册（`Pictures/Lozify`）；
- `shareBitmap`：通过 `FileProvider` 临时授权分享，唤起 Android 原生分享面板。

---

## 3. 变更文件清单

| 文件路径 | 变更类型 | 说明 |
|---|---|---|
| `app/src/main/java/com/witte/lozify/core/common/ShareCardExporter.kt` | 新增 | 视图位图截取、系统相册存储与 FileProvider 分享 |
| `app/src/main/java/com/witte/lozify/presentation/home/NoteActionBottomSheet.kt` | 新增 | 长按卡片操作半窗 |
| `app/src/main/java/com/witte/lozify/presentation/share/ShareCardScreen.kt` | 新增 | 分享卡片生成器、3 套模板与两阶段底部抽屉 |
| `app/src/main/java/com/witte/lozify/presentation/home/NoteCard.kt` | 修改 | 支持点击内容展开、长按手势及菜单分享入口 |
| `app/src/main/java/com/witte/lozify/presentation/home/HomeScreen.kt` | 修改 | 挂载长按半窗与分享卡片弹窗状态 |
| `app/src/main/res/xml/file_paths.xml` | 修改 | 添加临时分享图片缓存目录权限 |

---

## 4. 验证与测试

- **全量 JVM 单元测试与代码编译**：
  ```powershell
  $env:JAVA_HOME = "D:\Android\Android Studio\jbr"; .\gradlew test
  ```
  **结果**：63 项构建任务与全部单元测试 100% 通过（`BUILD SUCCESSFUL in 8s`）。
