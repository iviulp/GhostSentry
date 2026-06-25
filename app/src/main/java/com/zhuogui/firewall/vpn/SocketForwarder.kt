package com.zhuogui.firewall.vpn

import android.util.Log
import com.zhuogui.firewall.vpn.proxy.Socks5Proxy
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Socket 转发器：基于纯用户态 TCP/UDP 协议包组装技术，免去本地 Socket 重定向与路由环回烦恼。
 */
class SocketForwarder(
    private val tunWrite: (ByteBuffer) -> Unit,
    val protectSocket: ((Socket) -> Boolean)? = null,
    val protectDatagramSocket: ((DatagramSocket) -> Boolean)? = null
) {
    companion object {
        private const val TAG = "SocketForwarder"
    }

    // 会话表
    val tcpSessions = ConcurrentHashMap<String, TcpSession>()
    val udpSessions = ConcurrentHashMap<String, UdpSession>()

    @Volatile
    var proxyConfig: Socks5Proxy.ProxyConfig? = null

    /**
     * 处理来自客户端的 TCP 数据包
     */
    fun handleTcp(info: PacketHandler.PacketInfo, rawPacket: ByteBuffer, packageName: String, appName: String) {
        val key = "${info.srcIp}:${info.srcPort}->${info.dstIp}:${info.dstPort}"
        
        var session = tcpSessions[key]
        if (session == null) {
            if (!info.isSYN) return // 如果不是 SYN 起手包，直接忽略
            
            session = TcpSession(
                key = key,
                srcIp = info.srcIp,
                srcPort = info.srcPort,
                dstIp = info.dstIp,
                dstPort = info.dstPort,
                packageName = packageName,
                appName = appName,
                forwarder = this,
                tunWrite = tunWrite
            )
            tcpSessions[key] = session
            session.handleClientPacket(info, rawPacket) // 先处理 SYN 初始化 Seq 和 Ack
            session.connectRemote()
        } else {
            session.handleClientPacket(info, rawPacket)
        }
    }

    /**
     * 处理来自客户端的 UDP 数据包
     */
    fun handleUdp(info: PacketHandler.PacketInfo, rawPacket: ByteBuffer, packageName: String) {
        val key = "${info.srcIp}:${info.srcPort}->${info.dstIp}:${info.dstPort}"
        if (info.payloadLength <= 0) return

        val payload = ByteArray(info.payloadLength)
        val pos = rawPacket.position()
        rawPacket.position(info.payloadOffset)
        rawPacket.get(payload)
        rawPacket.position(pos)

        var session = udpSessions[key]
        if (session == null) {
            session = UdpSession(
                key = key,
                srcIp = info.srcIp,
                srcPort = info.srcPort,
                dstIp = info.dstIp,
                dstPort = info.dstPort,
                packageName = packageName,
                forwarder = this,
                tunWrite = tunWrite
            )
            udpSessions[key] = session
            session.start()
        }
        session.sendPayload(payload)
    }

    /**
     * 关闭并清理所有会话
     */
    fun closeAll() {
        tcpSessions.values.forEach { it.close() }
        tcpSessions.clear()
        udpSessions.values.forEach { it.close() }
        udpSessions.clear()
    }

    /**
     * 用户态 TCP 会话状态机
     */
    class TcpSession(
        val key: String,
        val srcIp: String, val srcPort: Int,
        val dstIp: String, val dstPort: Int,
        var packageName: String,
        var appName: String,
        private val forwarder: SocketForwarder,
        private val tunWrite: (ByteBuffer) -> Unit
    ) {
        var clientSeq = 0L
        var clientAck = 0L
        var serverSeq = 1000L
        var serverAck = 0L
        
        var remoteSocket: Socket? = null
        val active = AtomicBoolean(true)
        private val pendingClientData = ArrayList<ByteArray>()

        fun connectRemote() {
            // 1. 立即向客户端发送 SYN-ACK 建立 TCP 连接，使用户态 socket 变为 ESTABLISHED 状态
            sendSynAck()

            thread(name = "TcpConnect-$key") {
                var connStatus = "SUCCESS"
                try {
                    // 等待客户端响应并建立连接状态以使系统记录连接所有者
                    Thread.sleep(20)

                    // 2. 此时连接已建立，getConnectionOwnerUid 将能够 100% 成功返回真实的 UID
                    val realUid = ConnectionManager.getUidForConnection(
                        context = com.zhuogui.firewall.ZhuoguiApp.instance,
                        protocol = PacketHandler.PROTO_TCP,
                        srcIp = srcIp, srcPort = srcPort,
                        dstIp = dstIp, dstPort = dstPort
                    )

                    if (realUid >= 0) {
                        val pm = com.zhuogui.firewall.ZhuoguiApp.instance.packageManager
                        val pkgs = pm.getPackagesForUid(realUid)
                        val resolvedPackageName = pkgs?.firstOrNull() ?: packageName
                        packageName = resolvedPackageName
                        
                        try {
                            val appInfo = pm.getApplicationInfo(resolvedPackageName, 0)
                            appName = pm.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {}
                    }

                    // 3. 安全审计：如果检测出真实包名已被列入黑名单，则立即斩断连接
                    // （由系统服务层判定 checkBlocked，避免黑客应用伪装）
                    val db = com.zhuogui.firewall.ZhuoguiApp.instance.database
                    val isBlocked = try {
                        kotlinx.coroutines.runBlocking {
                            val app = db.appInfoDao().getByPackage(packageName)
                            app?.allowed == false
                        }
                    } catch (e: Exception) {
                        false
                    }

                    if (isBlocked) {
                        Log.i("TcpSession", "Blocked newly resolved package: $packageName")
                        connStatus = "BLOCKED"
                        sendRst()
                        close()
                        return@thread
                    }

                    val proxy = forwarder.proxyConfig?.takeIf { it.enabled }
                    // 确认该客户端是否被用户配置为直连（useProxy = false）
                    val isProxyEnabledForApp = try {
                        kotlinx.coroutines.runBlocking {
                            val app = db.appInfoDao().getByPackage(packageName)
                            app?.useProxy ?: true
                        }
                    } catch (e: Exception) {
                        true
                    }

                    // 4. 选择直连还是走代理：代理应用本身必须直连，用户设置为直连的也直连
                    val finalProxy = if (proxy != null && isProxyEnabledForApp && packageName != proxy.proxyPackage) proxy else null
                    val socket = try {
                        if (finalProxy != null) {
                            Socks5Proxy.connect(finalProxy, dstIp, dstPort, 10000, forwarder.protectSocket)
                        } else {
                            val s = Socket()
                            forwarder.protectSocket?.invoke(s)
                            s.connect(InetSocketAddress(dstIp, dstPort), 10000)
                            s
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        connStatus = "TIMEOUT"
                        throw e
                    } catch (e: Exception) {
                        connStatus = "FAILED"
                        throw e
                    }

                    if (socket == null) {
                        connStatus = "FAILED"
                        sendRst()
                        close()
                        return@thread
                    }
                    remoteSocket = socket
                    
                    // 5. 连接建立，把之前缓存的客户端写数据全部推送给远端
                    synchronized(pendingClientData) {
                        val out = socket.getOutputStream()
                        for (data in pendingClientData) {
                            out.write(data)
                        }
                        out.flush()
                        pendingClientData.clear()
                    }
                    
                    // 启动线程循环读取远端服务器发回的数据，打包成 TCP 回复注入 TUN
                    val input = socket.getInputStream()
                    val buffer = ByteArray(32768)
                    while (active.get() && !socket.isClosed) {
                        val len = input.read(buffer)
                        if (len < 0) break
                        if (len > 0) {
                            sendData(buffer.copyOfRange(0, len))
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Remote connection read ended for $key: ${e.message}")
                } finally {
                    val domain = ConnectionManager.getDomainForIp(dstIp)
                    val finalLog = com.zhuogui.firewall.data.entity.ConnectionLog(
                        packageName = packageName,
                        appName = appName,
                        destIp = dstIp,
                        destPort = dstPort,
                        destDomain = domain,
                        protocol = "TCP",
                        blocked = (connStatus == "BLOCKED"),
                        status = connStatus
                    )
                    com.zhuogui.firewall.service.FirewallVpnService.serviceInstance?.logConnection(finalLog)
                    close()
                }
            }
        }

        fun handleClientPacket(info: PacketHandler.PacketInfo, rawPacket: ByteBuffer) {
            clientSeq = info.tcpSeq
            clientAck = info.tcpAck

            if (info.isSYN) {
                serverAck = info.tcpSeq + 1
            } else if (info.payloadLength > 0) {
                val payload = ByteArray(info.payloadLength)
                val pos = rawPacket.position()
                rawPacket.position(info.payloadOffset)
                rawPacket.get(payload)
                rawPacket.position(pos)

                val socket = remoteSocket
                if (socket != null) {
                    try {
                        val out = socket.getOutputStream()
                        out.write(payload)
                        out.flush()
                        serverAck = info.tcpSeq + info.payloadLength
                        sendAck()
                    } catch (e: Exception) {
                        sendRst()
                        close()
                    }
                } else {
                    synchronized(pendingClientData) {
                        pendingClientData.add(payload)
                    }
                    serverAck = info.tcpSeq + info.payloadLength
                    sendAck()
                }
            } else if (info.isFIN) {
                serverAck = info.tcpSeq + 1
                sendFinAck()
                close()
            } else if (info.isRST) {
                close()
            }
        }

        private fun sendSynAck() {
            val response = PacketHandler.buildTcpPacket(
                srcIp = dstIp, srcPort = dstPort,
                dstIp = srcIp, dstPort = srcPort,
                seq = serverSeq, ack = serverAck,
                flags = 0x12.toByte() // SYN | ACK
            )
            serverSeq++
            tunWrite(ByteBuffer.wrap(response))
        }

        private fun sendAck() {
            val response = PacketHandler.buildTcpPacket(
                srcIp = dstIp, srcPort = dstPort,
                dstIp = srcIp, dstPort = srcPort,
                seq = serverSeq, ack = serverAck,
                flags = 0x10.toByte() // ACK
            )
            tunWrite(ByteBuffer.wrap(response))
        }

        private fun sendData(data: ByteArray) {
            val response = PacketHandler.buildTcpPacket(
                srcIp = dstIp, srcPort = dstPort,
                dstIp = srcIp, dstPort = srcPort,
                seq = serverSeq, ack = serverAck,
                flags = 0x18.toByte(), // PSH | ACK
                payload = data
            )
            serverSeq += data.size
            tunWrite(ByteBuffer.wrap(response))
        }

        private fun sendFinAck() {
            val response = PacketHandler.buildTcpPacket(
                srcIp = dstIp, srcPort = dstPort,
                dstIp = srcIp, dstPort = srcPort,
                seq = serverSeq, ack = serverAck,
                flags = 0x11.toByte() // FIN | ACK
            )
            tunWrite(ByteBuffer.wrap(response))
        }

        private fun sendRst() {
            val response = PacketHandler.buildTcpPacket(
                srcIp = dstIp, srcPort = dstPort,
                dstIp = srcIp, dstPort = srcPort,
                seq = serverSeq, ack = serverAck,
                flags = 0x04.toByte() // RST
            )
            tunWrite(ByteBuffer.wrap(response))
        }

        fun close() {
            if (active.getAndSet(false)) {
                try { remoteSocket?.close() } catch (_: Exception) {}
                forwarder.tcpSessions.remove(key)
            }
        }
    }

    /**
     * 用户态 UDP 会话
     */
    class UdpSession(
        val key: String,
        val srcIp: String, val srcPort: Int,
        val dstIp: String, val dstPort: Int,
        val packageName: String,
        private val forwarder: SocketForwarder,
        private val tunWrite: (ByteBuffer) -> Unit
    ) {
        var remoteSocket: DatagramSocket? = null
        val active = AtomicBoolean(true)

        fun start() {
            try {
                val s = DatagramSocket()
                forwarder.protectDatagramSocket?.invoke(s)
                remoteSocket = s

                thread(name = "UdpReceive-$key") {
                    val buffer = ByteArray(32768)
                    while (active.get() && !s.isClosed) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            s.receive(packet)
                            val len = packet.length
                            if (len > 0) {
                                val payload = packet.data.copyOfRange(0, len)
                                // DNS 响应拦截：将解析出的 IP 与域名绑定，便于详情记录展示域名
                                if (dstPort == 53) {
                                    val domain = ConnectionManager.getPendingDns(srcPort)
                                    if (domain != null) {
                                        val resolvedIps = PacketHandler.parseDnsResponse(payload)
                                        for (ip in resolvedIps) {
                                            ConnectionManager.addDomainMapping(ip, domain)
                                        }
                                    }
                                }
                                sendUdpResponse(payload)
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                    close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Session start failed: ${e.message}")
            }
        }

        fun sendPayload(data: ByteArray) {
            try {
                val packet = DatagramPacket(data, data.size, InetAddress.getByName(dstIp), dstPort)
                remoteSocket?.send(packet)
            } catch (e: Exception) {
                close()
            }
        }

        private fun sendUdpResponse(data: ByteArray) {
            val payloadSize = data.size
            val udpLen = 8 + payloadSize
            val ipLen = 20 + udpLen
            val buffer = ByteBuffer.allocate(ipLen)

            // --- IP Header (UDP) ---
            buffer.put(0x45.toByte())
            buffer.put(0.toByte())
            buffer.putShort(ipLen.toShort())
            buffer.putShort(1.toShort())
            buffer.putShort(0x4000.toShort())
            buffer.put(64.toByte())
            buffer.put(17.toByte()) // Protocol 17 = UDP
            buffer.putShort(0.toShort())

            val srcParts = dstIp.split(".").map { it.toInt().toByte() }
            buffer.put(srcParts[0]); buffer.put(srcParts[1]); buffer.put(srcParts[2]); buffer.put(srcParts[3])
            val dstParts = srcIp.split(".").map { it.toInt().toByte() }
            buffer.put(dstParts[0]); buffer.put(dstParts[1]); buffer.put(dstParts[2]); buffer.put(dstParts[3])

            // IP Checksum
            val ipChecksum = calculateChecksum(buffer, 0, 20)
            buffer.putShort(10, ipChecksum.toShort())

            // --- UDP Header ---
            val udpStart = 20
            buffer.putShort(udpStart, dstPort.toShort())
            buffer.putShort(udpStart + 2, srcPort.toShort())
            buffer.putShort(udpStart + 4, udpLen.toShort())
            buffer.putShort(udpStart + 6, 0.toShort()) // 无 UDP 校验和设为 0

            // 写入载荷
            buffer.position(udpStart + 8)
            buffer.put(data)

            tunWrite(ByteBuffer.wrap(buffer.array()))
        }

        private fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int): Int {
            var sum = 0
            var i = 0
            while (i < length - 1) {
                sum += buffer.getShort(offset + i).toInt() and 0xFFFF
                i += 2
            }
            if (i == length - 1) {
                sum += (buffer.get(offset + i).toInt() and 0xFF) shl 8
            }
            while (sum shr 16 != 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            return (sum.inv() and 0xFFFF)
        }

        fun close() {
            if (active.getAndSet(false)) {
                try { remoteSocket?.close() } catch (_: Exception) {}
                forwarder.udpSessions.remove(key)
            }
        }
    }
}
