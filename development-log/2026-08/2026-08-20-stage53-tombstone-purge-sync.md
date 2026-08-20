# Lozify 开发日志 - 2026-08-20 (Stage 53)

## 📌 阶段概述
* **阶段编号**: Stage 53
* **阶段主题**: 回收站彻底删除与清空跨端墓碑同步协议 (Tombstone Purge Synchronization Protocol)
* **开发日期**: 2026-08-20
* **完成状态**: ✅ 已完成 (Build & 83/83 Unit Tests Passed)

---

## 🔍 问题分析与解决方案

### 1. 痛点分析
* **现象**：手机 A 清空了回收站，同步后手机 B 的回收站没有清空；甚至手机 A 再次同步时，已被清空的笔记又从云端被拉了回来（死灰复燃）。
* **根因**：
  * 此前「放入回收站（软删除）」通过 `is_deleted = 1` 可以跨端同步；
  * 但「清空回收站/彻底删除」在本地直接执行了物理 `DELETE`，云端 WebDAV 的 payload 依然残留着这些已删除笔记；
  * 同步引擎在双向合并时，发现“云端有、本地没有”，误判为是其他端新增的数据，从而将其再次拉回本地。

### 2. 解决方案：墓碑销毁同步协议 (Tombstone Purge Protocol)
1. **本地销毁清单记录**：
   * 在 [UserPreferencesManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/preferences/UserPreferencesManager.kt) 中维护 `purgedSyncIds` 集合；
   * 当调用 `hardDeleteNote` 或 `emptyTrash` 时，自动将其 `syncId` 记录进销毁墓碑清单中；
2. **云端增量维护销毁墓碑**：
   * 云端 `notes_payload.json` 同步持久化 `purgedSyncIds` 字段；
   * 在 [WebDavSyncManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/network/webdav/WebDavSyncManager.kt) 的 Stage 3 中双向合并多端墓碑清单；
3. **多端级联同步物理抹除**：
   * 当任意设备同步时，检测到本地存在处于 `allPurgedSyncIds` 中的笔记（无论处于活跃还是回收站），**直接从本地数据库物理删除，并同步删除本地磁盘上的多媒体附件文件**；
   * 合并远程笔记时，跳过所有处于 `allPurgedSyncIds` 中的笔记，**彻底终结死灰复燃**；
   * 在 Stage 5 回写云端时剔除已销毁笔记，保证云端 payload 干净轻量；
   * 在 `manifest.json` 中增加 `purgedCount` 校验，确保清空回收站后立即触发跨端同步。

---

## 🛠️ 涉及核心文件

1. **[UserPreferencesManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/preferences/UserPreferencesManager.kt)**：
   * 增加 `purgedSyncIds`、`recordPurgedSyncIds`、`getPurgedSyncIds` 管理持久化墓碑。
2. **[NoteDao.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/local/dao/NoteDao.kt)**：
   * 增加 `getNoteByIdDirect(noteId)`，支持同步获取实体 `syncId`。
3. **[NoteRepositoryImpl.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/repository/NoteRepositoryImpl.kt)**：
   * 在 `hardDeleteNote` 与 `emptyTrash` 中接入 `preferencesManager.recordPurgedSyncIds`。
4. **[WebDavSyncManager.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/core/network/webdav/WebDavSyncManager.kt)**：
   * Stage 3 实现双端墓碑合并、本地级联物理删除与附件清理、远程已销毁笔记跳过；
   * Stage 5 剔除已销毁数据回写云端；
   * `manifest.json` 与 Fast-path 引入 `purgedCount` 校验。
5. **[WebDavSyncTest.kt](file:///d:/Code/Lozify/app/src/test/java/com/witte/lozify/core/network/webdav/WebDavSyncTest.kt)**：
   * 新增 `testTombstonePurgeProtocol_preventsResurrectionAndSyncsEmptyTrash` 单元测试。

---

## 🧪 验证与打包信息

* **全量单元测试**: **83/83 项全量通过** (`BUILD SUCCESSFUL in 40s`)
* **构建产物**: [app-debug.apk](file:///d:/Code/Lozify/app/build/outputs/apk/debug/app-debug.apk) (v1.2.5)
