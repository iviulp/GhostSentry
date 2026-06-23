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

    suspend fun insertOrUpdateApp(app: AppInfo) = db.appInfoDao().insert(app)

    suspend fun setAppAllowed(pkg: String, allowed: Boolean?) =
        db.appInfoDao().setAllowed(pkg, allowed)

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
