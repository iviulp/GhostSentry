package com.zhuogui.firewall.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.zhuogui.firewall.data.entity.ConnectionLog;
import java.lang.Class;
import java.lang.Exception;
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
public final class ConnectionLogDao_Impl implements ConnectionLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConnectionLog> __insertionAdapterOfConnectionLog;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ConnectionLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConnectionLog = new EntityInsertionAdapter<ConnectionLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `connection_logs` (`id`,`packageName`,`appName`,`destIp`,`destPort`,`destDomain`,`protocol`,`blocked`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConnectionLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPackageName());
        statement.bindString(3, entity.getAppName());
        statement.bindString(4, entity.getDestIp());
        statement.bindLong(5, entity.getDestPort());
        if (entity.getDestDomain() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDestDomain());
        }
        statement.bindString(7, entity.getProtocol());
        final int _tmp = entity.getBlocked() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM connection_logs WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM connection_logs";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ConnectionLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfConnectionLog.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
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
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ConnectionLog>> getRecentLogs() {
    final String _sql = "SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 500";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"connection_logs"}, new Callable<List<ConnectionLog>>() {
      @Override
      @NonNull
      public List<ConnectionLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfDestDomain = CursorUtil.getColumnIndexOrThrow(_cursor, "destDomain");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<ConnectionLog> _result = new ArrayList<ConnectionLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConnectionLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final String _tmpDestDomain;
            if (_cursor.isNull(_cursorIndexOfDestDomain)) {
              _tmpDestDomain = null;
            } else {
              _tmpDestDomain = _cursor.getString(_cursorIndexOfDestDomain);
            }
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new ConnectionLog(_tmpId,_tmpPackageName,_tmpAppName,_tmpDestIp,_tmpDestPort,_tmpDestDomain,_tmpProtocol,_tmpBlocked,_tmpTimestamp);
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
  public Flow<List<ConnectionLog>> getLogsForPackage(final String pkg) {
    final String _sql = "SELECT * FROM connection_logs WHERE packageName = ? ORDER BY timestamp DESC LIMIT 200";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pkg);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"connection_logs"}, new Callable<List<ConnectionLog>>() {
      @Override
      @NonNull
      public List<ConnectionLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfDestDomain = CursorUtil.getColumnIndexOrThrow(_cursor, "destDomain");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "blocked");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<ConnectionLog> _result = new ArrayList<ConnectionLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConnectionLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final String _tmpDestDomain;
            if (_cursor.isNull(_cursorIndexOfDestDomain)) {
              _tmpDestDomain = null;
            } else {
              _tmpDestDomain = _cursor.getString(_cursorIndexOfDestDomain);
            }
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final boolean _tmpBlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBlocked);
            _tmpBlocked = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new ConnectionLog(_tmpId,_tmpPackageName,_tmpAppName,_tmpDestIp,_tmpDestPort,_tmpDestDomain,_tmpProtocol,_tmpBlocked,_tmpTimestamp);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
