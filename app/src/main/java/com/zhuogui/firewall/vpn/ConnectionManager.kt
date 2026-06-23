package com.zhuogui.firewall.vpn

import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.ConcurrentHashMap

/**
 * 连接管理器：通过 /proc/net/tcp 和 /proc/net/udp 映射连接 -> UID
 */
object ConnectionManager {

    private const val TAG = "ConnectionManager"

    // 缓存: "srcIP:srcPort" -> UID
    private val uidCache = ConcurrentHashMap<String, Int>()

    // IP -> 域名映射 (从 DNS 解析)
    private val ipToDomain = ConcurrentHashMap<String, String>()

    /**
     * 根据源 IP 和端口查找 UID (Android 10+ 优先使用系统 API，旧版本降级使用 /proc)
     */
    fun getUidForConnection(
        context: android.content.Context,
        protocol: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ): Int {
        val key = "$srcIp:$srcPort"
        uidCache[key]?.let { return it }

        var uid = -1

        // 1. Android Q (10) 及以上优先使用 ConnectivityManager.getConnectionOwnerUid
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                if (cm != null) {
                    val local = java.net.InetSocketAddress(java.net.InetAddress.getByName(srcIp), srcPort)
                    val remote = java.net.InetSocketAddress(java.net.InetAddress.getByName(dstIp), dstPort)
                    val ipProto = if (protocol == PacketHandler.PROTO_TCP) {
                        android.system.OsConstants.IPPROTO_TCP
                    } else {
                        android.system.OsConstants.IPPROTO_UDP
                    }
                    uid = cm.getConnectionOwnerUid(ipProto, local, remote)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getConnectionOwnerUid error: ${e.message}")
            }
        }

        // 2. 降级使用读取 /proc/net 机制
        if (uid < 0) {
            uid = parseUidFromProc(srcIp, srcPort)
        }

        if (uid >= 0) {
            uidCache[key] = uid
        }
        return uid
    }

    /**
     * 解析 /proc/net/tcp 和 /proc/net/udp 获取 UID
     */
    private fun parseUidFromProc(srcIp: String, srcPort: Int): Int {
        val hexIp = ipToHexLE(srcIp)
        val hexPort = String.format("%04X", srcPort)

        val files = arrayOf("/proc/net/tcp", "/proc/net/tcp6", "/proc/net/udp", "/proc/net/udp6")
        for (file in files) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    reader.readLine() // 跳过标题行
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val parts = line!!.trim().split("\\s+".toRegex())
                        if (parts.size < 8) continue

                        val localAddr = parts[1] // 格式: IP:PORT (hex)
                        val colonIdx = localAddr.lastIndexOf(':')
                        if (colonIdx < 0) continue

                        val localIp = localAddr.substring(0, colonIdx)
                        val localPort = localAddr.substring(colonIdx + 1)

                        if (localIp.equals(hexIp, ignoreCase = true) &&
                            localPort.equals(hexPort, ignoreCase = true)
                        ) {
                            val uid = parts.getOrNull(7)?.toIntOrNull() ?: -1
                            if (uid >= 0) return uid
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error reading $file: ${e.message}")
            }
        }
        return -1
    }

    /**
     * IP 转小端十六进制 (用于 /proc/net 匹配)
     */
    private fun ipToHexLE(ip: String): String {
        val parts = ip.split(".")
        if (parts.size != 4) return ip
        // /proc/net 中 IP 是小端序
        return String.format(
            "%02X%02X%02X%02X",
            parts[3].toInt(),
            parts[2].toInt(),
            parts[1].toInt(),
            parts[0].toInt()
        )
    }

    /**
     * 记录域名 -> IP 映射
     */
    fun addDomainMapping(ip: String, domain: String) {
        ipToDomain[ip] = domain
    }

    /**
     * 根据 IP 获取域名
     */
    fun getDomainForIp(ip: String): String? {
        return ipToDomain[ip]
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        uidCache.clear()
        ipToDomain.clear()
    }
}
