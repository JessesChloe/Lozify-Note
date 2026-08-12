# Lozify - Database Migration Documentation

## Overview

This document tracks all Room database schema migrations for Lozify. Every schema change must be documented here with explicit migration code.

**Critical Rules:**
- ✅ Always increment database version number
- ✅ Write explicit `Migration(from, to)` implementations
- ✅ Test migrations with Room's `MigrationTestHelper`
- ❌ Never use `fallbackToDestructiveMigration()` in production builds
- ❌ Never ship migrations that drop user data without explicit user consent

---

## Database Version History

| Version | Date | Description | ADR Reference |
|---------|------|-------------|---------------|
| 1 | 2026-08-07 | Initial schema (MVP baseline) | ADR-005, ADR-006, ADR-007 |
| 2 | 2026-08-12 | Add indexes for is_pinned and is_archived fields | Stage 9 |

---

## Version 1: Initial Schema (MVP Baseline)

**Date:** 2026-08-07  
**Stage:** Stage 1 (Data Layer)

### Tables Created

#### NoteEntity
```sql
CREATE TABLE IF NOT EXISTS `notes` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `content` TEXT NOT NULL,
    `createdAt` INTEGER NOT NULL,
    `updatedAt` INTEGER NOT NULL,
    `isPinned` INTEGER NOT NULL DEFAULT 0,
    `isDeleted` INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS `index_notes_createdAt` ON `notes` (`createdAt`);
CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON `notes` (`isDeleted`);
```

**Indexes:**
- `createdAt`: For sorting feed by newest first
- `isDeleted`: For filtering out soft-deleted notes

#### TagEntity
```sql
CREATE TABLE IF NOT EXISTS `tags` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`);
```

**Indexes:**
- `name`: For quick tag lookups when parsing #tags

#### NoteTagCrossRef (Many-to-Many Junction)
```sql
CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (
    `noteId` INTEGER NOT NULL,
    `tagId` INTEGER NOT NULL,
    PRIMARY KEY(`noteId`, `tagId`),
    FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`);
CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`);
```

**Foreign Keys:**
- Cascade delete: When note deleted, remove tag associations
- Cascade delete: When tag deleted, remove all associations (future feature)

#### AttachmentEntity (One-to-Many Images)
```sql
CREATE TABLE IF NOT EXISTS `attachments` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `noteId` INTEGER NOT NULL,
    `uri` TEXT NOT NULL,
    `type` TEXT NOT NULL,
    `order` INTEGER NOT NULL,
    `createdAt` INTEGER NOT NULL,
    FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_attachments_noteId` ON `attachments` (`noteId`);
```

**Foreign Keys:**
- Cascade delete: When note deleted, delete all attachments (images)

#### NoteRelationEntity (@Mentions)
```sql
CREATE TABLE IF NOT EXISTS `note_relations` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `fromNoteId` INTEGER NOT NULL,
    `toNoteId` INTEGER NOT NULL,
    `mentionText` TEXT NOT NULL,
    FOREIGN KEY(`fromNoteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
    FOREIGN KEY(`toNoteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_note_relations_fromNoteId` ON `note_relations` (`fromNoteId`);
CREATE INDEX IF NOT EXISTS `index_note_relations_toNoteId` ON `note_relations` (`toNoteId`);
```

**Foreign Keys:**
- Cascade delete: When either note deleted, remove relation

### Room Database Configuration

```kotlin
@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AttachmentEntity::class,
        NoteRelationEntity::class
    ],
    version = 1,
    exportSchema = true  // Generate schema JSON for migration testing
)
@TypeConverters(Converters::class)
abstract class LozifyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun relationDao(): RelationDao
}
```

### Type Converters

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

---

## Version 2: Add Indexes for Archive and Pin Status

**Date:** 2026-08-12  
**Stage:** Stage 9 (Card Operations - Data Layer)

### Schema Changes

#### Updated NoteEntity Indexes
```sql
-- Added two new indexes for query optimization
CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`);
CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`);
```

**Rationale:**
- Main feed query filters by `is_archived = 0` and sorts by `is_pinned DESC`
- Without indexes, these operations would require full table scans
- Indexes improve query performance as note count grows

### Query Changes

**Before (Version 1):**
```sql
SELECT * FROM notes
WHERE is_deleted = 0
ORDER BY is_pinned DESC, created_at DESC
```

**After (Version 2):**
```sql
SELECT * FROM notes
WHERE is_deleted = 0 AND is_archived = 0
ORDER BY is_pinned DESC, updated_at DESC
```

**Changes:**
1. Added `is_archived = 0` filter to exclude archived notes from main feed
2. Changed sort from `created_at` to `updated_at` (recently edited notes rise to top)
3. Pinned notes always appear first regardless of update time

### DAO Methods Added

```kotlin
/**
 * Toggle archive status for a note.
 */
