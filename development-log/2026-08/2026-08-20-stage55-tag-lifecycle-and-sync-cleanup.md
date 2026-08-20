# Lozify 开发日志 - 2026-08-20 (Stage 55)

## 📌 阶段概述
* **阶段编号**: Stage 55
* **阶段主题**: 修复删除标签与笔记后多端同步侧边栏残留孤立标签 Bug (Tag Lifecycle & Multi-Device Sync Cleanup)
* **开发日期**: 2026-08-20
* **完成状态**: ✅ 已完成 (Build & 83/83 Unit Tests Passed)

---

## 🔍 问题分析与根因

### 1. 现象
* 手机 A 在侧边栏删除了标签及该标签下的所有笔记，并同步上传云端；
* 手机 B 拉取同步后，卡片虽然被成功删除了，但**侧边栏的标签列表里依然残留着该标签**。

### 2. 根因
1. **数据层孤立实体未清理**：
   * 手机 A 删除标签时，在本地数据库直接调用了 `tagDao.deleteTag(tag)`，但 WebDAV 同步协议此前只向云端上传了剩余活跃标签清单；
   * 手机 B 同步时，卡片被移入回收站或彻底删除，导致该标签在手机 B 的实际活跃关联卡片计数变成了 0（`usage_count = 0`）；
   * 但手机 B 的 SQLite `tags` 表中该标签实体行并未被删除；
2. **侧边栏展示与查询未过滤 0 引用孤立标签**：
   * `TagDao.getAllTags()` 查询的是整个 `tags` 表，虽然 `usage_count` 准确返回为 0，但 `HomeViewModel` 的 `uiState.allTags` 将这些 0 引用的孤立非置顶标签全部展示在了侧边栏；
   * `WebDavSyncManager` 在 Stage 3 合并后与 Stage 5 上传前未触发孤立标签清理机制。

---

## 🛠️ 解决方案与架构重构

1. **[TagDao.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/local/dao/TagDao.kt)**：
   * 新增 `cleanupOrphanedTags()` SQL 方法：
     ```sql
     DELETE FROM tags
     WHERE is_pinned = 0
     AND id NOT IN (
         SELECT DISTINCT tag_id
         FROM note_tag_cross_ref
         INNER JOIN notes ON note_tag_cross_ref.note_id = notes.id
         WHERE notes.is_deleted = 0
     )
     ```
     精确清理所有非置顶且无任何活跃卡片引用的孤立标签。
2. **[HomeViewModel.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/HomeViewModel.kt)**：
   * `allTags` 过滤为 `it.usageCount > 0 || it.isPinned`，当卡片被删除或移入回收站时，侧边栏标签实时自动隐去；
   * 统计面板中的标签总数同步精确联动。
3. **[WebDavSyncManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/network/webdav/WebDavSyncManager.kt)**：
   * Stage 1 Fast-Path 引入 `remoteTagCount == activeLocalTagsCount` 校验；
   * Stage 3 笔记合并完成后立即执行 `cleanupOrphanedTags()`；
   * Stage 5 云端上传前执行 `cleanupOrphanedTags()` 并过滤有效标签写入 `notes_payload.json` 与 `manifest.json`。
4. **[NoteRepositoryImpl.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/repository/NoteRepositoryImpl.kt)**：
   * `hardDeleteNote` 与 `emptyTrash` 本地执行时级联触发 `cleanupOrphanedTags()`；
   * `restoreNote` 从回收站恢复卡片时，自动解析正文中的 `#tag` 并安全重建关联。

---

## 🧪 验证与打包信息

* **全量单元测试**: **83/83 项全量通过** (`BUILD SUCCESSFUL in 44s`)
* **构建产物**: [app-debug.apk](file:///d:/Code/Lozify/app/build/outputs/apk/debug/app-debug.apk) / [Lozify-v1.2.5.apk](file:///d:/Code/Lozify/Lozify-v1.2.5.apk)
