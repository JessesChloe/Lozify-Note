# Lozify - Project Working Conventions

## Project Identity

**Project Name:** Lozify  
**Package Namespace:** `com.witte.lozify`  
**Version:** 1.0.0-SNAPSHOT (MVP)  
**Platform:** Android Native  
**Repository:** Local development (no remote yet)

---

## Development Philosophy

### Vibe Coding Collaboration Model

This project follows a **stage-gated progression** model where:
- Each stage has clearly defined deliverables
- No work on Stage N+1 until Stage N is complete and verified
- Documentation is updated after each stage completion
- Every architectural decision is recorded in ADRs

**Key Principle:** *Slow is smooth, smooth is fast.*  
We build incrementally, test thoroughly, and document everything to prevent technical debt.

---

## Working Conventions

### 1. Code Organization (Clean Architecture)

**Layer Rules:**
```
src/main/java/com/witte/lozify/
├── core/               # Shared utilities, zero business logic
│   ├── common/         # Extensions, constants, utilities
│   ├── design/         # Material3 theme, colors, typography
│   ├── database/       # Room database setup, converters
│   └── navigation/     # Navigation graph, routes
│
├── domain/             # ⚠️ PURE KOTLIN ONLY - NO ANDROID IMPORTS
│   ├── model/          # Data classes (Note, Tag, Attachment, etc.)
│   ├── repository/     # Repository interfaces (contracts)
│   └── usecase/        # Business logic use cases
│
├── data/               # Android framework allowed here
│   ├── local/
│   │   ├── entity/     # Room entities
│   │   ├── dao/        # Room DAOs
│   │   └── converter/  # Type converters
│   ├── repository/     # Repository implementations
│   └── mapper/         # Entity ↔ Domain mapping
│
└── presentation/       # Jetpack Compose UI
    ├── main/           # MainActivity
    ├── home/           # Feature modules
    ├── editor/
    ├── drawer/
    └── detail/
```

**Dependency Flow:**  
`Presentation → Domain ← Data`

**Critical Rule:** Domain layer must NEVER import:
- `android.*`
- `androidx.*`
- `kotlinx.android.*`

✅ Domain CAN import:
- `kotlin.*`
- `kotlinx.coroutines.*`
- `java.time.*`

### 2. Dependency Management

**Mandatory:** ALL dependencies MUST be declared in `gradle/libs.versions.toml`

❌ **FORBIDDEN:**
```kotlin
// build.gradle.kts
implementation("androidx.core:core-ktx:1.12.0")  // WRONG - hardcoded version
```

✅ **CORRECT:**
```toml
# gradle/libs.versions.toml
[versions]
core-ktx = "1.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
```

```kotlin
// build.gradle.kts
implementation(libs.androidx.core.ktx)  // Correct - Version Catalog reference
```

### 3. File Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Kotlin Class | PascalCase | `NoteRepository.kt` |
| Composable Function | PascalCase | `NoteCard.kt` |
| ViewModel | Feature + ViewModel | `HomeViewModel.kt` |
| Repository Impl | Interface + Impl | `NoteRepositoryImpl.kt` |
| Entity | Model + Entity | `NoteEntity.kt` |
| DAO | Model + Dao | `NoteDao.kt` |
| Use Case | Action + UseCase | `CreateNoteUseCase.kt` |
| Test | Class + Test | `NoteRepositoryTest.kt` |

### 4. Git Commit Conventions

**Format:** `<type>(<scope>): <description>`

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring (no behavior change)
- `test`: Add/update tests
- `chore`: Build/tooling changes

**Examples:**
```
feat(editor): add #tag blue highlight rendering
fix(database): resolve cascade delete for attachments
docs(ADR): add decision for image storage strategy
refactor(domain): simplify Note model structure
test(repository): add unit tests for NoteRepository
chore(gradle): update Compose BOM to 2024.02.00
```

### 5. Pull Request Rules (Future)

- PR must reference a stage or issue number
- All tests must pass
- No hardcoded versions in build.gradle.kts
- No XML content in Gradle scripts
- ADR updated if architectural change
- Development log updated

### 6. Testing Strategy

**Per Layer:**
- **Domain Layer**: Unit tests with JUnit 5 (pure Kotlin, fast)
- **Data Layer**: Instrumented tests with Room in-memory database
- **Presentation Layer**: Compose UI tests with ComposeTestRule