@Query("UPDATE notes SET is_archived = :isArchived, updated_at = :updatedAt WHERE id = :noteId")
suspend fun updateArchiveStatus(noteId: Long, isArchived: Boolean, updatedAt: Long)
```

### Repository Methods Added

```kotlin
override suspend fun toggleArchiveStatus(noteId: Long, isArchived: Boolean) {
    val now = Instant.now().toEpochMilli()
    noteDao.updateArchiveStatus(noteId, isArchived, now)
}
```

### Migration Strategy

**Development (MVP):**
- Using `fallbackToDestructiveMigration()` in DatabaseModule
- Fields `is_pinned` and `is_archived` already exist with default values (false)
- Only adding indexes, no data migration needed
- Database will be recreated automatically on app restart

**Production (Future):**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add indexes for performance optimization
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
    }
}

// Register in DatabaseModule
Room.databaseBuilder(context, LozifyDatabase::class.java, DATABASE_NAME)
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Testing:**
- No data loss risk (only adding indexes)
- No schema changes to existing columns
- Backward compatible (indexes don't affect data integrity)

---

## Migration Template (For Future Use)

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL statements here
        database.execSQL("ALTER TABLE notes ADD COLUMN newField TEXT")
    }
}

// Register in database builder
Room.databaseBuilder(context, LozifyDatabase::class.java, "lozify.db")
    .addMigrations(MIGRATION_X_Y)
    .build()
```

### Example Future Migrations

#### Migration 1 → 2: Add FTS4 for Full-Text Search
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create FTS4 virtual table
        database.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` 
            USING fts4(content TEXT, tokenize=unicode61)
        """)
        
        // Backfill existing notes
        database.execSQL("""
            INSERT INTO notes_fts(docid, content)
            SELECT id, content FROM notes WHERE isDeleted = 0
        """)
        
        // Create triggers to keep FTS in sync
        database.execSQL("""
            CREATE TRIGGER notes_fts_insert AFTER INSERT ON notes BEGIN
                INSERT INTO notes_fts(docid, content) VALUES (new.id, new.content);
            END
        """)
        
        database.execSQL("""
            CREATE TRIGGER notes_fts_update AFTER UPDATE ON notes BEGIN
                UPDATE notes_fts SET content = new.content WHERE docid = new.id;
            END
        """)
        
        database.execSQL("""
            CREATE TRIGGER notes_fts_delete AFTER DELETE ON notes BEGIN
                DELETE FROM notes_fts WHERE docid = old.id;
            END
        """)
    }
}
```

#### Migration 2 → 3: Add deletedAt Timestamp
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add deletedAt column (nullable)
        database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
        
        // Backfill deleted notes with updatedAt timestamp
        database.execSQL("""
            UPDATE notes 
            SET deletedAt = updatedAt 
            WHERE isDeleted = 1
        """)
    }
}
```

#### Migration 3 → 4: Add Color to TagEntity (If Needed)
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add color column with default blue
        database.execSQL("""
            ALTER TABLE tags ADD COLUMN color TEXT NOT NULL DEFAULT '#4C88FF'
        """)
    }
}
```

---

## Migration Testing

