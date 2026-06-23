package com.zhuogui.firewall.vpn

import android.util.Log
import com.zhuogui.firewall.vpn.PacketHandler.Companion.PROTO_TCP
import com.zhuogui.firewall.vpn.PacketHandler.Companion.PROTO_UDP
import com.zhuogui.firewall.vpn.PacketHandler.PacketInfo
import com.zhuogui.firewall.vpn.proxy.Socks5Proxy
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.channels.spi.AbstractSelectableChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Socket 转发器：在 TUN 和真实 Socket 之间转发数据
 *
 * 支持两种模式：
 * 1. 直连模式：直接使用 NIO SocketChannel 连接目标
 * 2. 代理模式：通过 SOCKS5 代理连接目标（用于与现有 VPN 共存）
 */
class SocketForwarder(
    private val tunWrite: (ByteBuffer) -> Unit,
    private val protectSocket: ((java.net.Socket) -> Boolean)? = null
) {
    companion object {
        private const val TAG = "SocketForwarder"
        private const val BUFFER_SIZE = 32767
    }

    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val activeConnections = ConcurrentHashMap<String, Connection>()

    // 代理配置
    @Volatile
    var proxyConfig: Socks5Proxy.ProxyConfig? = null

    data class Connection(
        val key: String,
        val channel: AbstractSelectableChannel?,
        val socket: java.net.Socket?,
        val srcIp: String,
        val srcPort: Int,
        val dstIp: String,
        val dstPort: Int,
        val protocol: Int,
        val active: AtomicBoolean = AtomicBoolean(true)
    )

    /**
     * 处理 TCP 连接
     */
    fun handleTcp(packet: PacketInfo, rawPacket: ByteBuffer) {
        val key = "${packet.srcIp}:${packet.srcPort}->${packet.dstIp}:${packet.dstPort}"

        val existing = activeConnections[key]
        if (existing != null) {
            if (packet.isFIN || packet.isRST) {
                closeConnection(key)
                return
            }
            forwardTcpData(existing, rawPacket, packet)
            return
        }

        if (!packet.isSYN) return

        try {
            val proxy = proxyConfig?.takeIf { it.enabled }

            if (proxy != null) {
                // 代理模式：通过 SOCKS5 连接
                handleTcpViaProxy(key, packet, proxy)
            } else {
                // 直连模式：直接 SocketChannel
                handleTcpDirect(key, packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TCP connect error: ${e.message}")
        }
    }

    /**
     * 直连模式 TCP
     */
    private fun handleTcpDirect(key: String, packet: PacketInfo) {
        val channel = SocketChannel.open()
        channel.configureBlocking(false)
        channel.connect(InetSocketAddress(packet.dstIp, packet.dstPort))

        val conn = Connection(
            key = key,
            channel = channel,
            socket = null,
            srcIp = packet.srcIp,
            srcPort = packet.srcPort,
            dstIp = packet.dstIp,
            dstPort = packet.dstPort,
            protocol = PROTO_TCP
        )
        activeConnections[key] = conn

        executor.submit {
            handleTcpConnection(conn, channel)
        }
    }

    /**
     * 代理模式 TCP
     */
    private fun handleTcpViaProxy(key: String, packet: PacketInfo, proxy: Socks5Proxy.ProxyConfig) {
        val targetHost = packet.dstIp
        val targetPort = packet.dstPort

        val socket = Socks5Proxy.connect(proxy, targetHost, targetPort)
        if (socket == null) {
            Log.e(TAG, "SOCKS5 proxy connect failed for $targetHost:$targetPort")
            return
        }

        // 保护代理 socket，避免走自身 VPN
        protectSocket?.invoke(socket)

        val conn = Connection(
            key = key,
            channel = null,
            socket = socket,
            srcIp = packet.srcIp,
            srcPort = packet.srcPort,
            dstIp = packet.dstIp,
            dstPort = packet.dstPort,
            protocol = PROTO_TCP
        )
        activeConnections[key] = conn

        // 启动双向转发线程
        thread(name = "proxy-tcp-$key") {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                while (conn.active.get()) {
                    // 读取代理响应 → 写入 TUN
                    val buffer = ByteArray(BUFFER_SIZE)
                    val len = input.read(buffer)
                    if (len < 0) break
                    if (len > 0) {
                        tunWrite(ByteBuffer.wrap(buffer, 0, len))
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Proxy TCP read closed: ${e.message}")
            } finally {
                closeConnection(key)
            }
        }
    }

    /**
     * 处理 TCP 连接生命周期（直连模式）
     */
    private fun handleTcpConnection(conn: Connection, channel: SocketChannel) {
        try {
            val selector = Selector.open()
            channel.register(selector, SelectionKey.OP_CONNECT)

            while (conn.active.get()) {
                val ready = selector.select(3000)
                if (ready == 0) continue

                val keys = selector.selectedKeys().iterator()
                while (keys.hasNext()) {
                    val key = keys.next()
                    keys.remove()

                    if (key.isConnectable) {
                        try {
                            channel.finishConnect()
                            key.interestOps(SelectionKey.OP_READ)
                        } catch (e: Exception) {
                            closeConnection(conn.key)
                            return
                        }
                    }

                    if (key.isReadable) {
                        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                        val len = channel.read(buffer)
                        if (len < 0) {
                            closeConnection(conn.key)
                            return
                        }
                        if (len > 0) {
                            buffer.flip()
                            tunWrite(buffer)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "TCP connection closed: ${e.message}")
        } finally {
            closeConnection(conn.key)
        }
    }

    /**
     * 转发 TCP 数据 (TUN → Socket)
     */
    private fun forwardTcpData(conn: Connection, rawPacket: ByteBuffer, packet: PacketInfo) {
        try {
            if (packet.payloadLength <= 0) return

            val payload = ByteArray(packet.payloadLength)
            val pos = rawPacket.position()
            rawPacket.position(packet.payloadOffset)
            rawPacket.get(payload)
            rawPacket.position(pos)

            if (conn.socket != null) {
                // 代理模式：写入 Socket
                conn.socket.getOutputStream().write(payload)
            } else {
                // 直连模式：写入 Channel
                (conn.channel as? SocketChannel)?.let {
                    if (it.isConnected) {
                        it.write(ByteBuffer.wrap(payload))
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "TCP forward error: ${e.message}")
        }
    }

    /**
     * 处理 UDP 数据包
     */
    fun handleUdp(packet: PacketInfo, rawPacket: ByteBuffer) {
        val key = "${packet.srcIp}:${packet.srcPort}->${packet.dstIp}:${packet.dstPort}"

        try {
            var conn = activeConnections[key]
            if (conn == null) {
                val channel = DatagramChannel.open()
                channel.configureBlocking(false)
                channel.connect(InetSocketAddress(packet.dstIp, packet.dstPort))
                conn = Connection(
                    key = key,
                    channel = channel,
                    socket = null,
                    srcIp = packet.srcIp,
                    srcPort = packet.srcPort,
                    dstIp = packet.dstIp,
                    dstPort = packet.dstPort,
                    protocol = PROTO_UDP
                )
                activeConnections[key] = conn

                executor.submit {
                    handleUdpReceive(conn)
                }
            }

            if (packet.payloadLength <= 0) return
            val channel = conn.channel as? DatagramChannel ?: return
            val payload = ByteArray(packet.payloadLength)
            val pos = rawPacket.position()
            rawPacket.position(packet.payloadOffset)
            rawPacket.get(payload)
            rawPacket.position(pos)

            if (payload.isNotEmpty()) {
                channel.write(ByteBuffer.wrap(payload))
            }
        } catch (e: Exception) {
            Log.e(TAG, "UDP error: ${e.message}")
        }
    }

    private fun handleUdpReceive(conn: Connection) {
        try {
            val channel = conn.channel as? DatagramChannel ?: return
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            while (conn.active.get()) {
                buffer.clear()
                channel.receive(buffer)
                buffer.flip()
                if (buffer.hasRemaining()) {
                    tunWrite(buffer)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP receive closed: ${e.message}")
        } finally {
            closeConnection(conn.key)
        }
    }

    private fun closeConnection(key: String) {
        val conn = activeConnections.remove(key) ?: return
        conn.active.set(false)
        try {
            conn.channel?.close()
        } catch (e: Exception) { /* ignore */
        }
        try {
            conn.socket?.close()
        } catch (e: Exception) { /* ignore */
        }
    }

    fun closeAll() {
        activeConnections.keys.forEach { closeConnection(it) }
        executor.shutdownNow()
    }
}
