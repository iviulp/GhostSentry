package com.zhuogui.firewall.data.dao;

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
import com.zhuogui.firewall.data.entity.AppInfo;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class AppInfoDao_Impl implements AppInfoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AppInfo> __insertionAdapterOfAppInfo;

  private final EntityDeletionOrUpdateAdapter<AppInfo> __deletionAdapterOfAppInfo;

  private final EntityDeletionOrUpdateAdapter<AppInfo> __updateAdapterOfAppInfo;

  private final SharedSQLiteStatement __preparedStmtOfSetAllowed;

  private final SharedSQLiteStatement __preparedStmtOfSetAllAllowed;

  private final SharedSQLiteStatement __preparedStmtOfSetNetworkType;

  private final SharedSQLiteStatement __preparedStmtOfSetUseProxy;

  public AppInfoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAppInfo = new EntityInsertionAdapter<AppInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_info` (`packageName`,`appName`,`uid`,`allowed`,`wifiAllowed`,`mobileAllowed`,`useProxy`,`lastSeen`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppInfo entity) {
        statement.bindString(1, entity.getPackageName());
        statement.bindString(2, entity.getAppName());
        statement.bindLong(3, entity.getUid());
        final Integer _tmp = entity.getAllowed() == null ? null : (entity.getAllowed() ? 1 : 0);
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        final Integer _tmp_1 = entity.getWifiAllowed() == null ? null : (entity.getWifiAllowed() ? 1 : 0);
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp_1);
        }
        final Integer _tmp_2 = entity.getMobileAllowed() == null ? null : (entity.getMobileAllowed() ? 1 : 0);
        if (_tmp_2 == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp_2);
        }
        final int _tmp_3 = entity.getUseProxy() ? 1 : 0;
        statement.bindLong(7, _tmp_3);
        statement.bindLong(8, entity.getLastSeen());
      }
    };
    this.__deletionAdapterOfAppInfo = new EntityDeletionOrUpdateAdapter<AppInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `app_info` WHERE `packageName` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppInfo entity) {
        statement.bindString(1, entity.getPackageName());
      }
    };
    this.__updateAdapterOfAppInfo = new EntityDeletionOrUpdateAdapter<AppInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `app_info` SET `packageName` = ?,`appName` = ?,`uid` = ?,`allowed` = ?,`wifiAllowed` = ?,`mobileAllowed` = ?,`useProxy` = ?,`lastSeen` = ? WHERE `packageName` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppInfo entity) {
        statement.bindString(1, entity.getPackageName());
        statement.bindString(2, entity.getAppName());
        statement.bindLong(3, entity.getUid());
        final Integer _tmp = entity.getAllowed() == null ? null : (entity.getAllowed() ? 1 : 0);
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        final Integer _tmp_1 = entity.getWifiAllowed() == null ? null : (entity.getWifiAllowed() ? 1 : 0);
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp_1);
        }
        final Integer _tmp_2 = entity.getMobileAllowed() == null ? null : (entity.getMobileAllowed() ? 1 : 0);
        if (_tmp_2 == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp_2);
        }
        final int _tmp_3 = entity.getUseProxy() ? 1 : 0;
        statement.bindLong(7, _tmp_3);
        statement.bindLong(8, entity.getLastSeen());
        statement.bindString(9, entity.getPackageName());
      }
    };
    this.__preparedStmtOfSetAllowed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_info SET allowed = ? WHERE packageName = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetAllAllowed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_info SET allowed = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetNetworkType = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_info SET wifiAllowed = ?, mobileAllowed = ? WHERE packageName = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetUseProxy = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_info SET useProxy = ? WHERE packageName = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AppInfo app, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppInfo.insert(app);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final AppInfo app, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAppInfo.handle(app);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final AppInfo app, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAppInfo.handle(app);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setAllowed(final String pkg, final Boolean allowed,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetAllowed.acquire();
        int _argIndex = 1;
        final Integer _tmp = allowed == null ? null : (allowed ? 1 : 0);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        _argIndex = 2;
        _stmt.bindString(_argIndex, pkg);
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
          __preparedStmtOfSetAllowed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setAllAllowed(final Boolean allowed, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetAllAllowed.acquire();
        int _argIndex = 1;
        final Integer _tmp = allowed == null ? null : (allowed ? 1 : 0);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
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
          __preparedStmtOfSetAllAllowed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setNetworkType(final String pkg, final Boolean w, final Boolean m,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetNetworkType.acquire();
        int _argIndex = 1;
        final Integer _tmp = w == null ? null : (w ? 1 : 0);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        _argIndex = 2;
        final Integer _tmp_1 = m == null ? null : (m ? 1 : 0);
        if (_tmp_1 == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp_1);
        }
        _argIndex = 3;
        _stmt.bindString(_argIndex, pkg);
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
          __preparedStmtOfSetNetworkType.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setUseProxy(final String pkg, final boolean useProxy,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetUseProxy.acquire();
        int _argIndex = 1;
        final int _tmp = useProxy ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, pkg);
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
          __preparedStmtOfSetUseProxy.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AppInfo>> getAllApps() {
    final String _sql = "SELECT * FROM app_info ORDER BY appName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_info"}, new Callable<List<AppInfo>>() {
      @Override
      @NonNull
      public List<AppInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "allowed");
          final int _cursorIndexOfWifiAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "wifiAllowed");
          final int _cursorIndexOfMobileAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "mobileAllowed");
          final int _cursorIndexOfUseProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "useProxy");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final List<AppInfo> _result = new ArrayList<AppInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppInfo _item;
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final int _tmpUid;
            _tmpUid = _cursor.getInt(_cursorIndexOfUid);
            final Boolean _tmpAllowed;
            final Integer _tmp;
            if (_cursor.isNull(_cursorIndexOfAllowed)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfAllowed);
            }
            _tmpAllowed = _tmp == null ? null : _tmp != 0;
            final Boolean _tmpWifiAllowed;
            final Integer _tmp_1;
            if (_cursor.isNull(_cursorIndexOfWifiAllowed)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getInt(_cursorIndexOfWifiAllowed);
            }
            _tmpWifiAllowed = _tmp_1 == null ? null : _tmp_1 != 0;
            final Boolean _tmpMobileAllowed;
            final Integer _tmp_2;
            if (_cursor.isNull(_cursorIndexOfMobileAllowed)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getInt(_cursorIndexOfMobileAllowed);
            }
            _tmpMobileAllowed = _tmp_2 == null ? null : _tmp_2 != 0;
            final boolean _tmpUseProxy;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfUseProxy);
            _tmpUseProxy = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            _item = new AppInfo(_tmpPackageName,_tmpAppName,_tmpUid,_tmpAllowed,_tmpWifiAllowed,_tmpMobileAllowed,_tmpUseProxy,_tmpLastSeen);
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
  public Object getByUid(final int uid, final Continuation<? super AppInfo> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE uid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, uid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AppInfo>() {
      @Override
      @Nullable
      public AppInfo call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "allowed");
          final int _cursorIndexOfWifiAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "wifiAllowed");
          final int _cursorIndexOfMobileAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "mobileAllowed");
          final int _cursorIndexOfUseProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "useProxy");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final AppInfo _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final int _tmpUid;
            _tmpUid = _cursor.getInt(_cursorIndexOfUid);
            final Boolean _tmpAllowed;
            final Integer _tmp;
            if (_cursor.isNull(_cursorIndexOfAllowed)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfAllowed);
            }
            _tmpAllowed = _tmp == null ? null : _tmp != 0;
            final Boolean _tmpWifiAllowed;
            final Integer _tmp_1;
            if (_cursor.isNull(_cursorIndexOfWifiAllowed)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getInt(_cursorIndexOfWifiAllowed);
            }
            _tmpWifiAllowed = _tmp_1 == null ? null : _tmp_1 != 0;
            final Boolean _tmpMobileAllowed;
            final Integer _tmp_2;
            if (_cursor.isNull(_cursorIndexOfMobileAllowed)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getInt(_cursorIndexOfMobileAllowed);
            }
            _tmpMobileAllowed = _tmp_2 == null ? null : _tmp_2 != 0;
            final boolean _tmpUseProxy;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfUseProxy);
            _tmpUseProxy = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            _result = new AppInfo(_tmpPackageName,_tmpAppName,_tmpUid,_tmpAllowed,_tmpWifiAllowed,_tmpMobileAllowed,_tmpUseProxy,_tmpLastSeen);
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
  public Object getByPackage(final String pkg, final Continuation<? super AppInfo> $completion) {
    final String _sql = "SELECT * FROM app_info WHERE packageName = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, pkg);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AppInfo>() {
      @Override
      @Nullable
      public AppInfo call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "packageName");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "allowed");
          final int _cursorIndexOfWifiAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "wifiAllowed");
          final int _cursorIndexOfMobileAllowed = CursorUtil.getColumnIndexOrThrow(_cursor, "mobileAllowed");
          final int _cursorIndexOfUseProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "useProxy");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final AppInfo _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final int _tmpUid;
            _tmpUid = _cursor.getInt(_cursorIndexOfUid);
            final Boolean _tmpAllowed;
            final Integer _tmp;
            if (_cursor.isNull(_cursorIndexOfAllowed)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfAllowed);
            }
            _tmpAllowed = _tmp == null ? null : _tmp != 0;
            final Boolean _tmpWifiAllowed;
            final Integer _tmp_1;
            if (_cursor.isNull(_cursorIndexOfWifiAllowed)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getInt(_cursorIndexOfWifiAllowed);
            }
            _tmpWifiAllowed = _tmp_1 == null ? null : _tmp_1 != 0;
            final Boolean _tmpMobileAllowed;
            final Integer _tmp_2;
            if (_cursor.isNull(_cursorIndexOfMobileAllowed)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getInt(_cursorIndexOfMobileAllowed);
            }
            _tmpMobileAllowed = _tmp_2 == null ? null : _tmp_2 != 0;
            final boolean _tmpUseProxy;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfUseProxy);
            _tmpUseProxy = _tmp_3 != 0;
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            _result = new AppInfo(_tmpPackageName,_tmpAppName,_tmpUid,_tmpAllowed,_tmpWifiAllowed,_tmpMobileAllowed,_tmpUseProxy,_tmpLastSeen);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
