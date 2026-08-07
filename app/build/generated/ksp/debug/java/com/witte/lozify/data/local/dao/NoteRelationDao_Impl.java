package com.witte.lozify.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.witte.lozify.data.local.converter.Converters;
import com.witte.lozify.data.local.entity.NoteRelationEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class NoteRelationDao_Impl implements NoteRelationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NoteRelationEntity> __insertionAdapterOfNoteRelationEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<NoteRelationEntity> __deletionAdapterOfNoteRelationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllOutgoingRelations;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRelationByNoteIds;

  public NoteRelationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNoteRelationEntity = new EntityInsertionAdapter<NoteRelationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `note_relations` (`id`,`from_note_id`,`to_note_id`,`mention_text`,`created_at`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteRelationEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getFromNoteId());
        statement.bindLong(3, entity.getToNoteId());
        statement.bindString(4, entity.getMentionText());
        final Long _tmp = __converters.instantToTimestamp(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp);
        }
      }
    };
    this.__deletionAdapterOfNoteRelationEntity = new EntityDeletionOrUpdateAdapter<NoteRelationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `note_relations` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteRelationEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllOutgoingRelations = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM note_relations WHERE from_note_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteRelationByNoteIds = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM note_relations WHERE from_note_id = ? AND to_note_id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertRelation(final NoteRelationEntity relation,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfNoteRelationEntity.insertAndReturnId(relation);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRelations(final List<NoteRelationEntity> relations,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNoteRelationEntity.insert(relations);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRelation(final NoteRelationEntity relation,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNoteRelationEntity.handle(relation);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllOutgoingRelations(final long noteId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllOutgoingRelations.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfDeleteAllOutgoingRelations.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRelationByNoteIds(final long fromNoteId, final long toNoteId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRelationByNoteIds.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, fromNoteId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, toNoteId);
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
          __preparedStmtOfDeleteRelationByNoteIds.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteRelationEntity>> getOutgoingRelations(final long noteId) {
    final String _sql = "SELECT * FROM note_relations WHERE from_note_id = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, noteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"note_relations"}, new Callable<List<NoteRelationEntity>>() {
      @Override
      @NonNull
      public List<NoteRelationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFromNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "from_note_id");
          final int _cursorIndexOfToNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "to_note_id");
          final int _cursorIndexOfMentionText = CursorUtil.getColumnIndexOrThrow(_cursor, "mention_text");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<NoteRelationEntity> _result = new ArrayList<NoteRelationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteRelationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpFromNoteId;
            _tmpFromNoteId = _cursor.getLong(_cursorIndexOfFromNoteId);
            final long _tmpToNoteId;
            _tmpToNoteId = _cursor.getLong(_cursorIndexOfToNoteId);
            final String _tmpMentionText;
            _tmpMentionText = _cursor.getString(_cursorIndexOfMentionText);
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
            _item = new NoteRelationEntity(_tmpId,_tmpFromNoteId,_tmpToNoteId,_tmpMentionText,_tmpCreatedAt);
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
  public Flow<List<NoteRelationEntity>> getIncomingRelations(final long noteId) {
    final String _sql = "SELECT * FROM note_relations WHERE to_note_id = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, noteId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"note_relations"}, new Callable<List<NoteRelationEntity>>() {
      @Override
      @NonNull
      public List<NoteRelationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFromNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "from_note_id");
          final int _cursorIndexOfToNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "to_note_id");
          final int _cursorIndexOfMentionText = CursorUtil.getColumnIndexOrThrow(_cursor, "mention_text");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<NoteRelationEntity> _result = new ArrayList<NoteRelationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteRelationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpFromNoteId;
            _tmpFromNoteId = _cursor.getLong(_cursorIndexOfFromNoteId);
            final long _tmpToNoteId;
            _tmpToNoteId = _cursor.getLong(_cursorIndexOfToNoteId);
            final String _tmpMentionText;
            _tmpMentionText = _cursor.getString(_cursorIndexOfMentionText);
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
            _item = new NoteRelationEntity(_tmpId,_tmpFromNoteId,_tmpToNoteId,_tmpMentionText,_tmpCreatedAt);
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
  public Object getRelation(final long fromNoteId, final long toNoteId,
      final Continuation<? super NoteRelationEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM note_relations\n"
            + "        WHERE from_note_id = ? AND to_note_id = ?\n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fromNoteId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, toNoteId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NoteRelationEntity>() {
      @Override
      @Nullable
      public NoteRelationEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFromNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "from_note_id");
          final int _cursorIndexOfToNoteId = CursorUtil.getColumnIndexOrThrow(_cursor, "to_note_id");
          final int _cursorIndexOfMentionText = CursorUtil.getColumnIndexOrThrow(_cursor, "mention_text");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final NoteRelationEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpFromNoteId;
            _tmpFromNoteId = _cursor.getLong(_cursorIndexOfFromNoteId);
            final long _tmpToNoteId;
            _tmpToNoteId = _cursor.getLong(_cursorIndexOfToNoteId);
            final String _tmpMentionText;
            _tmpMentionText = _cursor.getString(_cursorIndexOfMentionText);
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
            _result = new NoteRelationEntity(_tmpId,_tmpFromNoteId,_tmpToNoteId,_tmpMentionText,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getOutgoingRelationsCount(final long noteId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM note_relations WHERE from_note_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, noteId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
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
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getIncomingRelationsCount(final long noteId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM note_relations WHERE to_note_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, noteId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
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
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getRelationsCount() {
    final String _sql = "SELECT COUNT(*) FROM note_relations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"note_relations"}, new Callable<Integer>() {
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
}
