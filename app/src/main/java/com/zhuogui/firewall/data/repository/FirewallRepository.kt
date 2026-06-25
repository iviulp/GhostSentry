package com.zhuogui.firewall.data.repository

import com.zhuogui.firewall.data.AppDatabase
import com.zhuogui.firewall.data.entity.AppInfo
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.data.entity.FirewallRule
import kotlinx.coroutines.flow.Flow

class FirewallRepository(private val db: AppDatabase) {

    // ========== AppInfo ==========
    fun getAllApps(): Flow<List<AppInfo>> = db.appInfoDao().getAllApps()

    suspend fun getAppByUid(uid: Int): AppInfo? = db.appInfoDao().getByUid(uid)

    suspend fun getAppByPackage(pkg: String): AppInfo? = db.appInfoDao().getByPackage(pkg)

    suspend fun insertOrUpdateApp(app: AppInfo) {
        val existing = db.appInfoDao().getByPackage(app.packageName)
        if (existing == null) {
            db.appInfoDao().insert(app)
        } else {
            // 关键修复：合并更新时必须拷贝保留用户的允许/封锁联网配置与代理配置，否则每次启动应用都会被重置
            db.appInfoDao().insert(app.copy(
                allowed = existing.allowed,
                wifiAllowed = existing.wifiAllowed,
                mobileAllowed = existing.mobileAllowed,
                useProxy = existing.useProxy
            ))
        }
    }

    suspend fun setAppAllowed(pkg: String, allowed: Boolean?) =
        db.appInfoDao().setAllowed(pkg, allowed)

    suspend fun setAppUseProxy(pkg: String, useProxy: Boolean) =
        db.appInfoDao().setUseProxy(pkg, useProxy)

    suspend fun setAllAppsAllowed(allowed: Boolean?) =
        db.appInfoDao().setAllAllowed(allowed)

    // ========== FirewallRule ==========
    fun getAllRules(): Flow<List<FirewallRule>> = db.firewallRuleDao().getAllRules()

    fun getRulesForPackage(pkg: String): Flow<List<FirewallRule>> =
        db.firewallRuleDao().getRulesForPackage(pkg)

    suspend fun getBlockRulesForPackage(pkg: String): List<FirewallRule> =
        db.firewallRuleDao().getBlockRulesForPackage(pkg)

    suspend fun insertRule(rule: FirewallRule): Long =
        db.firewallRuleDao().insert(rule)

    suspend fun updateRule(rule: FirewallRule) =
        db.firewallRuleDao().update(rule)

    suspend fun deleteRule(rule: FirewallRule) =
        db.firewallRuleDao().delete(rule)

    suspend fun deleteRuleById(id: Long) =
        db.firewallRuleDao().deleteById(id)

    suspend fun deleteRuleByTarget(pkg: String, target: String) =
        db.firewallRuleDao().deleteByTarget(pkg, target)

    // ========== ConnectionLog ==========
    fun getRecentLogs(): Flow<List<ConnectionLog>> = db.connectionLogDao().getRecentLogs()

    fun getLogsForPackage(pkg: String): Flow<List<ConnectionLog>> =
        db.connectionLogDao().getLogsForPackage(pkg)

    suspend fun insertLog(log: ConnectionLog) = db.connectionLogDao().insert(log)

    suspend fun deleteOldLogs(before: Long) = db.connectionLogDao().deleteOlderThan(before)

    suspend fun clearAllLogs() = db.connectionLogDao().deleteAll()
}