### Test Setup

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private lateinit var migrationTestHelper: MigrationTestHelper
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LozifyDatabase::class.java
    )
    
    @Test
    fun migrate1To2() {
        // Create database at version 1
        val db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO notes (id, content, createdAt, updatedAt, isPinned, isDeleted) VALUES (1, 'Test note', 1000, 1000, 0, 0)")
            close()
        }
        
        // Run migration
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        
        // Verify FTS table exists and has data
        val migratedDb = helper.getMigrationDatabase()
        val cursor = migratedDb.query("SELECT content FROM notes_fts WHERE docid = 1")
        cursor.moveToFirst()
        assertEquals("Test note", cursor.getString(0))
        cursor.close()
    }
    
    companion object {
        private const val TEST_DB = "migration-test"
    }
}
```

### Manual Testing Checklist

Before shipping a migration to production:
- [ ] Write instrumented test with `MigrationTestHelper`
- [ ] Test migration on emulator with real data
- [ ] Verify all queries still work after migration
- [ ] Check database file size (ensure no bloat)
- [ ] Test rollback scenario (downgrade app version)
- [ ] Update this documentation

---

## Rollback Strategy

### If Migration Fails in Production

**Option 1: Fix Forward (Preferred)**
- Identify root cause from crash logs
- Release hotfix with corrected migration (e.g., MIGRATION_2_3_FIXED)
- Increment database version again

**Option 2: Destructive Fallback (Last Resort)**
```kotlin
// Only for emergency releases
Room.databaseBuilder(context, LozifyDatabase::class.java, "lozify.db")
    .fallbackToDestructiveMigration()
    .build()
```

⚠️ **Warning:** This deletes all user data. Only use if:
- Migration causes app crashes on launch
- No fix-forward solution possible
- User data is backed up to cloud (post-MVP only)

Always show user a warning dialog before destructive migration.

---

## Schema Export Configuration

Room automatically exports schema JSON files for migration testing:

```kotlin
// In app/build.gradle.kts
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}

// For KSP
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

**Generated files:**
- `app/schemas/com.witte.lozify.core.database.LozifyDatabase/1.json`
- `app/schemas/com.witte.lozify.core.database.LozifyDatabase/2.json`
- etc.

**Version control:** Commit schema JSON files to git for migration testing.

---

## Common Migration Pitfalls

### 1. Forgetting to Add Migration
```kotlin
// ❌ Wrong: Database version incremented but no migration added
@Database(entities = [...], version = 2)
abstract class LozifyDatabase : RoomDatabase()

// ✅ Correct: Register migration
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

### 2. Incorrect SQL Syntax
```kotlin
// ❌ Wrong: SQLite doesn't support ALTER COLUMN
database.execSQL("ALTER TABLE notes ALTER COLUMN content TYPE TEXT")

// ✅ Correct: Create new table, copy data, drop old, rename
database.execSQL("CREATE TABLE notes_new (...)")
database.execSQL("INSERT INTO notes_new SELECT * FROM notes")
database.execSQL("DROP TABLE notes")
database.execSQL("ALTER TABLE notes_new RENAME TO notes")
```

### 3. Forgetting Indexes
```kotlin
// ❌ Wrong: Add column but forget to recreate index
database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")

// ✅ Correct: Recreate index if needed
database.execSQL("CREATE INDEX index_notes_deletedAt ON notes(deletedAt)")
```

### 4. Breaking Foreign Keys
```kotlin
// ❌ Wrong: Rename column referenced by foreign key without updating references
database.execSQL("ALTER TABLE notes RENAME COLUMN id TO note_id")
// Breaks AttachmentEntity foreign key: FOREIGN KEY(noteId) REFERENCES notes(id)

// ✅ Correct: Recreate dependent tables with updated foreign keys
```

---

## Future Considerations

### Cloud Sync Schema Requirements

When implementing cloud sync (post-MVP), add these fields:
- `syncStatus`: enum (SYNCED, PENDING, CONFLICT)
- `syncVersion`: Integer (for conflict resolution)
- `remoteId`: String (server-side ID)

### Encryption

If implementing encryption (post-MVP):
- Use SQLCipher for Android
- Requires full database recreation (cannot encrypt in-place)
- Migration path: export plaintext → create encrypted DB → import data

---

## Change Log

- **2026-08-07**: Initial documentation with Version 1 schema (Stage 0)
- **2026-08-12**: Added Version 2 migration for is_pinned and is_archived indexes (Stage 9)
