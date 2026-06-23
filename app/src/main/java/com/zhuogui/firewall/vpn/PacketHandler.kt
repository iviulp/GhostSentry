package com.zhuogui.firewall.vpn

import java.nio.ByteBuffer

/**
 * IP/TCP/UDP/DNS 数据包解析器
 */
object PacketHandler {

    // IP 协议号
    const val PROTO_ICMP = 1
    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    /**
     * 解析后的连接信息
     */
    data class PacketInfo(
        val protocol: Int,          // 6=TCP, 17=UDP
        val srcIp: String,
        val srcPort: Int,
        val dstIp: String,
        val dstPort: Int,
        val tcpFlags: Int = 0,      // TCP 标志位
        val isSYN: Boolean = false,
        val isFIN: Boolean = false,
        val isRST: Boolean = false,
        val domain: String? = null, // DNS 查询域名
        val payloadOffset: Int = 0,
        val payloadLength: Int = 0
    )

    /**
     * 解析 IP 包，返回 PacketInfo
     */
    fun parsePacket(buffer: ByteBuffer): PacketInfo? {
        if (buffer.remaining() < 20) return null

        val startPos = buffer.position()
        val versionAndHeaderLen = buffer.get(startPos).toInt() and 0xFF
        val version = versionAndHeaderLen shr 4
        if (version != 4) return null // 只处理 IPv4

        val headerLen = (versionAndHeaderLen and 0x0F) * 4
        if (buffer.remaining() < headerLen) return null

        val protocol = buffer.get(startPos + 9).toInt() and 0xFF

        // 源 IP
        val srcIp = ByteArray(4)
        buffer.position(startPos + 12)
        buffer.get(srcIp)
        val srcIpStr = srcIp.joinToString(".") { (it.toInt() and 0xFF).toString() }

        // 目标 IP
        val dstIp = ByteArray(4)
        buffer.get(dstIp)
        val dstIpStr = dstIp.joinToString(".") { (it.toInt() and 0xFF).toString() }

        if (protocol != PROTO_TCP && protocol != PROTO_UDP) {
            return PacketInfo(
                protocol = protocol,
                srcIp = srcIpStr,
                srcPort = 0,
                dstIp = dstIpStr,
                dstPort = 0
            )
        }

        // 传输层头部
        val transportStart = startPos + headerLen
        if (buffer.remaining() < transportStart + 8 - buffer.position()) return null

        buffer.position(transportStart)
        val srcPort = (buffer.get().toInt() and 0xFF shl 8) or (buffer.get().toInt() and 0xFF)
        val dstPort = (buffer.get().toInt() and 0xFF shl 8) or (buffer.get().toInt() and 0xFF)

        var tcpFlags = 0
        var isSYN = false
        var isFIN = false
        var isRST = false
        var tcpHeaderLen = headerLen + 20

        if (protocol == PROTO_TCP) {
            // TCP 头: 偏移+保留(4bit) + 标志(8bit, 从第13字节开始)
            buffer.position(transportStart + 12)
            val offsetAndReserved = buffer.get().toInt() and 0xFF
            val dataOffset = ((offsetAndReserved shr 4) and 0x0F) * 4
            tcpFlags = buffer.get().toInt() and 0xFF
            isSYN = (tcpFlags and 0x02) != 0
            isFIN = (tcpFlags and 0x01) != 0
            isRST = (tcpFlags and 0x04) != 0
            tcpHeaderLen = headerLen + dataOffset
        }

        // DNS 解析
        var domain: String? = null
        if (protocol == PROTO_UDP && dstPort == 53) {
            val dnsStart = transportStart + 8
            if (buffer.remaining() >= dnsStart + 12) {
                domain = parseDnsQuery(buffer, dnsStart)
            }
        }

        // 计算 payload
        val totalLen = ((buffer.get(startPos + 2).toInt() and 0xFF) shl 8) or
                (buffer.get(startPos + 3).toInt() and 0xFF)
        val payloadOffset = tcpHeaderLen
        val payloadLength = (startPos + totalLen) - tcpHeaderLen

        return PacketInfo(
            protocol = protocol,
            srcIp = srcIpStr,
            srcPort = srcPort,
            dstIp = dstIpStr,
            dstPort = dstPort,
            tcpFlags = tcpFlags,
            isSYN = isSYN,
            isFIN = isFIN,
            isRST = isRST,
            domain = domain,
            payloadOffset = payloadOffset,
            payloadLength = maxOf(0, payloadLength)
        )
    }

    /**
     * 解析 DNS 查询中的域名
     */
    private fun parseDnsQuery(buffer: ByteBuffer, offset: Int): String? {
        try {
            // DNS 头部 12 字节，跳过
            var pos = offset + 12
            if (pos >= buffer.limit()) return null

            val parts = mutableListOf<String>()
            while (pos < buffer.limit()) {
                val len = buffer.get(pos).toInt() and 0xFF
                if (len == 0) break
                if (len > 63) return null // 压缩指针，跳过
                pos++
                if (pos + len > buffer.limit()) return null
                val bytes = ByteArray(len)
                for (i in 0 until len) {
                    bytes[i] = buffer.get(pos + i)
                }
                parts.add(String(bytes, Charsets.UTF_8))
                pos += len
            }
            return if (parts.isNotEmpty()) parts.joinToString(".") else null
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 构建 TCP RST 包
     */
    fun buildTcpRst(original: PacketInfo, buffer: ByteBuffer): ByteArray? {
        // 简化实现：返回 null 让上层丢弃
        return null
    }
}
