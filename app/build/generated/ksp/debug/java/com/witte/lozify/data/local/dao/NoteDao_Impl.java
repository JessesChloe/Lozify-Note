package com.witte.lozify.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.witte.lozify.data.local.converter.Converters;
import com.witte.lozify.data.local.entity.AttachmentEntity;
import com.witte.lozify.data.local.entity.NoteEntity;
import com.witte.lozify.data.local.entity.TagEntity;
import com.witte.lozify.data.local.model.NoteWithTagsAndAttachments;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NoteDao_Impl implements NoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NoteEntity> __insertionAdapterOfNoteEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<NoteEntity> __deletionAdapterOfNoteEntity;

  private final EntityDeletionOrUpdateAdapter<NoteEntity> __updateAdapterOfNoteEntity;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteNote;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePinStatus;

  public NoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNoteEntity = new EntityInsertionAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notes` (`id`,`content`,`created_at`,`updated_at`,`is_pinned`,`is_deleted`,`sync_id`,`last_synced_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getContent());
        final Long _tmp = __converters.instantToTimestamp(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, _tmp);
        }
        final Long _tmp_1 = __converters.instantToTimestamp(entity.getUpdatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        final int _tmp_2 = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp_2);
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(6, _tmp_3);
        if (entity.getSyncId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSyncId());
        }
        final Long _tmp_4 = __converters.instantToTimestamp(entity.getLastSyncedAt());
        if (_tmp_4 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_4);
        }
      }
    };
    this.__deletionAdapterOfNoteEntity = new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `notes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfNoteEntity = new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notes` SET `id` = ?,`content` = ?,`created_at` = ?,`updated_at` = ?,`is_pinned` = ?,`is_deleted` = ?,`sync_id` = ?,`last_synced_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getContent());
        final Long _tmp = __converters.instantToTimestamp(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, _tmp);
        }
        final Long _tmp_1 = __converters.instantToTimestamp(entity.getUpdatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        final int _tmp_2 = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp_2);
        final int _tmp_3 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(6, _tmp_3);
        if (entity.getSyncId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSyncId());
        }
        final Long _tmp_4 = __converters.instantToTimestamp(entity.getLastSyncedAt());
        if (_tmp_4 == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp_4);
        }
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfSoftDeleteNote = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET is_deleted = 1, updated_at = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePinStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notes SET is_pinned = ?, updated_at = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNote(final NoteEntity note, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfNoteEntity.insertAndReturnId(note);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNoteEntity.handle(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNoteEntity.handle(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteNote(final long noteId, final long deletedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteNote.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, deletedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, noteId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSoftDeleteNote.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePinStatus(final long noteId, final boolean isPinned, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePinStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isPinned ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, noteId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdatePinStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteEntity>> getAllNotes() {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0\n"
            + "        ORDER BY is_pinned DESC, created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final boolean _tmpIsPinned;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_4 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_5 != 0;
            final String _tmpSyncId;
            if (_cursor.isNull(_cursorIndexOfSyncId)) {
              _tmpSyncId = null;
            } else {
              _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            }
            final Instant _tmpLastSyncedAt;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
            _item = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteWithTagsAndAttachments>> getAllNotesWithRelations() {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0\n"
            + "        ORDER BY is_pinned DESC, created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"note_tag_cross_ref", "tags",
        "attachments", "notes"}, new Callable<List<NoteWithTagsAndAttachments>>() {
      @Override
      @NonNull
      public List<NoteWithTagsAndAttachments> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final LongSparseArray<ArrayList<TagEntity>> _collectionTags = new LongSparseArray<ArrayList<TagEntity>>();
            final LongSparseArray<ArrayList<AttachmentEntity>> _collectionAttachments = new LongSparseArray<ArrayList<AttachmentEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionTags.containsKey(_tmpKey)) {
                _collectionTags.put(_tmpKey, new ArrayList<TagEntity>());
              }
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionAttachments.containsKey(_tmpKey_1)) {
                _collectionAttachments.put(_tmpKey_1, new ArrayList<AttachmentEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(_collectionTags);
            __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(_collectionAttachments);
            final List<NoteWithTagsAndAttachments> _result = new ArrayList<NoteWithTagsAndAttachments>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteWithTagsAndAttachments _item;
              final NoteEntity _tmpNote;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _tmpNote = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              final ArrayList<TagEntity> _tmpTagsCollection;
              final long _tmpKey_2;
              _tmpKey_2 = _cursor.getLong(_cursorIndexOfId);
              _tmpTagsCollection = _collectionTags.get(_tmpKey_2);
              final ArrayList<AttachmentEntity> _tmpAttachmentsCollection;
              final long _tmpKey_3;
              _tmpKey_3 = _cursor.getLong(_cursorIndexOfId);
              _tmpAttachmentsCollection = _collectionAttachments.get(_tmpKey_3);
              _item = new NoteWithTagsAndAttachments(_tmpNote,_tmpTagsCollection,_tmpAttachmentsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<NoteEntity> getNoteById(final long noteId) {
    final String _sql = "SELECT * FROM notes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, noteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<NoteEntity>() {
      @Override
      @Nullable
      public NoteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
          final NoteEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final boolean _tmpIsPinned;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_4 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_5 != 0;
            final String _tmpSyncId;
            if (_cursor.isNull(_cursorIndexOfSyncId)) {
              _tmpSyncId = null;
            } else {
              _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            }
            final Instant _tmpLastSyncedAt;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
            _result = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> searchNotes(final String searchQuery) {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0 AND content LIKE '%' || ? || '%'\n"
            + "        ORDER BY is_pinned DESC, created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, searchQuery);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final boolean _tmpIsPinned;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_4 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_5 != 0;
            final String _tmpSyncId;
            if (_cursor.isNull(_cursorIndexOfSyncId)) {
              _tmpSyncId = null;
            } else {
              _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            }
            final Instant _tmpLastSyncedAt;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
            _item = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteWithTagsAndAttachments>> searchNotesWithRelations(final String searchQuery) {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0 AND content LIKE '%' || ? || '%'\n"
            + "        ORDER BY is_pinned DESC, created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, searchQuery);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"note_tag_cross_ref", "tags",
        "attachments", "notes"}, new Callable<List<NoteWithTagsAndAttachments>>() {
      @Override
      @NonNull
      public List<NoteWithTagsAndAttachments> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final LongSparseArray<ArrayList<TagEntity>> _collectionTags = new LongSparseArray<ArrayList<TagEntity>>();
            final LongSparseArray<ArrayList<AttachmentEntity>> _collectionAttachments = new LongSparseArray<ArrayList<AttachmentEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionTags.containsKey(_tmpKey)) {
                _collectionTags.put(_tmpKey, new ArrayList<TagEntity>());
              }
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionAttachments.containsKey(_tmpKey_1)) {
                _collectionAttachments.put(_tmpKey_1, new ArrayList<AttachmentEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(_collectionTags);
            __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(_collectionAttachments);
            final List<NoteWithTagsAndAttachments> _result = new ArrayList<NoteWithTagsAndAttachments>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteWithTagsAndAttachments _item;
              final NoteEntity _tmpNote;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _tmpNote = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              final ArrayList<TagEntity> _tmpTagsCollection;
              final long _tmpKey_2;
              _tmpKey_2 = _cursor.getLong(_cursorIndexOfId);
              _tmpTagsCollection = _collectionTags.get(_tmpKey_2);
              final ArrayList<AttachmentEntity> _tmpAttachmentsCollection;
              final long _tmpKey_3;
              _tmpKey_3 = _cursor.getLong(_cursorIndexOfId);
              _tmpAttachmentsCollection = _collectionAttachments.get(_tmpKey_3);
              _item = new NoteWithTagsAndAttachments(_tmpNote,_tmpTagsCollection,_tmpAttachmentsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getPinnedNotes() {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0 AND is_pinned = 1\n"
            + "        ORDER BY created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final boolean _tmpIsPinned;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_4 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_5 != 0;
            final String _tmpSyncId;
            if (_cursor.isNull(_cursorIndexOfSyncId)) {
              _tmpSyncId = null;
            } else {
              _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            }
            final Instant _tmpLastSyncedAt;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
            _item = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteWithTagsAndAttachments>> getPinnedNotesWithRelations() {
    final String _sql = "\n"
            + "        SELECT * FROM notes\n"
            + "        WHERE is_deleted = 0 AND is_pinned = 1\n"
            + "        ORDER BY created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"note_tag_cross_ref", "tags",
        "attachments", "notes"}, new Callable<List<NoteWithTagsAndAttachments>>() {
      @Override
      @NonNull
      public List<NoteWithTagsAndAttachments> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final LongSparseArray<ArrayList<TagEntity>> _collectionTags = new LongSparseArray<ArrayList<TagEntity>>();
            final LongSparseArray<ArrayList<AttachmentEntity>> _collectionAttachments = new LongSparseArray<ArrayList<AttachmentEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionTags.containsKey(_tmpKey)) {
                _collectionTags.put(_tmpKey, new ArrayList<TagEntity>());
              }
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionAttachments.containsKey(_tmpKey_1)) {
                _collectionAttachments.put(_tmpKey_1, new ArrayList<AttachmentEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(_collectionTags);
            __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(_collectionAttachments);
            final List<NoteWithTagsAndAttachments> _result = new ArrayList<NoteWithTagsAndAttachments>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteWithTagsAndAttachments _item;
              final NoteEntity _tmpNote;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _tmpNote = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              final ArrayList<TagEntity> _tmpTagsCollection;
              final long _tmpKey_2;
              _tmpKey_2 = _cursor.getLong(_cursorIndexOfId);
              _tmpTagsCollection = _collectionTags.get(_tmpKey_2);
              final ArrayList<AttachmentEntity> _tmpAttachmentsCollection;
              final long _tmpKey_3;
              _tmpKey_3 = _cursor.getLong(_cursorIndexOfId);
              _tmpAttachmentsCollection = _collectionAttachments.get(_tmpKey_3);
              _item = new NoteWithTagsAndAttachments(_tmpNote,_tmpTagsCollection,_tmpAttachmentsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getDeletedNotes() {
    final String _sql = "SELECT * FROM notes WHERE is_deleted = 1 ORDER BY updated_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_3;
            }
            final boolean _tmpIsPinned;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_4 != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_5 != 0;
            final String _tmpSyncId;
            if (_cursor.isNull(_cursorIndexOfSyncId)) {
              _tmpSyncId = null;
            } else {
              _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
            }
            final Instant _tmpLastSyncedAt;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
            _item = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteWithTagsAndAttachments>> getDeletedNotesWithRelations() {
    final String _sql = "SELECT * FROM notes WHERE is_deleted = 1 ORDER BY updated_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"note_tag_cross_ref", "tags",
        "attachments", "notes"}, new Callable<List<NoteWithTagsAndAttachments>>() {
      @Override
      @NonNull
      public List<NoteWithTagsAndAttachments> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final LongSparseArray<ArrayList<TagEntity>> _collectionTags = new LongSparseArray<ArrayList<TagEntity>>();
            final LongSparseArray<ArrayList<AttachmentEntity>> _collectionAttachments = new LongSparseArray<ArrayList<AttachmentEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionTags.containsKey(_tmpKey)) {
                _collectionTags.put(_tmpKey, new ArrayList<TagEntity>());
              }
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionAttachments.containsKey(_tmpKey_1)) {
                _collectionAttachments.put(_tmpKey_1, new ArrayList<AttachmentEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(_collectionTags);
            __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(_collectionAttachments);
            final List<NoteWithTagsAndAttachments> _result = new ArrayList<NoteWithTagsAndAttachments>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteWithTagsAndAttachments _item;
              final NoteEntity _tmpNote;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _tmpNote = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              final ArrayList<TagEntity> _tmpTagsCollection;
              final long _tmpKey_2;
              _tmpKey_2 = _cursor.getLong(_cursorIndexOfId);
              _tmpTagsCollection = _collectionTags.get(_tmpKey_2);
              final ArrayList<AttachmentEntity> _tmpAttachmentsCollection;
              final long _tmpKey_3;
              _tmpKey_3 = _cursor.getLong(_cursorIndexOfId);
              _tmpAttachmentsCollection = _collectionAttachments.get(_tmpKey_3);
              _item = new NoteWithTagsAndAttachments(_tmpNote,_tmpTagsCollection,_tmpAttachmentsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getNotesByTag(final long tagId) {
    final String _sql = "\n"
            + "        SELECT notes.* FROM notes\n"
            + "        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.note_id\n"
            + "        WHERE note_tag_cross_ref.tag_id = ? AND notes.is_deleted = 0\n"
            + "        ORDER BY notes.is_pinned DESC, notes.created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, tagId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"notes",
        "note_tag_cross_ref"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteEntity _item;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _item = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteWithTagsAndAttachments>> getNotesByTagWithRelations(final long tagId) {
    final String _sql = "\n"
            + "        SELECT notes.* FROM notes\n"
            + "        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.note_id\n"
            + "        WHERE note_tag_cross_ref.tag_id = ? AND notes.is_deleted = 0\n"
            + "        ORDER BY notes.is_pinned DESC, notes.created_at DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, tagId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"note_tag_cross_ref", "tags",
        "attachments", "notes"}, new Callable<List<NoteWithTagsAndAttachments>>() {
      @Override
      @NonNull
      public List<NoteWithTagsAndAttachments> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "is_pinned");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
            final int _cursorIndexOfSyncId = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_id");
            final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced_at");
            final LongSparseArray<ArrayList<TagEntity>> _collectionTags = new LongSparseArray<ArrayList<TagEntity>>();
            final LongSparseArray<ArrayList<AttachmentEntity>> _collectionAttachments = new LongSparseArray<ArrayList<AttachmentEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionTags.containsKey(_tmpKey)) {
                _collectionTags.put(_tmpKey, new ArrayList<TagEntity>());
              }
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionAttachments.containsKey(_tmpKey_1)) {
                _collectionAttachments.put(_tmpKey_1, new ArrayList<AttachmentEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(_collectionTags);
            __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(_collectionAttachments);
            final List<NoteWithTagsAndAttachments> _result = new ArrayList<NoteWithTagsAndAttachments>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final NoteWithTagsAndAttachments _item;
              final NoteEntity _tmpNote;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpContent;
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
              final Instant _tmpCreatedAt;
              final Long _tmp;
              if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
                _tmp = null;
              } else {
                _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
              }
              final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
              if (_tmp_1 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpCreatedAt = _tmp_1;
              }
              final Instant _tmpUpdatedAt;
              final Long _tmp_2;
              if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
                _tmp_2 = null;
              } else {
                _tmp_2 = _cursor.getLong(_cursorIndexOfUpdatedAt);
              }
              final Instant _tmp_3 = __converters.fromTimestamp(_tmp_2);
              if (_tmp_3 == null) {
                throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
              } else {
                _tmpUpdatedAt = _tmp_3;
              }
              final boolean _tmpIsPinned;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfIsPinned);
              _tmpIsPinned = _tmp_4 != 0;
              final boolean _tmpIsDeleted;
              final int _tmp_5;
              _tmp_5 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_5 != 0;
              final String _tmpSyncId;
              if (_cursor.isNull(_cursorIndexOfSyncId)) {
                _tmpSyncId = null;
              } else {
                _tmpSyncId = _cursor.getString(_cursorIndexOfSyncId);
              }
              final Instant _tmpLastSyncedAt;
              final Long _tmp_6;
              if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
                _tmp_6 = null;
              } else {
                _tmp_6 = _cursor.getLong(_cursorIndexOfLastSyncedAt);
              }
              _tmpLastSyncedAt = __converters.fromTimestamp(_tmp_6);
              _tmpNote = new NoteEntity(_tmpId,_tmpContent,_tmpCreatedAt,_tmpUpdatedAt,_tmpIsPinned,_tmpIsDeleted,_tmpSyncId,_tmpLastSyncedAt);
              final ArrayList<TagEntity> _tmpTagsCollection;
              final long _tmpKey_2;
              _tmpKey_2 = _cursor.getLong(_cursorIndexOfId);
              _tmpTagsCollection = _collectionTags.get(_tmpKey_2);
              final ArrayList<AttachmentEntity> _tmpAttachmentsCollection;
              final long _tmpKey_3;
              _tmpKey_3 = _cursor.getLong(_cursorIndexOfId);
              _tmpAttachmentsCollection = _collectionAttachments.get(_tmpKey_3);
              _item = new NoteWithTagsAndAttachments(_tmpNote,_tmpTagsCollection,_tmpAttachmentsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getActiveNotesCount() {
    final String _sql = "SELECT COUNT(*) FROM notes WHERE is_deleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(
      @NonNull final LongSparseArray<ArrayList<TagEntity>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshiptagsAscomWitteLozifyDataLocalEntityTagEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `tags`.`id` AS `id`,`tags`.`name` AS `name`,`tags`.`created_at` AS `created_at`,`tags`.`usage_count` AS `usage_count`,_junction.`note_id` FROM `note_tag_cross_ref` AS _junction INNER JOIN `tags` ON (_junction.`tag_id` = `tags`.`id`) WHERE _junction.`note_id` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      // _junction.note_id;
      final int _itemKeyIndex = 4;
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfName = 1;
      final int _cursorIndexOfCreatedAt = 2;
      final int _cursorIndexOfUsageCount = 3;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<TagEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final TagEntity _item_1;
          final long _tmpId;
          _tmpId = _cursor.getLong(_cursorIndexOfId);
          final String _tmpName;
          _tmpName = _cursor.getString(_cursorIndexOfName);
          final Instant _tmpCreatedAt;
          final Long _tmp;
          if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
            _tmp = null;
          } else {
            _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
          }
          final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
          if (_tmp_1 == null) {
            throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
          } else {
            _tmpCreatedAt = _tmp_1;
          }
          final int _tmpUsageCount;
          _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
          _item_1 = new TagEntity(_tmpId,_tmpName,_tmpCreatedAt,_tmpUsageCount);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }

  private void __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(
      @NonNull final LongSparseArray<ArrayList<AttachmentEntity>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipattachmentsAscomWitteLozifyDataLocalEntityAttachmentEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`note_id`,`file_path`,`display_order`,`created_at`,`mime_type`,`file_size` FROM `attachments` WHERE `note_id` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "note_id");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfNoteId = 1;
      final int _cursorIndexOfFilePath = 2;
      final int _cursorIndexOfDisplayOrder = 3;
      final int _cursorIndexOfCreatedAt = 4;
      final int _cursorIndexOfMimeType = 5;
      final int _cursorIndexOfFileSize = 6;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<AttachmentEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final AttachmentEntity _item_1;
          final long _tmpId;
          _tmpId = _cursor.getLong(_cursorIndexOfId);
          final long _tmpNoteId;
          _tmpNoteId = _cursor.getLong(_cursorIndexOfNoteId);
          final String _tmpFilePath;
          _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
          final int _tmpDisplayOrder;
          _tmpDisplayOrder = _cursor.getInt(_cursorIndexOfDisplayOrder);
          final Instant _tmpCreatedAt;
          final Long _tmp;
          if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
            _tmp = null;
          } else {
            _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
          }
          final Instant _tmp_1 = __converters.fromTimestamp(_tmp);
          if (_tmp_1 == null) {
            throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
          } else {
            _tmpCreatedAt = _tmp_1;
          }
          final String _tmpMimeType;
          if (_cursor.isNull(_cursorIndexOfMimeType)) {
            _tmpMimeType = null;
          } else {
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
          }
          final Long _tmpFileSize;
          if (_cursor.isNull(_cursorIndexOfFileSize)) {
            _tmpFileSize = null;
          } else {
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
          }
          _item_1 = new AttachmentEntity(_tmpId,_tmpNoteId,_tmpFilePath,_tmpDisplayOrder,_tmpCreatedAt,_tmpMimeType,_tmpFileSize);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
