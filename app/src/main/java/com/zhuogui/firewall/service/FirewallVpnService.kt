package com.zhuogui.firewall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zhuogui.firewall.MainActivity
import com.zhuogui.firewall.ZhuoguiApp
import com.zhuogui.firewall.data.entity.AppInfo
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.data.entity.FirewallRule
import com.zhuogui.firewall.data.repository.FirewallRepository
import com.zhuogui.firewall.util.AppInfoHelper
import com.zhuogui.firewall.vpn.ConnectionManager
import com.zhuogui.firewall.vpn.PacketHandler
import com.zhuogui.firewall.vpn.SocketForwarder
import com.zhuogui.firewall.vpn.proxy.Socks5Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class FirewallVpnService : VpnService() {

    companion object {
        private const val TAG = "FirewallVpnService"
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.8.0.1"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "firewall_vpn"

        // 服务状态
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        // 实时连接日志
        private val _liveConnections = MutableStateFlow<List<ConnectionLog>>(emptyList())
        val liveConnections: StateFlow<List<ConnectionLog>> = _liveConnections

        // 规则重载信号
        private val reloadSignal = Channel<Unit>(Channel.CONFLATED)

        // 代理配置
        private const val PREF_PROXY_HOST = "proxy_host"
        private const val PREF_PROXY_PORT = "proxy_port"
        private const val PREF_PROXY_ENABLED = "proxy_enabled"

        /**
         * 通知 VPN 服务重新加载规则（由 UI 层调用）
         */
        fun notifyRulesChanged() {
            reloadSignal.trySend(Unit)
        }

        /**
         * 获取当前代理配置
         */
        fun getProxyConfig(): Socks5Proxy.ProxyConfig {
            val prefs = ZhuoguiApp.instance.getSharedPreferences("vpn_proxy", Context.MODE_PRIVATE)
            return Socks5Proxy.ProxyConfig(
                host = prefs.getString(PREF_PROXY_HOST, "") ?: "",
                port = prefs.getInt(PREF_PROXY_PORT, 1080),
                enabled = prefs.getBoolean(PREF_PROXY_ENABLED, false)
            )
        }

        /**
         * 设置代理配置
         */
        fun setProxyConfig(host: String, port: Int, enabled: Boolean) {
            val prefs = ZhuoguiApp.instance.getSharedPreferences("vpn_proxy", Context.MODE_PRIVATE)
            prefs.edit()
                .putString(PREF_PROXY_HOST, host)
                .putInt(PREF_PROXY_PORT, port)
                .putBoolean(PREF_PROXY_ENABLED, enabled)
                .apply()
        }
    }

    private var serviceJob = SupervisorJob()
    private val serviceScope get() = CoroutineScope(Dispatchers.IO + serviceJob)
    private val active = AtomicBoolean(false)
    private var tunFd: ParcelFileDescriptor? = null
    private lateinit var repository: FirewallRepository
    private lateinit var forwarder: SocketForwarder

    // 全局阻止规则缓存
    private val globalBlockRules = ConcurrentHashMap<String, Boolean>()

    // 按 APP 分组的阻止规则缓存
    private val appBlockRules = ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>()

    // 被完全阻止的 APP 集合
    private val blockedApps = ConcurrentHashMap.newKeySet<String>()

    override fun onCreate() {
        super.onCreate()
        repository = FirewallRepository(ZhuoguiApp.instance.database)
        createNotificationChannel()
        serviceScope.launch {
            loadRules()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    /**
     * 启动 VPN
     */
    private fun startVpn() {
        if (active.get()) return

        try {
            // 创建 TUN 接口
            val builder = Builder()
                .setSession("卓 GUI 防火墙")
                .addAddress(VPN_ADDRESS, 24)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .setMtu(VPN_MTU)
                .setBlocking(true)
                .addDisallowedApplication(packageName) // 排除自身

            tunFd = builder.establish()
            if (tunFd == null) {
                Log.e(TAG, "Failed to establish VPN")
                return
            }

            active.set(true)
            _isRunning.value = true

            // 前台通知
            startForeground(NOTIFICATION_ID, buildNotification())

            // 初始化转发器（带代理支持和 protect 回调）
            val proxyConfig = loadProxyConfig()
            forwarder = SocketForwarder(
                tunWrite = { buffer -> writeToTun(buffer) },
                protectSocket = { socket ->
                    try {
                        protect(socket)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "protect() failed: ${e.message}")
                        false
                    }
                },
                protectDatagramSocket = { datagramSocket ->
                    try {
                        protect(datagramSocket)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "protect() failed for DatagramSocket: ${e.message}")
                        false
                    }
                }
            )
            forwarder.proxyConfig = proxyConfig

            // 加载规则
            serviceScope.launch {
                loadRules()
            }

            // 监听规则变更
            serviceScope.launch {
                for (unit in reloadSignal) {
                    reloadRules()
                }
            }

            // 开始读取 TUN 数据
            serviceScope.launch {
                readTunPackets()
            }

            Log.i(TAG, "VPN started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "VPN start error: ${e.message}", e)
            stopVpn()
        }
    }

    /**
     * 停止 VPN
     */
    private fun stopVpn() {
        active.set(false)
        _isRunning.value = false
        if (::forwarder.isInitialized) {
            forwarder.closeAll()
        }
        ConnectionManager.clearCache()
        serviceJob.cancel()
        serviceJob = SupervisorJob()
        try {
            tunFd?.close()
        } catch (e: Exception) { /* ignore */
        }
        tunFd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN stopped")
    }

    /**
     * 从 TUN 读取数据包
     */
    private suspend fun readTunPackets() = withContext(Dispatchers.IO) {
        val fd: FileDescriptor = tunFd?.fileDescriptor ?: return@withContext
        val inputStream = FileInputStream(fd)
        val buffer = ByteArray(VPN_MTU)

        while (active.get()) {
            try {
                val length = inputStream.read(buffer)
                if (length <= 0) continue

                val packet = ByteBuffer.wrap(buffer, 0, length)
                processPacket(packet)
            } catch (e: Exception) {
                if (active.get()) {
                    Log.e(TAG, "Read error: ${e.message}")
                }
            }
        }
    }

    /**
     * 处理单个数据包
     */
    private suspend fun processPacket(packet: ByteBuffer) {
        val info = PacketHandler.parsePacket(packet) ?: return

        // 只处理 TCP/UDP
        if (info.protocol != PacketHandler.PROTO_TCP &&
            info.protocol != PacketHandler.PROTO_UDP
        ) return

        // 查找 UID
        val uid = ConnectionManager.getUidForConnection(
            context = this,
            protocol = info.protocol,
            srcIp = info.srcIp,
            srcPort = info.srcPort,
            dstIp = info.dstIp,
            dstPort = info.dstPort
        )
        val appInfo = resolveApp(uid)

        val packageName = appInfo?.packageName ?: "unknown"
        val appName = appInfo?.appName ?: "Unknown"

        // 处理 DNS 域名记录
        if (info.domain != null) {
            ConnectionManager.addDomainMapping(info.dstIp, info.domain)
        }

        // 获取域名
        val domain = info.domain ?: ConnectionManager.getDomainForIp(info.dstIp)

        // 检查是否被阻止
        val blocked = checkBlocked(packageName, info.dstIp, domain)

        // 异步记录日志与更新UI，绝对不阻塞网卡读包循环
        val log = ConnectionLog(
            packageName = packageName,
            appName = appName,
            destIp = info.dstIp,
            destPort = info.dstPort,
            destDomain = domain,
            protocol = if (info.protocol == PacketHandler.PROTO_TCP) "TCP" else "UDP",
            blocked = blocked
        )
        serviceScope.launch {
            repository.insertLog(log)
            updateLiveConnections(log)
        }

        if (blocked) {
            Log.d(TAG, "Blocked: $packageName -> $domain (${info.dstIp}:${info.dstPort})")
            if (info.protocol == PacketHandler.PROTO_TCP) {
                // 如果被阻止且是 TCP 连接，发送 RST 包通知客户端关闭连接
                val rstPacket = PacketHandler.buildTcpPacket(
                    srcIp = info.dstIp, srcPort = info.dstPort,
                    dstIp = info.srcIp, dstPort = info.srcPort,
                    seq = 0, ack = info.tcpSeq + 1,
                    flags = 0x04.toByte() // RST
                )
                writeToTun(ByteBuffer.wrap(rstPacket))
            }
            return
        }

        // 允许通过：直接交给用户态转发器处理
        if (info.protocol == PacketHandler.PROTO_TCP) {
            forwarder.handleTcp(info, packet)
        } else {
            forwarder.handleUdp(info, packet)
        }
    }

    /**
     * 解析 UID 对应的 APP 信息
     */
    private suspend fun resolveApp(uid: Int): AppInfo? {
        if (uid < 0) return null

        // 先从数据库查
        var app = repository.getAppByUid(uid)
        if (app != null) {
            // 更新最后活跃时间
            repository.insertOrUpdateApp(app.copy(lastSeen = System.currentTimeMillis()))
            return app
        }

        // 从 PackageManager 查
        val pmApp = AppInfoHelper.getAppByUid(this, uid)
        if (pmApp != null) {
            repository.insertOrUpdateApp(pmApp)
            return pmApp
        }

        return null
    }

    /**
     * 检查是否应该阻止该连接
     */
    private fun checkBlocked(packageName: String, destIp: String, domain: String?): Boolean {
        // 1. 检查 APP 是否被完全阻止
        if (packageName in blockedApps) return true

        // 2. 检查全局阻止规则
        if (globalBlockRules[destIp] == true) return true
        if (domain != null && globalBlockRules[domain] == true) return true

        // 3. 检查 APP 特定阻止规则
        val appRules = appBlockRules[packageName] ?: return false
        if (appRules[destIp] == true) return true
        if (domain != null && appRules[domain] == true) return true

        return false
    }

    /**
     * 加载所有规则
     */
    private suspend fun loadRules() {
        try {
            val db = ZhuoguiApp.instance.database

            // 加载 APP 阻止状态
            val allApps = db.appInfoDao().getAllApps()
            try {
                val apps = allApps.first()
                apps.forEach { app ->
                    if (app.allowed == false) {
                        blockedApps.add(app.packageName)
                    } else {
                        blockedApps.remove(app.packageName)
                    }
                }
            } catch (_: NoSuchElementException) {
                // 空列表
            }

            // 加载防火墙规则
            val allRules = db.firewallRuleDao().getAllRules()
            try {
                val rules = allRules.first()
                rules.forEach { rule ->
                    if (!rule.blocked) return@forEach
                    if (rule.packageName == "*") {
                        globalBlockRules[rule.target] = true
                    } else {
                        appBlockRules.getOrPut(rule.packageName) {
                            ConcurrentHashMap()
                        }[rule.target] = true
                    }
                }
            } catch (_: NoSuchElementException) {
                // 空列表
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load rules error: ${e.message}")
        }
    }

    /**
     * 重新加载规则 (外部调用)
     */
    fun reloadRules() {
        globalBlockRules.clear()
        appBlockRules.clear()
        blockedApps.clear()
        serviceScope.launch {
            loadRules()
        }
    }

    /**
     * 更新实时连接列表
     */
    private fun updateLiveConnections(log: ConnectionLog) {
        val current = _liveConnections.value.toMutableList()
        current.add(0, log)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _liveConnections.value = current
    }

    /**
     * 写数据到 TUN
     */
    private fun writeToTun(buffer: ByteBuffer) {
        try {
            val fd = tunFd?.fileDescriptor ?: return
            val outputStream = FileOutputStream(fd)
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            outputStream.write(data)
        } catch (e: Exception) {
            Log.e(TAG, "Write error: ${e.message}")
        }
    }

    /**
     * 构建通知
     */
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("卓 GUI 防火墙")
            .setContentText("正在监控网络流量...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * 创建通知频道
     */
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN 防火墙",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN 防火墙运行状态"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 从 SharedPreferences 加载代理配置
     */
    private fun loadProxyConfig(): Socks5Proxy.ProxyConfig? {
        val prefs = getSharedPreferences("vpn_proxy", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(PREF_PROXY_ENABLED, false)
        if (!enabled) return null
        val host = prefs.getString(PREF_PROXY_HOST, "") ?: ""
        val port = prefs.getInt(PREF_PROXY_PORT, 1080)
        if (host.isEmpty()) return null
        return Socks5Proxy.ProxyConfig(host = host, port = port, enabled = true)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

}