**Coverage Goals:**
- Domain layer: 80%+ (business logic is critical)
- Data layer: 70%+ (test DAOs and mappers)
- Presentation layer: Manual testing in MVP (automated post-MVP)

### 7. Documentation Requirements

**Mandatory Files:**
- `/docs/PRD.md` - Product requirements
- `/docs/TECH-STACK.md` - Technical documentation
- `/docs/UI-DESIGN.md` - UI specifications
- `/docs/decisions.md` - Architectural decisions (ADRs)
- `/docs/database-migrations.md` - Schema evolution
- `/CLAUDE.md` - This file (working conventions)
- `/development-log/YYYY-MM/YYYY-MM-DD.md` - Daily logs

**Update Triggers:**
- New architectural decision → Add ADR in `decisions.md`
- Database schema change → Update `database-migrations.md`
- Stage completion → Update `CLAUDE.md` stage table
- Daily progress → Create/update development log

### 8. Code Style

**Kotlin:**
- Indentation: 4 spaces (no tabs)
- Line length: 120 characters
- Braces: K&R style (opening brace on same line)
- Imports: No wildcards, sorted alphabetically

**Compose:**
- Composable functions start with uppercase
- Stateless composables preferred
- Use `remember` for computed values
- Use `derivedStateOf` for derived state

**Comments:**
- Only comment "why", never "what"
- No TODO comments (use GitHub issues instead)
### 9. 核心架构避坑指南与铁律 (Pitfalls & Lessons Learned)

#### 1. 分布式同步中的“死灰复燃 (Resurrection)”陷阱
* **踩坑历史**: 同步引擎若仅查询本地活跃笔记（`WHERE is_deleted = 0`），本地软删除的卡片在本地被过滤掉，但拉取云端历史 Payload 时因在本地活跃集找不到，会被误判为“远端新笔记”再次重新插入，导致已删除卡片死灰复燃！
* **架构铁律**: 
  - **全状态加载**: 同步合并比对时，**必须加载包含软删除（Tombstones）在内的全量实体** (`getAllNotesWithRelationsIncludingDeleted()`)；
  - **LWW 决策原则**: 本地软删除笔记带有最新的 `updatedAt` 时间戳，本地删除状态绝对胜出（`localUpdatedAt >= remoteUpdatedAt`），并回写 `isDeleted: true` 覆盖云端旧数据；
  - **全端同步**: 远端拉取到 `isDeleted: true` 且时间戳较新的笔记时，本地必须同步执行软删除将其送入本地回收站，绝不作为新笔记复活。

#### 2. 字段语义错位与“回收站失联”陷阱
* **踩坑历史**: 首页卡片删除标记的是 `is_deleted = 1`，而回收站 DAO 查询若误写为 `is_archived = 1 AND is_deleted = 0`，会导致被删除的卡片从首页消失后在回收站也看不见。
* **架构铁律**:
  - **回收站查询准则**: 统一使用 `WHERE is_deleted = 1 OR is_archived = 1` 兼容历史归档与软删除；
  - **恢复操作准则**: 从回收站恢复卡片时，必须同时将 `is_deleted = 0` 与 `is_archived = 0` 清零，并将 `updatedAt` 更新为当前最新时间，以便下次同步恢复到所有设备。

#### 3. 自然语言文本中的 URL 贪婪截断陷阱
* **踩坑历史**: 用户在正文中输入“项目地址是 https://github.com/xxx。欢迎体验！”，URL 正则如果把句尾的中文句号 `。` 或括号 `)` 贪婪吞入，会导致点击链接跳转时报 404 错误。
* **架构铁律**:
  - **末尾标点净化**: 解析 URL 时必须配合 `cleanUrlMatch` 截断函数，过滤字符集 `.,!?:;，。！？；：()[]{}<>）】\"'“”‘’`；
  - **标签防误判隔离**: URL 内部携带的 `#anchor` 锚点（如 `https://site.com/docs#intro`）必须优先由 URL 正则识别，严禁误提取为 `#tag` 胶囊。

#### 4. RichText 内联点击与卡片手势穿透陷阱
* **踩坑历史**: 卡片内如果直接使用全局 ClickableText 可能会拦截内联胶囊标签 (`InlineTextContent`) 的点击或卡片展开/收起手势。
* **架构铁律**:
  - **偏移量精确命中**: 使用 `TextLayoutResult.getOffsetForPosition(offset)` 结合 `RichTextUtils.getUrlAtOffset()`，仅在用户精确点击在蓝色链接字符上时才消费事件并唤起浏览器；点击其他文本区域保持原有卡片手势不受干扰。

