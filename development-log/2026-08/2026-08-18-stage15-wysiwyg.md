# Stage 15 补充开发日志 - 输入框实时 Markdown 语法高亮与符号弱化

**日期**: 2026-08-18  
**阶段**: Stage 15.2 - Real-time Markdown Syntax Highlighting & Marker Fading  
**状态**: ✅ 已完成

---

## 📋 任务概述

为了提升用户在输入框打字时的“所见即所得”富文本体验，同时保证不干扰 Jetpack Compose 底层文本光标与中文输入法的稳定性，我们引入了 `VisualTransformation` 机制，实现了输入框实时的 Markdown 语法高亮与标记符号弱化。

### 核心需求
1. **新建视觉转换器 (`MarkdownVisualTransformation.kt`)**：
   - 实现 `VisualTransformation` 接口，重写 `filter(text: AnnotatedString): TransformedText`。
   - 必须基于 `OffsetMapping.Identity` 返回，保证字符长度不变，光标绝对不会偏移或崩溃。
2. **支持语法高亮与分层染色 (Layered Styling)**：
   - **标签 (#Tag)**: 包含中文与英数，整体渲染为蓝色 `Color(0xFF1A73E8)`。
   - **加粗 (**Bold**)**: 外层 `**` 弱化为浅灰色 `Color(0xFFCCCCCC)`，内部文字加粗。
   - **高亮 (==Highlight==)**: 外层 `==` 弱化为浅灰色，内部文字浅黄背景 `Color(0xFFFFF59D)`。
   - **下划线 (__Underline__)**: 外层 `__` 弱化为浅灰色，内部文字带下划线。
   - **提及 (@[Title](note:id))**: 外层符号弱化，内部标题以紫色 `Color(0xFF7C4DFF)` 呈现。
   - **待办 (- [ ] / - [x])**: 绿色/灰色状态突出显示。
3. **输入框接入 (`NoteEditorBottomSheet.kt`)**：
   - 在 `TextField` 传入 `visualTransformation = remember { MarkdownVisualTransformation() }`。

---

## 🛠️ 架构与实现细节

### 分层染色算法 (Layered SpanStyle)
```kotlin
// 1. 先将整个匹配范围（包含符号）应用 markerStyle
addStyle(markerStyle, match.range.first, match.range.last + 1)

// 2. 获取捕获组的有效内容范围，单独覆盖内容高亮样式
val contentGroup = match.groups[1]
if (contentGroup != null && contentGroup.value.isNotEmpty()) {
    addStyle(
        style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF333333)),
        start = contentGroup.range.first,
        end = contentGroup.range.last + 1
    )
}
```

---

## ⚠️ 踩坑记录与避坑指南 (Pitfalls & Lessons Learned)

### 坑点 1：正则量词使用 `.+?` 导致空标记对无法弱化
- **现象**：当用户通过工具栏点击插入空标记（如 `****`、`====`、`____`）且光标在中间时，符号依旧是深黑色，只有输入一个字符后才变灰。
- **原因**：`+?` 要求至少存在 1 个字符，导致 0 字符的空标记无法被正则命中。
- **解法**：
  - 将量词调整为 `*?`（如 `\*\*(?s)(.*?)\*\*`、`==(?s)(.*?)==`）。
  - 在设置内层内容样式时，增加判空防护 `if (contentGroup.value.isNotEmpty())`，避免向 `AnnotatedString.Builder` 传入 0 长度的无效区间。

### 坑点 2：高亮正则右侧闭合符意外多字符
- **现象**：测试发现高亮 `==文本==` 完全没有渲染效果。
- **原因**：正则表达式右侧多写了一个等号 `==(?s)(.*?)===`（3 个等号），导致正常的双等号高亮无法匹配。
- **解法**：修正为严格的 `==(?s)(.*?)==`，并通过自动化编译与全量用例测试。

### 坑点 3：富文本输入框不要轻易在 `VisualTransformation` 中裁剪字符
- **痛点**：若试图在输入框中直接将 `**` 从视觉上隐藏（即输入 6 个字符只显示 2 个字符），必须手动计算极其繁琐的双向 `OffsetMapping`（尤其是涉及快速删除、长按选区、中文输入法 Composing 组合态时）。稍有差池就会抛出 `IndexOutOfBoundsException` 导致 App 崩溃退出。
- **最佳实践**：采用 **“弱化符号（Marker Fading）+ 1:1 Identity 映射”** 策略，既满足了所见即所得的现代视觉审美，又保证了 100% 的输入稳定性。

---

## 📁 变更文件列表

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `app/src/main/java/com/witte/lozify/core/common/MarkdownVisualTransformation.kt` | 新增 | 输入框实时 Markdown 语法高亮与符号弱化转换器 |
| `app/src/main/java/com/witte/lozify/presentation/editor/NoteEditorBottomSheet.kt` | 修改 | 输入框接入 `MarkdownVisualTransformation` |
| `development-log/2026-08/2026-08-18-stage15-wysiwyg.md` | 新增 | Stage 15.2 开发与踩坑总结日志 |

---

## 🔮 下一步计划 (Stage 16)
1. **侧边栏置顶标签（Pinned Tags）持久化与拖拽排序**。
2. **富文本单元测试与断言覆盖**。
3. **主页全文搜索关键词高亮**。
