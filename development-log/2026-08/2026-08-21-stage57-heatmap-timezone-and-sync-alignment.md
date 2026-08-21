# Stage 57: 侧边栏打卡日历热力图跨设备时区对齐与同步刷新修复

**日期**：2026-08-21  
**阶段**：Stage 57  
**状态**：已完成 (Completed) & 通过全量单元测试

---

## 1. 需求背景与问题现象

在多设备（如真机 + Android 模拟器 / 平板）使用坚果云 WebDAV 进行云同步时，用户反馈：
- 同步后，侧边栏的打卡日历热力图（Contribution Heatmap）两台设备上的标绿方块不一致（如一台设备标在 8月21日，另一台设备标在 8月20日）；
- 侧边栏日历图在某些情况下未随云端笔记同步即时重绘刷新。

---

## 2. 根本原因排查与剖析

1. **时区转换差异（核心原因）**：
   - 笔记在底层数据库和云端 JSON 中统一使用 UTC 绝对时间戳（`createdAt: Instant`）；
   - 在统计打卡天数时，原先直接使用当前设备的系统时区：`it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()`；
   - 真机通常为东八区（UTC+8），而测试模拟器/海外时区设备默认为 UTC（GMT+0）；两端相差 8 个小时，导致在夜间或清晨创建的笔记被映射到不同的 `LocalDate`（相差 1 天），在两端网格中呈现出不同的绿色方块。

2. **`ContributionHeatmap` 组件内 `today` 无 Key 缓存锁死**：
   - 原代码中使用了 `val today = remember { LocalDate.now() }`（未指定 key），导致即便同步了新数据，日历网格的基准起始日期和当前日轮廓未能响应刷新。

3. **日历详情页数据源全量性**：
   - 当侧边栏打卡日历被点击打开 `ActivityCalendarBottomSheet` 时，原先传入了当前可能被标签或搜索过滤的列表，现统一传入 `allActiveNotes`，确保全量打卡数据完整。

---

## 3. 核心改进与技术方案

### 3.1 用户偏好设置：可配置打卡日历时区
- 在 `UserPreferencesManager` 中新增 `calendarTimeZone: StateFlow<String>`（默认空字符串代表跟随系统）；
- 在 `SettingsScreen` 中新增 **“打卡日历时区”** 设置项及弹窗，支持预设时区：
  - 跟随系统默认时区
  - 中国标准时间 / 北京时间 (Asia/Shanghai, UTC+8)
  - 协调世界时 (UTC, GMT+0)
  - 日本标准时间 (Asia/Tokyo, UTC+9)
  - 美国东部时间 (America/New_York)
  - 美国太平洋时间 (America/Los_Angeles)
  - 欧洲西部时间 (Europe/London)
  - 欧洲中部时间 (Europe/Berlin)
- 跨设备固定统一时区（如北京时间）后，无论设备位于何种系统时区，两端日历打卡方块将 100% 绝对一致。

### 3.2 `HomeViewModel` 响应式时区映射
- `HomeViewModel.uiState` 与 `heatmapData` 将 `noteRepository.getAllNotes()` 与 `preferencesManager.calendarTimeZone` 进行 `combine`；
- 动态通过 `getResolvedZoneId(calendarTimeZone)` 准确将 `Instant` 转换为指定时区的 `LocalDate` 进行分组聚合统计。

### 3.3 `ContributionHeatmap` 与 `ActivityCalendarBottomSheet` 动态重绘
- `ContributionHeatmap` 改为 `remember(dailyCounts) { LocalDate.now() }`，每次同步或打卡数据变化时，即时重绘整个 13 周打卡网格；
- `ActivityCalendarBottomSheet` 接收 `timeZoneId`，并以 `allActiveNotes` 作为基准进行月份与打卡渲染。

---

## 4. 涉及修改的文件

- [UserPreferencesManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/preferences/UserPreferencesManager.kt)：新增 `calendarTimeZone` 偏好配置。
- [HomeViewModel.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/HomeViewModel.kt)：响应式结合用户时区计算 `heatmapData`，并在 `uiState` 中暴露 `allActiveNotes` 与 `calendarTimeZone`。
- [HomeScreen.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/HomeScreen.kt)：向 `ActivityCalendarBottomSheet` 传递全量笔记与时区参数。
- [ContributionHeatmap.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/ContributionHeatmap.kt)：解除 `today` 的固定缓存，随 `dailyCounts` 变化动态更新。
- [ActivityCalendarBottomSheet.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/ActivityCalendarBottomSheet.kt)：支持动态时区解析与日期分组。
- [SettingsViewModel.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/settings/SettingsViewModel.kt)：暴露时区配置状态流与设置方法。
- [SettingsScreen.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/settings/SettingsScreen.kt)：提供打卡日历时区选择弹窗与设置入口。

---

## 5. 验证结果

- 运行 `$env:JAVA_HOME = "D:\Android\Android Studio\jbr"; .\gradlew test`，63 个 Task 全部通过，构建状态 `BUILD SUCCESSFUL`。