---

## Stage Progression Tracker

### Stage Table

| Stage | Name | Status | Start Date | End Date | Deliverables |
|-------|------|--------|------------|----------|--------------|
| **0** | Foundation | ✅ Completed | 2026-08-07 | 2026-08-07 | Documentation, Gradle structure, package hierarchy |
| **1** | Data Layer | ✅ Completed | 2026-08-07 | 2026-08-07 | Room entities, DAOs, repositories, domain models, Gradle build fix |
| **2** | Basic Display | ✅ Completed | 2026-08-07 | 2026-08-07 | NoteCard, HomeScreen, FAB, expand/collapse interaction |
| **3** | Editor Foundation | ✅ Completed | 2026-08-07 | 2026-08-07 | ViewModel, ModalBottomSheet editor, Room integration, EmptyState |
| **4** | Tag System | ✅ Completed | 2026-08-08 | 2026-08-08 | #tag parsing, blue highlight, tag extraction, database integration, filter bug fix |
| **5** | Tag Filtering | ✅ Completed | 2026-08-08 | 2026-08-08 | Real-time search, tag filter bar, pin/edit/delete operations |
| **6** | Multi-Image | ✅ Completed | 2026-08-08 | 2026-08-10 | Image picker, compression, 3-column grid, AttachmentEntity integration, OOM fix, nested layout fix |
| **7** | Rich Formatting | ✅ Completed | 2026-08-10 | 2026-08-10 | Bold, underline, highlight, interactive checkboxes, AnnotatedString rendering, multiline regex fix |
| **8** | @Relations | ✅ Completed | 2026-08-10 | 2026-08-11 | @mention parsing, NoteRelationEntity, bidirectional links, relation tracing UI, scroll+highlight interaction |
| **9** | Card Operations | ✅ Completed | 2026-08-11 | 2026-08-12 | SwipeToDismiss gestures, pin/archive status management, data layer refactor |
| **10** | Archive Screen | ✅ Completed | 2026-08-12 | 2026-08-12 | Archive screen UI, navigation system, restore/delete operations |
| **11** | Polish & Navigation | ✅ Completed | 2026-08-12 | 2026-08-12 | Empty states, error handling, drawer navigation integration |
| **12** | Trash Rename & Tag Ops | ✅ Completed | 2026-08-12 | 2026-08-12 | Tag operations UI integration and trash rename |
| **13** | Schema Migration & Media | ✅ Completed | 2026-08-17 | 2026-08-17 | Inline tag capsules, responsive AttachmentGrid, thread timeline view |
| **14** | Rich Card Menu & Stats | ✅ Completed | 2026-08-17 | 2026-08-17 | Flomo card dropdown menu, drawer achievement stats panel |
| **15** | Heatmap & Input Filter | ✅ Completed | 2026-08-17 | 2026-08-18 | Contribution heatmap, smart input filter with atomic deletion, marker fading |
| **16** | Media Lightbox & Backup | ✅ Completed | 2026-08-18 | 2026-08-18 | Pinned tags, fullscreen image lightbox, search highlight, ZIP backup/restore |
| **17** | Editor Toolbar & Settings | ✅ Completed | 2026-08-18 | 2026-08-18 | Flomo-style editor toolbar, settings center, note collapse preferences |
| **18** | Visual Alignment | ✅ Completed | 2026-08-18 | 2026-08-18 | 1:1 Flomo typography, borderless cards, 85% drawer width, relation lines |
| **19** | Zero-Delay & Keyboard | ✅ Completed | 2026-08-18 | 2026-08-18 | TopBar seamless gray, smooth keyboard sync, instant scroll-to-top on save |
| **20** | Build Modernization | ✅ Completed | 2026-08-18 | 2026-08-18 | Daemon JVM criteria migration, compileSdk 35 compatibility |
| **21** | Stream Sorting & ZIP Detect | ✅ Completed | 2026-08-18 | 2026-08-18 | TopBar sorting menu, dynamic tag title, auto-detecting Markdown ZIP restore |
| **22** | Flomo Heatmap & Calendar | ✅ Completed | 2026-08-18 | 2026-08-18 | 13-column chunky heatmap, bold stats panel, ActivityCalendarBottomSheet |
| **23** | Clean Brand Logo | ✅ Completed | 2026-08-18 | 2026-08-18 | Clean green L-pen logo without outer ring, full Android mipmaps |
| **24** | Scrolling Performance | ✅ Completed | 2026-08-19 | 2026-08-19 | Precompiled static regexes, LRU cache, onTextLayout fix, lazy menu mounting |
| **25** | Vector Logotype & TopBar | ✅ Completed | 2026-08-19 | 2026-08-19 | Geometric lozify vector brand logotype, two-line minimalist hamburger menu |
| **26** | WebDAV / Jianguoyun Sync | ✅ Completed | 2026-08-19 | 2026-08-19 | Two-way LWW merge, delta image sync, Jianguoyun preset & sync screen |
| **27** | WebDAV E2EE & Anti-Censorship | ✅ Completed | 2026-08-19 | 2026-08-19 | AES-256-GCM hardware encryption, PBKDF2 KDF, anti-censorship .enc sync |
| **28** | Gitee / GitHub Update Push | ✅ Completed | 2026-08-19 | 2026-08-19 | Gitee primary + GitHub fallback dual-channel update check, AppUpdateDialog |
| **29** | Pull-to-Sync & Fast-Path | ✅ Completed | 2026-08-19 | 2026-08-19 | Elastic notes count header, in-place sync state transition, manifest-first zero-traffic fast-path |
| **30** | Distributed Global UUID Sync | ✅ Completed | 2026-08-19 | 2026-08-19 | Universal syncId entity matching, auto-increment collision elimination, self-healing legacy notes |
| **31** | Anti-Duplication Fingerprint | ✅ Completed | 2026-08-19 | 2026-08-19 | Dual-fingerprint (createdAt+content) cross-version deduplication & historical clean-up (v1.2.2) |
| **32** | Pull Gesture & Cloud Purging | ✅ Completed | 2026-08-19 | 2026-08-19 | Ultra-responsive pull physics (40px, onPostScroll), cloud payload pre-filtering & clean-up |
| **33** | URL Link Recognition & Tap | ✅ Completed | 2026-08-19 | 2026-08-19 | Hyperlink Blue (#2563EB), punctuation trimming, browser launch, tag isolation |
| **34** | Release v1.2.3 Distribution | ✅ Completed | 2026-08-19 | 2026-08-19 | Lozify v1.2.3 release package, dual-channel update announcement, end-to-end verified |
| **35** | Trash Lifecycle & Deletion Sync | ✅ Completed | 2026-08-19 | 2026-08-19 | Soft-delete to Trash display fix, full-state LWW sync preventing resurrection |
| **36** | Pull Damping & Trash Actions | ✅ Completed | 2026-08-19 | 2026-08-19 | 75dp pull resistance against accidental sync, Recycle Bin long-press action sheet & delete confirmation |

**Status Legend:**
- ⚪ Not Started
- 🟡 In Progress
- ✅ Completed
- ⚠️ Blocked

### Stage Gate Checklist

Before marking a stage as complete, verify:
- [ ] All code compiles without errors/warnings
- [ ] Stage-specific tests pass (unit tests for data layer, manual UI tests for presentation)
- [ ] No hardcoded versions in build.gradle.kts
- [ ] No Android imports in domain layer
- [ ] Stage table updated in CLAUDE.md
- [ ] Development log entry created/updated
- [ ] ADRs added if new architectural decisions made
- [ ] Gradle sync succeeds

---

## Critical Constraints (Zero Tolerance)

### 1. Version Management
❌ **FORBIDDEN:** Hardcoded dependency versions in `.gradle.kts` files  
✅ **REQUIRED:** All versions in `gradle/libs.versions.toml`

### 2. Script Purity
❌ **FORBIDDEN:** XML content (AndroidManifest tags, layout XML) in `.gradle.kts` files  
✅ **REQUIRED:** Gradle scripts contain only Kotlin DSL, XML in `AndroidManifest.xml`

### 3. Domain Layer Isolation
❌ **FORBIDDEN:** `android.*` or `androidx.*` imports in `domain/` package  
✅ **REQUIRED:** Pure Kotlin with zero Android framework dependencies

### 4. Package Namespace
❌ **FORBIDDEN:** `com.flomo.*` or references to original commercial software  
✅ **REQUIRED:** `com.witte.lozify` consistently throughout codebase

### 5. Database Migrations
❌ **FORBIDDEN:** `fallbackToDestructiveMigration()` in production builds  
✅ **REQUIRED:** Explicit `Migration(from, to)` implementations for all schema changes

---

## 全局核心铁律 (Global Core Principles)

### 铁律一：中文与技术术语双语规范

**沟通与文档语言：**
1. **与用户交流**：始终使用**简体中文**进行沟通、阶段总结汇报、问题讨论
2. **项目文档**：所有新增及更新的项目文档（`/docs/`、`/development-log/`、`CLAUDE.md` 等）默认使用**简体中文**编写
3. **工作交接格式**：每个 Stage 完成后的汇报必须使用**简体中文**

**技术术语纯正性：**
- ✅ **保持英文**：Android/Kotlin 核心架构术语必须保持官方英文，严禁翻译为生硬中文
  - Clean Architecture, MVVM
  - Repository, ViewModel, Entity, DAO, UseCase, Mapper
  - Jetpack Compose, Navigation, Room Database, Hilt
  - Coroutine, Flow, StateFlow
- ✅ **代码规范**：所有包名、类名、变量名、函数名及标准代码注释保持**全英文**书写
- ✅ **中英穿插**：在中文文档中采用"中文业务说明 + 英文技术命名"格式
  - 示例："通过 Repository 处理 Entity 与 Domain Model 的互转"
  - 示例："使用 Flow 实现响应式数据流，ViewModel 监听数据库变化"

### 铁律二：真实文件原则 (Real File Principle)

**从 Stage 1 开始，所有开发与汇报必须建立在工作区真实且合法写入的文件之上！**

**严格禁止事项：**
1. ❌ **禁止假设与推测**：不得假设文件已创建、不得推测代码已生成、不得"仅输出执行计划"而不真正将代码写至硬盘
2. ❌ **禁止浮于表面**：不得只在对话框回复中展示代码片段、不得只生成 Diff 差异展示、不得仅提供示例代码而不实际写入文件
3. ❌ **禁止虚假汇报**：不得在文件尚未写入磁盘的情况下汇报"已完成"

**强制执行要求：**
1. ✅ **实际写入**：必须使用 Write/Edit 工具将完整代码真正写入对应的源文件
2. ✅ **眼见为实**：任何完成汇报都必须以真正成功写入磁盘的文件及路径为准
3. ✅ **文件清单**：每个 Stage 完成汇报时，必须列出所有真实创建/修改的文件路径清单
4. ✅ **可验证性**：用户必须能够在工作区中直接打开并查看所有汇报的文件

**违反后果：**
- 如果一个文件不存在于磁盘，则该文件相关的功能视为**未完成**
- 如果汇报中包含未真实写入的文件，则该 Stage 视为**未通过验证**

---

## Quick Reference Commands

### Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Database
```bash
# Export Room schema (after KSP configuration)
./gradlew kspDebugKotlin

# View database on device (requires root or debuggable app)
adb shell
run-as com.witte.lozify
ls databases/
```

### Useful ADB Commands
```bash
# Clear app data (reset database)
adb shell pm clear com.witte.lozify

# View app logs
adb logcat | grep Lozify

# Take screenshot
adb exec-out screencap -p > screenshot.png
```

---

## Communication Protocol

### When to Update This File

**Stage Table:**
- Update status when starting/completing a stage
- Add start date when beginning work
- Add end date when stage gate checklist passes

**Working Conventions:**
- Add new sections if team conventions evolve
- Update constraints if new "zero tolerance" rules emerge

**Do NOT:**
- Add temporary notes (use development log instead)
- Track daily progress here (use `/development-log/` instead)
- Document code-level details (use inline comments instead)

### Decision Escalation

**Architectural Decisions:**
1. Identify decision point (e.g., "Should we use sealed classes for Note types?")
2. Research alternatives
3. Draft ADR in `docs/decisions.md`
4. Implement chosen approach
5. Update TECH-STACK.md if major technology change

**Scope Changes:**
1. Evaluate impact on stage roadmap
2. Decide: defer to post-MVP or adjust stages
3. Update PRD.md with decision
4. Notify in development log

---

## Stage 0 Completion Criteria

**Must Complete Before Stage 1:**
- [x] Create all 5 documentation files in `/docs/`
- [x] Initialize development log structure
- [x] Create this CLAUDE.md file
- [ ] Build `gradle/libs.versions.toml` with all MVP dependencies
- [ ] Create Android Gradle project structure (settings.gradle.kts, app/build.gradle.kts, root build.gradle.kts)
- [ ] Initialize package hierarchy (`core/`, `data/`, `domain/`, `presentation/`)
- [ ] Add placeholder `.gitkeep` or empty Kotlin files to maintain directory structure
- [ ] Run Gradle sync without errors
- [ ] Update Stage Table (mark Stage 0 as ✅ Completed)
- [ ] Final development log entry for Stage 0

**Success Metrics:**
- `./gradlew tasks` executes successfully
- Android Studio recognizes project as valid Android application
- All dependencies resolve from Version Catalog
- No XML in Gradle scripts
- Package structure visible in IDE

---

## Resources

**Official Documentation:**
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Material3 Design System](https://m3.material.io/)
- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)

