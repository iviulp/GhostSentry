package com.zhuogui.firewall.data;

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
import com.zhuogui.firewall.data.dao.AppInfoDao;
import com.zhuogui.firewall.data.dao.AppInfoDao_Impl;
import com.zhuogui.firewall.data.dao.ConnectionLogDao;
import com.zhuogui.firewall.data.dao.ConnectionLogDao_Impl;
import com.zhuogui.firewall.data.dao.FirewallRuleDao;
import com.zhuogui.firewall.data.dao.FirewallRuleDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AppInfoDao _appInfoDao;

  private volatile FirewallRuleDao _firewallRuleDao;

  private volatile ConnectionLogDao _connectionLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_info` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `uid` INTEGER NOT NULL, `allowed` INTEGER, `wifiAllowed` INTEGER, `mobileAllowed` INTEGER, `useProxy` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, PRIMARY KEY(`packageName`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `firewall_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `target` TEXT NOT NULL, `blocked` INTEGER NOT NULL, `type` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `connection_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `destIp` TEXT NOT NULL, `destPort` INTEGER NOT NULL, `destDomain` TEXT, `protocol` TEXT NOT NULL, `blocked` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '590ef6e3349b50836eca91036a6093a7')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `app_info`");
        db.execSQL("DROP TABLE IF EXISTS `firewall_rules`");
        db.execSQL("DROP TABLE IF EXISTS `connection_logs`");
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
        final HashMap<String, TableInfo.Column> _columnsAppInfo = new HashMap<String, TableInfo.Column>(8);
        _columnsAppInfo.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("appName", new TableInfo.Column("appName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("uid", new TableInfo.Column("uid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("allowed", new TableInfo.Column("allowed", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("wifiAllowed", new TableInfo.Column("wifiAllowed", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("mobileAllowed", new TableInfo.Column("mobileAllowed", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("useProxy", new TableInfo.Column("useProxy", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppInfo.put("lastSeen", new TableInfo.Column("lastSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppInfo = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppInfo = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppInfo = new TableInfo("app_info", _columnsAppInfo, _foreignKeysAppInfo, _indicesAppInfo);
        final TableInfo _existingAppInfo = TableInfo.read(db, "app_info");
        if (!_infoAppInfo.equals(_existingAppInfo)) {
          return new RoomOpenHelper.ValidationResult(false, "app_info(com.zhuogui.firewall.data.entity.AppInfo).\n"
                  + " Expected:\n" + _infoAppInfo + "\n"
                  + " Found:\n" + _existingAppInfo);
        }
        final HashMap<String, TableInfo.Column> _columnsFirewallRules = new HashMap<String, TableInfo.Column>(6);
        _columnsFirewallRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFirewallRules.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFirewallRules.put("target", new TableInfo.Column("target", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFirewallRules.put("blocked", new TableInfo.Column("blocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFirewallRules.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFirewallRules.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFirewallRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFirewallRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFirewallRules = new TableInfo("firewall_rules", _columnsFirewallRules, _foreignKeysFirewallRules, _indicesFirewallRules);
        final TableInfo _existingFirewallRules = TableInfo.read(db, "firewall_rules");
        if (!_infoFirewallRules.equals(_existingFirewallRules)) {
          return new RoomOpenHelper.ValidationResult(false, "firewall_rules(com.zhuogui.firewall.data.entity.FirewallRule).\n"
                  + " Expected:\n" + _infoFirewallRules + "\n"
                  + " Found:\n" + _existingFirewallRules);
        }
        final HashMap<String, TableInfo.Column> _columnsConnectionLogs = new HashMap<String, TableInfo.Column>(9);
        _columnsConnectionLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("appName", new TableInfo.Column("appName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("destIp", new TableInfo.Column("destIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("destPort", new TableInfo.Column("destPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("destDomain", new TableInfo.Column("destDomain", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("protocol", new TableInfo.Column("protocol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("blocked", new TableInfo.Column("blocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConnectionLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConnectionLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConnectionLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoConnectionLogs = new TableInfo("connection_logs", _columnsConnectionLogs, _foreignKeysConnectionLogs, _indicesConnectionLogs);
        final TableInfo _existingConnectionLogs = TableInfo.read(db, "connection_logs");
        if (!_infoConnectionLogs.equals(_existingConnectionLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "connection_logs(com.zhuogui.firewall.data.entity.ConnectionLog).\n"
                  + " Expected:\n" + _infoConnectionLogs + "\n"
                  + " Found:\n" + _existingConnectionLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "590ef6e3349b50836eca91036a6093a7", "b2537b4f5736e8531b627215b5c515c9");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "app_info","firewall_rules","connection_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `app_info`");
      _db.execSQL("DELETE FROM `firewall_rules`");
      _db.execSQL("DELETE FROM `connection_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(AppInfoDao.class, AppInfoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FirewallRuleDao.class, FirewallRuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ConnectionLogDao.class, ConnectionLogDao_Impl.getRequiredConverters());
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
  public AppInfoDao appInfoDao() {
    if (_appInfoDao != null) {
      return _appInfoDao;
    } else {
      synchronized(this) {
        if(_appInfoDao == null) {
          _appInfoDao = new AppInfoDao_Impl(this);
        }
        return _appInfoDao;
      }
    }
  }

  @Override
  public FirewallRuleDao firewallRuleDao() {
    if (_firewallRuleDao != null) {
      return _firewallRuleDao;
    } else {
      synchronized(this) {
        if(_firewallRuleDao == null) {
          _firewallRuleDao = new FirewallRuleDao_Impl(this);
        }
        return _firewallRuleDao;
      }
    }
  }

  @Override
  public ConnectionLogDao connectionLogDao() {
    if (_connectionLogDao != null) {
      return _connectionLogDao;
    } else {
      synchronized(this) {
        if(_connectionLogDao == null) {
          _connectionLogDao = new ConnectionLogDao_Impl(this);
        }
        return _connectionLogDao;
      }
    }
  }
}
