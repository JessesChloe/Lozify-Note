package com.witte.lozify.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.witte.lozify.data.local.dao.AttachmentDao;
import com.witte.lozify.data.local.dao.AttachmentDao_Impl;
import com.witte.lozify.data.local.dao.NoteDao;
import com.witte.lozify.data.local.dao.NoteDao_Impl;
import com.witte.lozify.data.local.dao.NoteRelationDao;
import com.witte.lozify.data.local.dao.NoteRelationDao_Impl;
import com.witte.lozify.data.local.dao.TagDao;
import com.witte.lozify.data.local.dao.TagDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LozifyDatabase_Impl extends LozifyDatabase {
  private volatile NoteDao _noteDao;

  private volatile TagDao _tagDao;

  private volatile AttachmentDao _attachmentDao;

  private volatile NoteRelationDao _noteRelationDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `is_pinned` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, `sync_id` TEXT, `last_synced_at` INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_created_at` ON `notes` (`created_at`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updated_at` ON `notes` (`updated_at`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_is_deleted` ON `notes` (`is_deleted`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `usage_count` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_usage_count` ON `tags` (`usage_count`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (`note_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, PRIMARY KEY(`note_id`, `tag_id`), FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_note_id` ON `note_tag_cross_ref` (`note_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tag_id` ON `note_tag_cross_ref` (`tag_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `note_id` INTEGER NOT NULL, `file_path` TEXT NOT NULL, `display_order` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `mime_type` TEXT, `file_size` INTEGER, FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_note_id` ON `attachments` (`note_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `note_relations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `from_note_id` INTEGER NOT NULL, `to_note_id` INTEGER NOT NULL, `mention_text` TEXT NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`from_note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`to_note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_relations_from_note_id` ON `note_relations` (`from_note_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_relations_to_note_id` ON `note_relations` (`to_note_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e312e52e9383312ceae0fb1156c33485')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `notes`");
        db.execSQL("DROP TABLE IF EXISTS `tags`");
        db.execSQL("DROP TABLE IF EXISTS `note_tag_cross_ref`");
        db.execSQL("DROP TABLE IF EXISTS `attachments`");
        db.execSQL("DROP TABLE IF EXISTS `note_relations`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsNotes = new HashMap<String, TableInfo.Column>(8);
        _columnsNotes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("is_pinned", new TableInfo.Column("is_pinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("is_deleted", new TableInfo.Column("is_deleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("sync_id", new TableInfo.Column("sync_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("last_synced_at", new TableInfo.Column("last_synced_at", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotes = new HashSet<TableInfo.Index>(3);
        _indicesNotes.add(new TableInfo.Index("index_notes_created_at", false, Arrays.asList("created_at"), Arrays.asList("ASC")));
        _indicesNotes.add(new TableInfo.Index("index_notes_updated_at", false, Arrays.asList("updated_at"), Arrays.asList("ASC")));
        _indicesNotes.add(new TableInfo.Index("index_notes_is_deleted", false, Arrays.asList("is_deleted"), Arrays.asList("ASC")));
        final TableInfo _infoNotes = new TableInfo("notes", _columnsNotes, _foreignKeysNotes, _indicesNotes);
        final TableInfo _existingNotes = TableInfo.read(db, "notes");
        if (!_infoNotes.equals(_existingNotes)) {
          return new RoomOpenHelper.ValidationResult(false, "notes(com.witte.lozify.data.local.entity.NoteEntity).\n"
                  + " Expected:\n" + _infoNotes + "\n"
                  + " Found:\n" + _existingNotes);
        }
        final HashMap<String, TableInfo.Column> _columnsTags = new HashMap<String, TableInfo.Column>(4);
        _columnsTags.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("usage_count", new TableInfo.Column("usage_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTags = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTags = new HashSet<TableInfo.Index>(2);
        _indicesTags.add(new TableInfo.Index("index_tags_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        _indicesTags.add(new TableInfo.Index("index_tags_usage_count", false, Arrays.asList("usage_count"), Arrays.asList("ASC")));
        final TableInfo _infoTags = new TableInfo("tags", _columnsTags, _foreignKeysTags, _indicesTags);
        final TableInfo _existingTags = TableInfo.read(db, "tags");
        if (!_infoTags.equals(_existingTags)) {
          return new RoomOpenHelper.ValidationResult(false, "tags(com.witte.lozify.data.local.entity.TagEntity).\n"
                  + " Expected:\n" + _infoTags + "\n"
                  + " Found:\n" + _existingTags);
        }
        final HashMap<String, TableInfo.Column> _columnsNoteTagCrossRef = new HashMap<String, TableInfo.Column>(2);
        _columnsNoteTagCrossRef.put("note_id", new TableInfo.Column("note_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteTagCrossRef.put("tag_id", new TableInfo.Column("tag_id", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNoteTagCrossRef = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysNoteTagCrossRef.add(new TableInfo.ForeignKey("notes", "CASCADE", "NO ACTION", Arrays.asList("note_id"), Arrays.asList("id")));
        _foreignKeysNoteTagCrossRef.add(new TableInfo.ForeignKey("tags", "CASCADE", "NO ACTION", Arrays.asList("tag_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesNoteTagCrossRef = new HashSet<TableInfo.Index>(2);
        _indicesNoteTagCrossRef.add(new TableInfo.Index("index_note_tag_cross_ref_note_id", false, Arrays.asList("note_id"), Arrays.asList("ASC")));
        _indicesNoteTagCrossRef.add(new TableInfo.Index("index_note_tag_cross_ref_tag_id", false, Arrays.asList("tag_id"), Arrays.asList("ASC")));
        final TableInfo _infoNoteTagCrossRef = new TableInfo("note_tag_cross_ref", _columnsNoteTagCrossRef, _foreignKeysNoteTagCrossRef, _indicesNoteTagCrossRef);
        final TableInfo _existingNoteTagCrossRef = TableInfo.read(db, "note_tag_cross_ref");
        if (!_infoNoteTagCrossRef.equals(_existingNoteTagCrossRef)) {
          return new RoomOpenHelper.ValidationResult(false, "note_tag_cross_ref(com.witte.lozify.data.local.entity.NoteTagCrossRef).\n"
                  + " Expected:\n" + _infoNoteTagCrossRef + "\n"
                  + " Found:\n" + _existingNoteTagCrossRef);
        }
        final HashMap<String, TableInfo.Column> _columnsAttachments = new HashMap<String, TableInfo.Column>(7);
        _columnsAttachments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("note_id", new TableInfo.Column("note_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("file_path", new TableInfo.Column("file_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("display_order", new TableInfo.Column("display_order", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("mime_type", new TableInfo.Column("mime_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("file_size", new TableInfo.Column("file_size", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAttachments = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAttachments.add(new TableInfo.ForeignKey("notes", "CASCADE", "NO ACTION", Arrays.asList("note_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesAttachments = new HashSet<TableInfo.Index>(1);
        _indicesAttachments.add(new TableInfo.Index("index_attachments_note_id", false, Arrays.asList("note_id"), Arrays.asList("ASC")));
        final TableInfo _infoAttachments = new TableInfo("attachments", _columnsAttachments, _foreignKeysAttachments, _indicesAttachments);
        final TableInfo _existingAttachments = TableInfo.read(db, "attachments");
        if (!_infoAttachments.equals(_existingAttachments)) {
          return new RoomOpenHelper.ValidationResult(false, "attachments(com.witte.lozify.data.local.entity.AttachmentEntity).\n"
                  + " Expected:\n" + _infoAttachments + "\n"
                  + " Found:\n" + _existingAttachments);
        }
        final HashMap<String, TableInfo.Column> _columnsNoteRelations = new HashMap<String, TableInfo.Column>(5);
        _columnsNoteRelations.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteRelations.put("from_note_id", new TableInfo.Column("from_note_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteRelations.put("to_note_id", new TableInfo.Column("to_note_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteRelations.put("mention_text", new TableInfo.Column("mention_text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNoteRelations.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNoteRelations = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysNoteRelations.add(new TableInfo.ForeignKey("notes", "CASCADE", "NO ACTION", Arrays.asList("from_note_id"), Arrays.asList("id")));
        _foreignKeysNoteRelations.add(new TableInfo.ForeignKey("notes", "CASCADE", "NO ACTION", Arrays.asList("to_note_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesNoteRelations = new HashSet<TableInfo.Index>(2);
        _indicesNoteRelations.add(new TableInfo.Index("index_note_relations_from_note_id", false, Arrays.asList("from_note_id"), Arrays.asList("ASC")));
        _indicesNoteRelations.add(new TableInfo.Index("index_note_relations_to_note_id", false, Arrays.asList("to_note_id"), Arrays.asList("ASC")));
        final TableInfo _infoNoteRelations = new TableInfo("note_relations", _columnsNoteRelations, _foreignKeysNoteRelations, _indicesNoteRelations);
        final TableInfo _existingNoteRelations = TableInfo.read(db, "note_relations");
        if (!_infoNoteRelations.equals(_existingNoteRelations)) {
          return new RoomOpenHelper.ValidationResult(false, "note_relations(com.witte.lozify.data.local.entity.NoteRelationEntity).\n"
                  + " Expected:\n" + _infoNoteRelations + "\n"
                  + " Found:\n" + _existingNoteRelations);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e312e52e9383312ceae0fb1156c33485", "193eb48151ce563cf387f06e24495141");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "notes","tags","note_tag_cross_ref","attachments","note_relations");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `notes`");
      _db.execSQL("DELETE FROM `tags`");
      _db.execSQL("DELETE FROM `note_tag_cross_ref`");
      _db.execSQL("DELETE FROM `attachments`");
      _db.execSQL("DELETE FROM `note_relations`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(NoteDao.class, NoteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TagDao.class, TagDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AttachmentDao.class, AttachmentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NoteRelationDao.class, NoteRelationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public NoteDao noteDao() {
    if (_noteDao != null) {
      return _noteDao;
    } else {
      synchronized(this) {
        if(_noteDao == null) {
          _noteDao = new NoteDao_Impl(this);
        }
        return _noteDao;
      }
    }
  }

  @Override
  public TagDao tagDao() {
    if (_tagDao != null) {
      return _tagDao;
    } else {
      synchronized(this) {
        if(_tagDao == null) {
          _tagDao = new TagDao_Impl(this);
        }
        return _tagDao;
      }
    }
  }

  @Override
  public AttachmentDao attachmentDao() {
    if (_attachmentDao != null) {
      return _attachmentDao;
    } else {
      synchronized(this) {
        if(_attachmentDao == null) {
          _attachmentDao = new AttachmentDao_Impl(this);
        }
        return _attachmentDao;
      }
    }
  }

  @Override
  public NoteRelationDao noteRelationDao() {
    if (_noteRelationDao != null) {
      return _noteRelationDao;
    } else {
      synchronized(this) {
        if(_noteRelationDao == null) {
          _noteRelationDao = new NoteRelationDao_Impl(this);
        }
        return _noteRelationDao;
      }
    }
  }
}
