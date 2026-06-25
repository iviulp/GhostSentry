package com.zhuogui.firewall.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhuogui.firewall.ZhuoguiApp
import com.zhuogui.firewall.data.entity.AppInfo
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.data.entity.FirewallRule
import com.zhuogui.firewall.data.repository.FirewallRepository
import com.zhuogui.firewall.service.FirewallVpnService
import com.zhuogui.firewall.util.AppInfoHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirewallRepository(ZhuoguiApp.instance.database)
    private val context = application.applicationContext

    // ========== APP 列表 ==========
    val allApps: StateFlow<List<AppInfo>> = repository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ========== 规则 ==========
    val allRules: StateFlow<List<FirewallRule>> = repository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ========== VPN 状态 ==========
    val isVpnRunning: StateFlow<Boolean> = FirewallVpnService.isRunning

    // ========== 选中的 APP ==========
    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage

    init {
        viewModelScope.launch {
            syncInstalledApps()
        }
    }

    fun selectPackage(pkg: String?) {
        _selectedPackage.value = pkg
    }

    /**
     * 获取某个 APP 的所有连接日志
     */
    fun getConnectionsForPackage(pkg: String): Flow<List<ConnectionLog>> {
        return repository.getLogsForPackage(pkg)
    }

    fun syncInstalledApps() {
        viewModelScope.launch {
            val apps = AppInfoHelper.getInstalledApps(context)
            apps.forEach { app ->
                repository.insertOrUpdateApp(app)
            }
        }
    }

    fun setAppAllowed(pkg: String, allowed: Boolean?) {
        viewModelScope.launch {
            repository.setAppAllowed(pkg, allowed)
            notifyVpnReload()
        }
    }

    fun setAllAppsAllowed(allowed: Boolean?) {
        viewModelScope.launch {
            repository.setAllAppsAllowed(allowed)
            notifyVpnReload()
        }
    }

    fun addRule(rule: FirewallRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
            notifyVpnReload()
        }
    }

    fun deleteRule(rule: FirewallRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            notifyVpnReload()
        }
    }

    fun deleteRuleById(id: Long) {
        viewModelScope.launch {
            repository.deleteRuleById(id)
            notifyVpnReload()
        }
    }

    /**
     * 按包名和目标删除规则
     */
    fun deleteRuleByTarget(pkg: String, target: String) {
        viewModelScope.launch {
            repository.deleteRuleByTarget(pkg, target)
            notifyVpnReload()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    private fun notifyVpnReload() {
        FirewallVpnService.notifyRulesChanged()
    }
}
