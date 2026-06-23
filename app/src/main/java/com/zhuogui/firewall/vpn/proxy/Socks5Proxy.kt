package com.zhuogui.firewall.vpn.proxy

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * SOCKS5 代理客户端
 *
 * 用于将 VPN 流量链式转发到上游 SOCKS5 代理（如 Shadowsocks、Clash 等），
 * 实现与现有 VPN/代理的共存。
 *
 * SOCKS5 协议（RFC 1928）：
 * 1. 握手：客户端发送 [0x05, 0x01, 0x00]（版本5, 1种认证, 无认证）
 * 2. 代理回复：[0x05, 0x00]（版本5, 无认证）
 * 3. CONNECT：客户端发送 [0x05, 0x01, 0x00, 0x01, IP(4), PORT(2)]
 * 4. 代理回复：[0x05, 0x00, 0x00, 0x01, IP(4), PORT(2)]
 */
object Socks5Proxy {

    private const val TAG = "Socks5Proxy"
    private const val SOCKS5_VERSION = 0x05
    private const val CMD_CONNECT = 0x01
    private const val ATYP_IPV4 = 0x01
    private const val ATYP_DOMAIN = 0x03
    private const val METHOD_NO_AUTH = 0x00
    private const val REPLY_SUCCESS = 0x00

    data class ProxyConfig(
        val host: String,
        val port: Int,
        val enabled: Boolean = false
    )

    /**
     * 通过 SOCKS5 代理建立 TCP 连接到目标地址
     * @return 已连接并完成 SOCKS5 握手的 Socket
     */
    fun connect(
        config: ProxyConfig,
        dstHost: String,
        dstPort: Int,
        timeout: Int = 10000
    ): Socket? {
        try {
            // 1. 连接到代理服务器
            val socket = Socket()
            socket.connect(InetSocketAddress(config.host, config.port), timeout)
            socket.soTimeout = timeout

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // 2. SOCKS5 握手
            if (!handshake(input, output)) {
                Log.e(TAG, "SOCKS5 handshake failed")
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                return null
            }

            // 3. 发送 CONNECT 命令
            if (!sendConnect(input, output, dstHost, dstPort)) {
                Log.e(TAG, "SOCKS5 CONNECT failed for $dstHost:$dstPort")
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                return null
            }

            Log.d(TAG, "SOCKS5 connected to $dstHost:$dstPort via ${config.host}:${config.port}")
            return socket

        } catch (e: Exception) {
            Log.e(TAG, "SOCKS5 connect error: ${e.message}")
            return null
        }
    }

    /**
     * SOCKS5 握手（无认证方式）
     */
    private fun handshake(input: InputStream, output: OutputStream): Boolean {
        try {
            // 发送握手请求：版本5, 1种方法, 方法0x00（无认证）
            output.write(byteArrayOf(0x05, 0x01, 0x00))
            output.flush()

            // 读取响应：版本(1) + 方法(1)
            val response = ByteArray(2)
            if (input.read(response) != 2) return false

            return response[0].toInt() == SOCKS5_VERSION &&
                    response[1].toInt() == METHOD_NO_AUTH
        } catch (e: Exception) {
            Log.e(TAG, "Handshake error: ${e.message}")
            return false
        }
    }

    /**
     * 发送 SOCKS5 CONNECT 命令
     */
    private fun sendConnect(
        input: InputStream,
        output: OutputStream,
        host: String,
        port: Int
    ): Boolean {
        try {
            // 判断是 IP 还是域名
            val isIp = host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))

            val request = if (isIp) {
                // IPv4 地址
                val ipParts = host.split(".").map { it.toInt() and 0xFF }
                byteArrayOf(
                    0x05, CMD_CONNECT, 0x00, ATYP_IPV4,
                    ipParts[0].toByte(), ipParts[1].toByte(),
                    ipParts[2].toByte(), ipParts[3].toByte(),
                    (port shr 8).toByte(), (port and 0xFF).toByte()
                )
            } else {
                // 域名
                val domainBytes = host.toByteArray(Charsets.UTF_8)
                val requestBytes = ByteArray(7 + domainBytes.size)
                requestBytes[0] = 0x05
                requestBytes[1] = CMD_CONNECT
                requestBytes[2] = 0x00
                requestBytes[3] = ATYP_DOMAIN
                requestBytes[4] = domainBytes.size.toByte()
                System.arraycopy(domainBytes, 0, requestBytes, 5, domainBytes.size)
                requestBytes[5 + domainBytes.size] = (port shr 8).toByte()
                requestBytes[6 + domainBytes.size] = (port and 0xFF).toByte()
                requestBytes
            }

            output.write(request)
            output.flush()

            // 读取响应（前 4 字节 + 地址 + 端口）
            val header = ByteArray(4)
            if (input.read(header) != 4) return false

            if (header[1].toInt() != REPLY_SUCCESS) {
                Log.e(TAG, "SOCKS5 reply error code: ${header[1]}")
                return false
            }

            // 读取剩余地址和端口（长度取决于地址类型）
            val atyp = header[3].toInt()
            val addrLen = when (atyp) {
                ATYP_IPV4 -> 4 + 2  // IPv4(4) + Port(2)
                ATYP_DOMAIN -> {
                    val len = input.read()
                    len + 2  // 域名长度 + Port(2)
                }

                else -> 4 + 2  // 默认 IPv4
            }

            val remaining = ByteArray(addrLen)
            var totalRead = 0
            while (totalRead < addrLen) {
                val n = input.read(remaining, totalRead, addrLen - totalRead)
                if (n < 0) return false
                totalRead += n
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "CONNECT error: ${e.message}")
            return false
        }
    }
}
