package com.zhuogui.firewall.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zhuogui.firewall.data.entity.FirewallRule;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
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
public final class FirewallRuleDao_Impl implements FirewallRuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FirewallRule> __insertionAdapterOfFirewallRule;

  private final EntityDeletionOrUpdateAdapter<FirewallRule> __deletionAdapterOfFirewallRule;

  private final EntityDeletionOrUpdateAdapter<FirewallRule> __updateAdapterOfFirewallRule;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByTarget;

  public FirewallRuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFirewallRule = new EntityInsertionAdapter<FirewallRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `firewall_rules` (`id`,`packageName`,`target`,`blocked`,`type`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FirewallRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPackageName());
        statement.bindString(3, entity.getTarget());
        final int _tmp = entity.getBlocked() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindString(5, entity.getType());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfFirewallRule = new EntityDeletionOrUpdateAdapter<FirewallRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `firewall_rules` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FirewallRule entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfFirewallRule = new EntityDeletionOrUpdateAdapter<FirewallRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `firewall_rules` SET `id` = ?,`packageName` = ?,`target` = ?,`blocked` = ?,`type` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FirewallRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPackageName());
        statement.bindString(3, entity.getTarget());
        final int _tmp = entity.getBlocked() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindString(5, entity.getType());
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM firewall_rules WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByTarget = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM firewall_rules WHERE packageName = ? AND target = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final FirewallRule rule, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfFirewallRule.insertAndReturnId(rule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final FirewallRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFirewallRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final FirewallRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFirewallRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByTarget(final String pkg, final String target,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByTarget.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, pkg);
        _argIndex = 2;
        _stmt.bindString(_argIndex, target);
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
          __preparedStmtOfDeleteByTarget.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FirewallRule>> getAllRules() {
    final String _sql = "SELECT * FROM firewall_rules ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"firewall_rules"}, new Callable<List<FirewallRule>>() {
      @Override
      @NonNull
      public List<FirewallRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FirewallRule> _result = new ArrayList<FirewallRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FirewallRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpTarget;
            _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FirewallRule(_tmpId,_tmpPackageName,_tmpTarget,_tmpBlocked,_tmpType,_tmpCreatedAt);
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
  public Flow<List<FirewallRule>> getRulesForPackage(final String pkg) {
    final String _sql = "SELECT * FROM firewall_rules WHERE packageName = ? OR packageName = '*'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pkg);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"firewall_rules"}, new Callable<List<FirewallRule>>() {
      @Override
      @NonNull
      public List<FirewallRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FirewallRule> _result = new ArrayList<FirewallRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FirewallRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpTarget;
            _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FirewallRule(_tmpId,_tmpPackageName,_tmpTarget,_tmpBlocked,_tmpType,_tmpCreatedAt);
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
  public Object getBlockRulesForPackage(final String pkg,
      final Continuation<? super List<FirewallRule>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM firewall_rules\n"
            + "        WHERE (packageName = ? OR packageName = '*')\n"
            + "        AND blocked = 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pkg);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FirewallRule>>() {
      @Override
      @NonNull
      public List<FirewallRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "target");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FirewallRule> _result = new ArrayList<FirewallRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FirewallRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpTarget;
            _tmpTarget = _cursor.getString(_cursorIndexOfTarget);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FirewallRule(_tmpId,_tmpPackageName,_tmpTarget,_tmpBlocked,_tmpType,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
