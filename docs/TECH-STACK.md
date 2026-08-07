# Lozify - Technical Stack Documentation

## 1. Technology Overview

**Project Type:** Android Native Application  
**Package Namespace:** `com.witte.lozify`  
**Minimum SDK:** 26 (Android 8.0 Oreo)  
**Target SDK:** 35 (Android 15)  
**Compile SDK:** 35  
**JVM Target:** 17

## 2. Core Technologies

### 2.1 Programming Language
- **Kotlin** 2.0.20
  - Null safety by default
  - Coroutines for async operations
  - Flow for reactive streams
  - Extension functions for clean APIs
  - New Compose Compiler Plugin architecture

### 2.2 UI Framework
- **Jetpack Compose** (BOM 2024.06.00)
  - Declarative UI paradigm
  - Material3 design system
  - State management via ViewModel
  - Navigation Compose for screen transitions

**Key Compose Libraries:**
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`
- `androidx.compose.ui:ui-tooling` (debug preview)
- `androidx.navigation:navigation-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`

### 2.3 Architecture Pattern
**Clean Architecture + MVVM**

**Layer Responsibilities:**
```
┌─────────────────────────────────────────┐
│  Presentation Layer (UI + ViewModels)  │
│  - Jetpack Compose screens             │
│  - ViewModels with StateFlow           │
│  - UI state management                 │
└──────────────┬──────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────┐
│  Domain Layer (Pure Kotlin)             │
│  - Use cases (business logic)           │
│  - Domain models                        │
│  - Repository interfaces                │
└──────────────┬──────────────────────────┘
               │ depends on
┌──────────────▼──────────────────────────┐
│  Data Layer (Android Framework)         │
│  - Room database                        │
│  - Repository implementations           │
│  - Data entities & DAOs                 │
│  - Mappers (Entity ↔ Domain)            │
└─────────────────────────────────────────┘
```

**Benefits:**
- Clear separation of concerns
- Testable business logic (domain layer has zero Android dependencies)
- Easy to swap implementations
- Scalable for future features

### 2.4 Database
- **Room Database** 2.6.0+ with **KSP** (not KAPT)
  - Type-safe SQL queries
  - Compile-time verification
  - Automatic migration support
  - Coroutines + Flow integration

**Room Components:**
- `@Database` annotation for database class
- `@Entity` for table definitions
- `@Dao` for data access interfaces
- Type converters for custom types (Instant, enums)

**Why KSP over KAPT:**
- 2x faster compilation
- Better IDE integration
- KAPT is deprecated for Room 2.6+

### 2.5 Dependency Injection
- **Hilt** (Dagger wrapper for Android)
  - Compile-time DI container
  - Automatic ViewModel injection
  - Scoped dependencies (Singleton, ViewModelScoped)
  - Test-friendly

**Key Annotations:**
- `@HiltAndroidApp` for Application class
- `@AndroidEntryPoint` for Activities/Fragments
- `@Inject` for constructor injection
- `@Module` + `@InstallIn` for providing dependencies

### 2.6 Asynchronous Programming
- **Kotlin Coroutines** 1.7.3+
  - `suspend` functions for async operations
  - `CoroutineScope` for lifecycle management
  - `Dispatchers.IO` for database/file operations
  - `Dispatchers.Main` for UI updates

- **Flow** (reactive streams)
  - `StateFlow` for UI state in ViewModels
  - `Flow` for database queries (Room integration)
  - Operators: `map`, `filter`, `combine`, `flatMapLatest`

### 2.7 Image Loading
- **Coil** 2.5.0+
  - Kotlin-first image loader
  - Coroutine-based
  - Compose integration via `AsyncImage`
  - Automatic memory/disk caching
  - Image transformations (resize, crop, compress)

**Usage:**
```kotlin
AsyncImage(
    model = imageUri,
    contentDescription = null,
    modifier = Modifier.size(120.dp),
    contentScale = ContentScale.Crop
)
```

## 3. Build System

### 3.1 Gradle Configuration
- **Gradle Version:** 8.2+
- **Android Gradle Plugin:** 8.2.0+
- **Build Script Language:** Kotlin DSL (`.gradle.kts`)

### 3.2 Version Catalog (gradle/libs.versions.toml)
**Mandatory centralized dependency management**

**Structure:**
```toml
[versions]
kotlin = "1.9.20"
compose-bom = "2024.01.00"
room = "2.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**Benefits:**
- Single source of truth for versions
- Type-safe dependency references in build scripts
- Easy to update versions across modules
- IDE autocomplete support

**Reference in build.gradle.kts:**
```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
}
```

## 4. Database Schema Design

### 4.1 Core Tables

**NoteEntity**
```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false  // Soft delete
)
```

**TagEntity**
```kotlin
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String  // Color managed by Compose theme, not stored
)
```

**NoteTagCrossRef** (Many-to-Many)
```kotlin
@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [...]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long
)
```

**AttachmentEntity** (One-to-Many)
```kotlin
@Entity(
    tableName = "attachments",
    foreignKeys = [...]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val uri: String,        // Internal app storage path
    val type: String,       // "image/jpeg", "image/png"
    val order: Int,         // Display order in grid
    val createdAt: Instant
)
```

