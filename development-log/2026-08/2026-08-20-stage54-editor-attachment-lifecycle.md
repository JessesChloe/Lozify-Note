# Lozify 开发日志 - 2026-08-20 (Stage 54)

## 📌 阶段概述
* **阶段编号**: Stage 54
* **阶段主题**: 修复编辑卡片时已有附件回显、追加/删除及数据覆写丢失 Bug (Editor Attachment Lifecycle & Persistence Fix)
* **开发日期**: 2026-08-20
* **完成状态**: ✅ 已完成 (Build & 83/83 Unit Tests Passed)

---

## 🔍 问题根因深度剖析

### 1. 现象
* 编辑已有卡片时，弹出的编辑器里看不到原有卡片的图片与附件；
* 在编辑时新添加图片/附件保存后，主页卡片上的新旧图片附件全部丢失。

### 2. 根因
1. **UI 展现层缺失已有附件上下文**：
   * [HomeScreen.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/HomeScreen.kt) 触发编辑时只传了 `note.id` 和 `note.content`，未传 `note.attachments`；
   * [NoteEditorBottomSheet.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/editor/NoteEditorBottomSheet.kt) 未初始化并渲染已有附件，导致图片/文件列表只显示新选择的 URI。
2. **数据层存在破坏性盲删逻辑（Fatal Data Wipe）**：
   * [NoteRepositoryImpl.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/repository/NoteRepositoryImpl.kt) 的 `updateNote` 方法中存在历史遗留的 `attachmentDao.deleteAllAttachmentsForNote(note.id)`；
   * [EditorViewModel.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/editor/EditorViewModel.kt) 在编辑保存时先调用了 `attachmentRepository.addImageAttachment(...)` 将新附件写入数据库，紧接着调用了 `noteRepository.updateNote(...)`，从而把刚刚写入的新旧附件全部抹除。

---

## 🛠️ 解决方案与架构重构

1. **[NoteEditorBottomSheet.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/editor/NoteEditorBottomSheet.kt)**：
   * 新增 `initialAttachments: List<Attachment>` 参数并维护 `existingAttachments` 状态；
   * 细分为已有图片/已有文件与新增图片/新增文件列表，支持已有图片/文件缩略图展示、右上角一键移除 (`Close` 图标)；
   * 保存回调更新为 `onSave(textFieldValue, keptAttachmentIds, imageUris, fileUris)`。
2. **[EditorViewModel.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/editor/EditorViewModel.kt)**：
   * `saveNote` 接收 `keptExistingAttachmentIds`；
   * 对比数据库当前附件，仅删除用户主动移除的附件；
   * 追加新选中的图片与文件，并安全更新正文与标签。
3. **[NoteRepositoryImpl.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/data/repository/NoteRepositoryImpl.kt)**：
   * 移除 `updateNote` 中的破坏性 `attachmentDao.deleteAllAttachmentsForNote`，附件全生命周期统一交由 `AttachmentRepository` 负责，彻底杜绝数据擦除。
4. **[HomeScreen.kt](file:///d:/Code/Lozify/app/src/main/java/com/witte/lozify/presentation/home/HomeScreen.kt)**：
   * `onEditClick` 注入 `editingNoteAttachments = note.attachments`，并在 `onDismiss` 时安全重置。

---

## 🧪 验证与打包信息

* **全量单元测试**: **83/83 项全量通过** (`BUILD SUCCESSFUL in 54s`)
* **构建产物**: [app-debug.apk](file:///d:/Code/Lozify/app/build/outputs/apk/debug/app-debug.apk) (v1.2.5)
