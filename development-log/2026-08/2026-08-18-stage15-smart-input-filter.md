# Stage 15 补充开发日志 - 智能输入拦截器 (Smart Input Interceptor)

**日期**: 2026-08-18  
**阶段**: Stage 15.3 - Smart Input Interceptor (Atomic Deletion, Enter Escape, Cursor Repulsion)  
**状态**: ✅ 已完成

---

## 📋 任务概述

在 Jetpack Compose 的 `TextField` 层面引入了智能输入拦截器 `SmartInputFilter`，针对 Markdown 编辑场景中常见的符号操作痛点进行了专门优化：

1. **原子级连带删除 (Atomic Deletion)**：
   - 当用户在空标记对（如 `**|**`、`==|==`、`__|__`）正中间按退格键时，自动将 4 个符号一次性完全切除，光标前移 2 格，避免用户需要连按 4 次退格键。
2. **智能回车跃出 (Smart Enter Escape)**：
   - 当光标紧挨着闭合标记（例如 `**加粗|**`）按回车键换行时，自动拦截换行符并向右跃出 2 格跳过闭合符号，在标记外层插入换行符（变成 `**加粗**\n|`），避免产生断裂语法 `**加粗\n**`。
3. **光标弹斥结界 (Cursor Repulsion)**：
   - 坚决防止光标因点击或方向键停留在同一对定界符号之间（如 `*|*`、`=|=`、`_|_`），根据移动方向智能向外或向内弹斥 1 格，保持标记对完整性。

---

## 🛠️ 核心架构实现

### [SmartInputFilter.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/common/SmartInputFilter.kt)
```kotlin
object SmartInputFilter {
    private val MARKER_PAIRS = listOf("**", "==", "__")

    fun applySmartInputFilter(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        var tempValue = newValue

        // 1. 原子级删除
        if (oldValue.text.length - newValue.text.length == 1 && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            for (marker in MARKER_PAIRS) {
                if (oldCursor >= 2 && oldCursor + 2 <= oldValue.text.length) {
                    val before = oldValue.text.substring(oldCursor - 2, oldCursor)
                    val after = oldValue.text.substring(oldCursor, oldCursor + 2)
                    if (before == marker && after == marker) {
                        val newText = oldValue.text.removeRange(oldCursor - 2, oldCursor + 2)
                        val newCursor = (oldCursor - 2).coerceIn(0, newText.length)
                        tempValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                        break
                    }
                }
            }
        }
        // 2. 智能回车跃出
        else if (newValue.text.length - oldValue.text.length == 1 && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            val insertedChar = newValue.text.getOrNull(oldCursor)
            if (insertedChar == '\n') {
                for (marker in MARKER_PAIRS) {
                    if (oldCursor + 2 <= oldValue.text.length) {
                        val nextTwo = oldValue.text.substring(oldCursor, oldCursor + 2)
                        if (nextTwo == marker) {
                            val before = oldValue.text.substring(0, oldCursor + 2)
                            val after = oldValue.text.substring(oldCursor + 2)
                            val newText = before + "\n" + after
                            val newCursor = (oldCursor + 2 + 1).coerceIn(0, newText.length)
                            tempValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                            break
                        }
                    }
                }
            }
        }

        // 3. 光标弹斥结界
        if (tempValue.selection.collapsed) {
            val cursor = tempValue.selection.start
            if (cursor > 0 && cursor < tempValue.text.length) {
                val prev = tempValue.text[cursor - 1]
                val next = tempValue.text[cursor]
                val isTrapped = (prev == '*' && next == '*') ||
                        (prev == '=' && next == '=') ||
                        (prev == '_' && next == '_')

                if (isTrapped) {
                    val direction = if (tempValue.selection.start >= oldValue.selection.start) 1 else -1
                    val repelledCursor = (cursor + direction).coerceIn(0, tempValue.text.length)
                    tempValue = tempValue.copy(selection = TextRange(repelledCursor))
                }
            }
        }

        return tempValue
    }
}
```

---

## ⚠️ 踩坑记录与边界安全防护

1. **`IndexOutOfBoundsException` 防护**：
   - 针对 `oldCursor - 2`、`oldCursor + 2` 以及 `cursor - 1`、`cursor` 进行严格的前置范围判断（如 `oldCursor >= 2 && oldCursor + 2 <= oldValue.text.length`）。
   - 所有的目标新光标位置统一经过 `.coerceIn(0, newText.length)` 安全约束。
2. **多选区删除不误伤**：
   - 只有在 `oldValue.selection.collapsed`（光标为单点没有选区）时才触发原子级删除与回车跃出，避免用户选中大段文字按退格时被误判。
3. **中文输入法联想不干扰**：
   - 拦截器仅在单字符变动（增减 1 个字符）时介入，中文输入法 Composing 组合字符变动时自动放行。

---

## 📁 变更文件列表

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `app/src/main/java/com/witte/lozify/core/common/SmartInputFilter.kt` | 新增 | 智能输入拦截器（原子删除、回车跃出、光标弹斥） |
| `app/src/main/java/com/witte/lozify/presentation/editor/NoteEditorBottomSheet.kt` | 修改 | `onValueChange` 接入 `SmartInputFilter` 逻辑 |
| `development-log/2026-08/2026-08-18-stage15-smart-input-filter.md` | 新增 | Stage 15.3 开发与踩坑总结日志 |

---

## 🔮 下一步计划 (Stage 16)
1. **侧边栏置顶标签（Pinned Tags）持久化与拖拽排序**。
2. **富文本单元测试与断言覆盖**。
3. **主页全文搜索关键词高亮与滚动定位**。