**NoteRelationEntity** (@Mention relations)
```kotlin
@Entity(tableName = "note_relations")
data class NoteRelationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromNoteId: Long,
    val toNoteId: Long,
    val mentionText: String  // "@NoteName" for display
)
```

### 4.2 Type Converters
```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
```

## 5. Key Design Decisions

### 5.1 Why Clean Architecture?
- **Domain isolation**: Business logic has no Android dependencies → easier testing
- **Flexibility**: Can swap Room for another database without touching domain layer
- **Scalability**: Clear boundaries make it easy to add features without spaghetti code

### 5.2 Why Jetpack Compose over XML Views?
- **Less code**: Declarative UI reduces boilerplate by ~40%
- **Type safety**: No more `findViewById()` casting errors
- **Live previews**: See changes instantly without rebuilding
- **Modern standard**: Google's recommended approach since 2021

### 5.3 Why Room + KSP?
- **Type safety**: Queries verified at compile time
- **Coroutines-first**: Native `suspend` function support
- **KSP performance**: 2x faster than KAPT
- **Migration safety**: Room enforces schema version management

### 5.4 Why Hilt over Manual DI?
- **Less boilerplate**: No manual singleton management
- **Lifecycle awareness**: Automatic ViewModel injection
- **Testability**: Built-in test harness for overriding dependencies
- **Android-optimized**: Scopes tied to Activity/Fragment lifecycle

### 5.5 Image Storage Strategy
**Problem:** If we store gallery image URIs directly, deleting the original breaks the note.

**Solution:**
1. User selects images from gallery
2. Copy images to `context.filesDir/images/` (app private storage)
3. Compress images (target <500KB per image using Coil)
4. Store only internal file paths in `AttachmentEntity`
5. On app uninstall, images auto-deleted with app data

**Benefits:**
- Immune to external file deletion
- Full control over image lifecycle
- Compression reduces storage footprint

## 6. Development Tools

### 6.1 IDE
- **Android Studio** Hedgehog (2023.1.1) or later
- **Required plugins:** Kotlin, Compose preview

### 6.2 Testing Framework (Future)
- **Unit Tests:** JUnit 5 + MockK
- **Instrumented Tests:** Espresso + Compose UI Testing
- **Architecture Tests:** Room schema validation

### 6.3 Code Quality
- **Linting:** Android Lint (baseline file for legacy issues)
- **Formatting:** ktlint or IntelliJ default Kotlin style
- **Static Analysis:** Detekt (optional for complex projects)

## 7. Performance Considerations

### 7.1 Database Optimization
- Index on `NoteEntity.createdAt` for fast sorting
- Index on `TagEntity.name` for quick tag lookups
- Use `Flow<List<T>>` for reactive UI updates (automatic refresh)

### 7.2 Image Loading
- Coil's automatic memory cache (LRU strategy)
- Disk cache for thumbnails
- Lazy loading in LazyColumn (only visible cards load images)

### 7.3 Compose Performance
- Use `remember` to cache computed values
- Use `derivedStateOf` for derived UI state
- Avoid recomposition with `key()` in LazyColumn items

## 8. Future Enhancements (Post-MVP)

### 8.1 Search Optimization
- Implement Room FTS4 (Full-Text Search) virtual table
- Index `NoteEntity.content` for fast `MATCH` queries
- Support Chinese word segmentation

### 8.2 Cloud Sync
- Leverage `updatedAt` and `isDeleted` for incremental sync
- Conflict resolution strategy (last-write-wins or manual merge)
- Backend: Firebase or custom API

### 8.3 Export/Import
- Export to Markdown (convert #tags and @mentions)
- Backup to local ZIP file
- Import from other note apps

## 9. Dependencies Reference

See [gradle/libs.versions.toml](../gradle/libs.versions.toml) for exact versions.

**Core Libraries:**
- androidx.core:core-ktx
- androidx.lifecycle:lifecycle-runtime-ktx
- androidx.activity:activity-compose
- androidx.compose.material3:material3
- androidx.room:room-runtime + room-ktx
- com.google.dagger:hilt-android
- io.coil-kt:coil-compose
- org.jetbrains.kotlinx:kotlinx-coroutines-android

**Build Tools:**
- com.google.devtools.ksp (Kotlin Symbol Processing)
- com.google.dagger:hilt-android-gradle-plugin

## 10. Anti-Patterns to Avoid

1. ❌ **Hardcoding versions in build.gradle.kts** → Use Version Catalog
2. ❌ **Putting XML in Gradle scripts** → Keep AndroidManifest separate
3. ❌ **Using KAPT for Room** → Use KSP (KAPT is deprecated)
4. ❌ **Mixing UI logic in ViewModel** → Keep ViewModel pure (no Compose code)
5. ❌ **Storing gallery URIs directly** → Copy to app private storage
6. ❌ **Using `fallbackToDestructiveMigration()`** → Write proper migrations

## 11. Migration Strategy

See [database-migrations.md](database-migrations.md) for detailed schema evolution rules.

**Principles:**
- Every schema change → increment database version
- Write explicit `Migration(from, to)` implementations
- Test migrations with Room's MigrationTestHelper
- Never ship destructive migrations to production