**Community Resources:**
- [Android Developers on Reddit](https://reddit.com/r/androiddev)
- [Kotlin Slack](https://kotlinlang.slack.com)
- [Stack Overflow - Android Tag](https://stackoverflow.com/questions/tagged/android)

**Learning Path:**
- Start with `/docs/PRD.md` (understand product)
- Read `/docs/TECH-STACK.md` (understand architecture)
- Review `/docs/UI-DESIGN.md` (understand design system)
- **Read `/docs/compose-guidelines.md` (Compose best practices & icon constraints)**
- Follow Stage Table in order (build incrementally)

---

- **2026-08-19**: Stage 36 下拉同步阻尼阈值优化与回收站长按确认交互重构
  - 下拉触发阈值升级至 75dp 并加入 0.55f 弹性阻尼，杜绝正常滑动浏览时的误触发
  - 回收站废弃左右滑动，重构为卡片长按/轻触操作面板（还原笔记/彻底删除）及二次确认弹窗
- **2026-08-19**: Stage 35 回收站闭环与防死灰复燃同步修复
  - 修复首页软删除卡片未进入回收站问题 (`WHERE is_deleted = 1 OR is_archived = 1`)
  - 修复下拉同步时已删除笔记死灰复燃 Bug，实现全状态 LWW 同步合并
- **2026-08-19**: Stage 34 Lozify v1.2.3 正式版本发布
  - 版本号升级为 `v1.2.3` (`versionCode = 5`)，同步更新 `version.json`
- **2026-08-19**: Stage 33 智能网络链接识别与超链接蓝标
  - 自动识别 http/https/www 链接并以 `#2563EB` 标蓝与下划线渲染
  - 实现中英文末尾标点智能截断与卡片点击一键唤起系统浏览器
- **2026-08-19**: Stage 32 首页下拉刷新手势阻尼优化与云端预清理
- **2026-08-19**: Stage 31 双指纹防重引擎与 v1.2.2 自动更新推送
- **2026-08-19**: Stage 30 分布式全局 UUID (syncId) 实体同步系统
- **2026-08-12**: Stage 10 归档箱与导航系统开发
  - 创建 ArchiveScreen UI 页面，支持恢复/永久删除操作
  - 建立 Navigation 系统（Routes + NavGraph + NavController）
  - 在 HomeScreen 顶部添加归档箱入口按钮
  - DAO 新增 `getArchivedNotesWithRelations()` 查询方法
  - 创建 ArchiveViewModel 管理归档笔记状态
  - 更新 Compose 规范文档，补充 Refresh 图标和语义映射表
- **2026-08-12**: Stage 9 滑动交互开发
  - 添加 `/docs/compose-guidelines.md` Compose 开发规范文档
  - 记录图标库使用约束（仅核心包，禁用扩展包图标）
  - 记录 Modifier 扩展函数 import 规范
  - 记录 SwipeToDismissBox 使用规范
- **2026-08-07**: Initial CLAUDE.md created for Stage 0
  - Defined Clean Architecture layer rules
  - Established Version Catalog mandate
  - Created 10-stage roadmap
  - Documented critical constraints (zero tolerance rules)
  - Set stage gate checklist

---

**Last Updated:** 2026-08-19  
**Current Stage:** Stage 35 (Trash Lifecycle & Deletion Sync) - ✅ Completed  
**Next Milestone:** Next user requirements / feature enhancements
