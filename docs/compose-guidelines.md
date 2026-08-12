# Jetpack Compose 开发规范

本文档记录 Lozify 项目在使用 Jetpack Compose 时的技术约束和最佳实践。

## 图标使用规范 (Material Icons)

### ⚠️ 关键约束

**当前项目仅引入了 Compose 基础核心图标库，未引入扩展图标库。**

### 可用图标库

✅ **已引入（可直接使用）：**
```gradle
androidx.compose.material:material-icons-core
```

包含的常用图标：
- `Icons.Default.Add`
- `Icons.Default.Close`
- `Icons.Default.Delete`
- `Icons.Default.Search`
- `Icons.Default.Menu`
- `Icons.Default.Star`
- `Icons.Default.Home`
- `Icons.Default.Settings`
- `Icons.Default.ArrowBack`
- `Icons.Default.ArrowForward`
- `Icons.Default.KeyboardArrowUp`
- `Icons.Default.KeyboardArrowDown`
- `Icons.Default.MoreVert`
- `Icons.Default.Check`
- `Icons.Default.Clear`
- `Icons.Default.Info`
- `Icons.Default.Edit`
- `Icons.Default.Share`
- `Icons.Default.Favorite`
- `Icons.Default.Person`
- `Icons.Default.Refresh`

❌ **未引入（编译报错）：**
```gradle
androidx.compose.material:material-icons-extended
```

扩展包图标（当前项目不可用）：
- `Icons.Default.Archive` ❌ 编译错误
- `Icons.Default.PushPin` ❌ 编译错误
- `Icons.Default.ContentCopy` ❌ 编译错误
- `Icons.Default.Label` ❌ 编译错误
- 等等 5000+ 扩展图标

### 开发规则

**规则 1：只使用核心图标库中的图标**

在编写 UI 代码时，必须确认图标属于 `material-icons-core`，否则会导致编译失败。

**规则 2：优先选择语义相近的替代图标**

如果所需图标在扩展包中，寻找核心包中语义相近的替代：

| 需求 | 扩展包图标 (不可用) | 核心包替代 (可用) |
|------|-------------------|------------------|
| 归档 | `Archive` | `Delete` (删除，同样表示移除) |
| 置顶 | `PushPin` | `Star` (星标，通用收藏/置顶标识) |
| 恢复 | `Restore` | `Refresh` (刷新，表示重置/恢复) |
| 复制 | `ContentCopy` | `Share` (分享，类似传播语义) |
| 标签 | `Label` | 使用 Emoji 📌 或自定义 Painter |
| 文件夹 | `Folder` | `Menu` (菜单，表示容器/列表) |

**规则 3：Emoji 作为备用方案**

当核心图标库无法满足需求时，优先使用 Unicode Emoji：
```kotlin
Text(text = "📌", fontSize = 16.sp) // 置顶
Text(text = "🗑️", fontSize = 16.sp) // 删除
Text(text = "📋", fontSize = 16.sp) // 复制
Text(text = "🏷️", fontSize = 16.sp) // 标签
```

**规则 4：如需扩展图标，必须先修改依赖**

如果项目后续必须使用扩展图标，需在 `gradle/libs.versions.toml` 中添加：
```toml
[libraries]
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "compose-bom" }
```

并在 `app/build.gradle.kts` 中引入：
```kotlin
implementation(libs.androidx.material.icons.extended)
```

⚠️ 注意：扩展图标库体积较大（~10MB），会显著增加 APK 大小。

### 常见编译错误

**错误 1：Unresolved reference 'Archive'**
```
e: Unresolved reference 'Archive'.
e: Unresolved reference: Icons.Default.Archive
```

**原因**：`Archive` 属于扩展图标库，当前项目未引入。

**解决**：替换为 `Icons.Default.Delete` 或使用 Emoji "🗑️"。

**错误 2：Unresolved reference 'PushPin'**
```
e: Unresolved reference 'PushPin'.
e: Unresolved reference: Icons.Default.PushPin
```

**原因**：`PushPin` 属于扩展图标库，当前项目未引入。

**解决**：替换为 `Icons.Default.Star` 或使用 Emoji "📌"。

---

## Modifier 扩展函数注意事项

### 常用 Modifier 扩展的 Import 路径

| 扩展函数 | Import 路径 | 用途 |
|---------|------------|------|
| `.clip()` | `androidx.compose.ui.draw.clip` | 裁剪形状（圆角等） |
| `.shadow()` | `androidx.compose.ui.draw.shadow` | 添加阴影 |
| `.rotate()` | `androidx.compose.ui.draw.rotate` | 旋转组件 |
| `.scale()` | `androidx.compose.ui.draw.scale` | 缩放组件 |
| `.alpha()` | `androidx.compose.ui.draw.alpha` | 设置透明度 |
| `.blur()` | `androidx.compose.ui.draw.blur` | 模糊效果 |

### 常见编译错误

**错误：Unresolved reference 'clip'**
```
e: Unresolved reference 'clip'.
```

**原因**：缺少 `androidx.compose.ui.draw.clip` 的 import。

**解决**：添加 import 语句：
```kotlin
import androidx.compose.ui.draw.clip
```

---

## SwipeToDismiss 滑动手势规范

### Material 3 SwipeToDismissBox 使用

**正确的组件名**：`SwipeToDismissBox`（Material 3）

❌ **错误**：`SwipeToDismiss`（Material 2，已废弃）

### 滑动方向枚举

```kotlin
SwipeToDismissBoxValue.StartToEnd  // 从左向右滑动
SwipeToDismissBoxValue.EndToStart  // 从右向左滑动
SwipeToDismissBoxValue.Settled     // 未滑动状态
```

### 阈值设置

```kotlin
positionalThreshold = { totalDistance -> totalDistance * 0.4f }
```

建议阈值：30% ~ 50%，避免误触。

### 回弹处理

```kotlin
confirmValueChange = { dismissValue ->
    when (dismissValue) {
        SwipeToDismissBoxValue.StartToEnd -> {
            // 执行操作
            viewModel.doSomething()
            false // 返回 false，卡片回弹而不是消失
        }
        else -> false
    }
}
```

**关键**：如果不希望卡片被移除，`confirmValueChange` 必须返回 `false`。

---

## 更新日志

- **2026-08-12**：初始版本，记录图标使用规范和常见错误
- **Stage 9**：添加 SwipeToDismissBox 使用规范

---

## 参考资料

- [Material Icons 官方文档](https://fonts.google.com/icons)
- [Compose Material 3 组件库](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Compose UI Draw API](https://developer.android.com/jetpack/compose/graphics/draw)
