package com.zhuogui.firewall.vpn

import java.nio.ByteBuffer

/**
 * IP/TCP/UDP/DNS 数据包解析与重写工具
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
        val tcpSeq: Long = 0,       // TCP 序列号
        val tcpAck: Long = 0,       // TCP 确认号
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
        val startPos = buffer.position()
        if (buffer.remaining() < 20) return null

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
        if (buffer.limit() < transportStart + 8) return null

        buffer.position(transportStart)
        val srcPort = (buffer.get().toInt() and 0xFF shl 8) or (buffer.get().toInt() and 0xFF)
        val dstPort = (buffer.get().toInt() and 0xFF shl 8) or (buffer.get().toInt() and 0xFF)

        var tcpSeq = 0L
        var tcpAck = 0L
        var tcpFlags = 0
        var isSYN = false
        var isFIN = false
        var isRST = false
        var tcpHeaderLen = headerLen + (if (protocol == PROTO_TCP) 20 else 8)

        if (protocol == PROTO_TCP) {
            if (buffer.limit() < transportStart + 20) return null
            tcpSeq = buffer.getInt(transportStart + 4).toLong() and 0xFFFFFFFFL
            tcpAck = buffer.getInt(transportStart + 8).toLong() and 0xFFFFFFFFL
            
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
            if (buffer.limit() >= dnsStart + 12) {
                domain = parseDnsQuery(buffer, dnsStart)
            }
        }

        // 计算 payload
        val totalLen = ((buffer.get(startPos + 2).toInt() and 0xFF) shl 8) or
                (buffer.get(startPos + 3).toInt() and 0xFF)
        val payloadOffset = tcpHeaderLen
        val payloadLength = (startPos + totalLen) - tcpHeaderLen

        // 恢复 buffer 位置
        buffer.position(startPos)

        return PacketInfo(
            protocol = protocol,
            srcIp = srcIpStr,
            srcPort = srcPort,
            dstIp = dstIpStr,
            dstPort = dstPort,
            tcpSeq = tcpSeq,
            tcpAck = tcpAck,
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
     * 构建一个完整的 IP + TCP 数据包字节数组
     */
    fun buildTcpPacket(
        srcIp: String, srcPort: Int,
        dstIp: String, dstPort: Int,
        seq: Long, ack: Long,
        flags: Byte,
        payload: ByteArray? = null
    ): ByteArray {
        val payloadSize = payload?.size ?: 0
        val tcpLen = 20 + payloadSize
        val ipLen = 20 + tcpLen
        val buffer = ByteBuffer.allocate(ipLen)

        // --- IP 头部 (20 字节) ---
        buffer.put(0x45.toByte()) // Version(4) + IHL(5)
        buffer.put(0.toByte()) // TOS
        buffer.putShort(ipLen.toShort()) // Total Length
        buffer.putShort(1.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buffer.put(64.toByte()) // TTL
        buffer.put(6.toByte()) // Protocol (6 = TCP)
        buffer.putShort(0.toShort()) // Checksum placeholder

        // IP 地址
        val srcParts = srcIp.split(".").map { it.toInt().toByte() }
        buffer.put(srcParts[0]); buffer.put(srcParts[1]); buffer.put(srcParts[2]); buffer.put(srcParts[3])
        val dstParts = dstIp.split(".").map { it.toInt().toByte() }
        buffer.put(dstParts[0]); buffer.put(dstParts[1]); buffer.put(dstParts[2]); buffer.put(dstParts[3])

        // 计算并写入 IP 校验和
        val ipChecksum = calculateChecksum(buffer, 0, 20)
        buffer.putShort(10, ipChecksum.toShort())

        // --- TCP 头部 (20 字节) ---
        val tcpStart = 20
        buffer.putShort(tcpStart, srcPort.toShort())
        buffer.putShort(tcpStart + 2, dstPort.toShort())
        buffer.putInt(tcpStart + 4, seq.toInt())
        buffer.putInt(tcpStart + 8, ack.toInt())
        buffer.putShort(tcpStart + 12, (0x5000 or (flags.toInt() and 0xFF)).toShort()) // Header Length (5 words = 20 bytes) + Flags
        buffer.putShort(tcpStart + 14, 65535.toShort()) // Window Size
        buffer.putShort(tcpStart + 16, 0.toShort()) // Checksum placeholder
        buffer.putShort(tcpStart + 18, 0.toShort()) // Urgent pointer

        // 写入 TCP 数据载荷
        if (payload != null) {
            buffer.position(tcpStart + 20)
            buffer.put(payload)
        }

        // 计算并写入 TCP 伪首部和 TCP 校验和
        val pseudoHeaderSum = calculatePseudoHeaderSum(buffer, 0, 6, tcpLen)
        val tcpChecksum = calculateChecksum(buffer, tcpStart, tcpLen, pseudoHeaderSum)
        buffer.putShort(tcpStart + 16, tcpChecksum.toShort())

        return buffer.array()
    }

    /**
     * 重写 IP 和 TCP/UDP 头部的目标 IP 和目标端口
     */
    fun rewriteDestination(buffer: ByteBuffer, newDstIp: String, newDstPort: Int) {
        val startPos = buffer.position()
        val headerLen = (buffer.get(startPos).toInt() and 0x0F) * 4
        val protocol = buffer.get(startPos + 9).toInt() and 0xFF

        // 写入新目标 IP 到 IP 头部
        val ipParts = newDstIp.split(".").map { it.toInt().toByte() }
        if (ipParts.size == 4) {
            buffer.position(startPos + 16)
            buffer.put(ipParts[0])
            buffer.put(ipParts[1])
            buffer.put(ipParts[2])
            buffer.put(ipParts[3])
        }

        // 写入新目标端口到 TCP/UDP 头部
        buffer.position(startPos + headerLen + 2)
        buffer.put((newDstPort shr 8).toByte())
        buffer.put((newDstPort and 0xFF).toByte())

        // 重新计算校验和
        recalculateChecksums(buffer, startPos, headerLen, protocol)
        buffer.position(startPos)
    }

    /**
     * 重写 IP 和 TCP/UDP 头部的源 IP 和源端口
     */
    fun rewriteSource(buffer: ByteBuffer, newSrcIp: String, newSrcPort: Int) {
        val startPos = buffer.position()
        val headerLen = (buffer.get(startPos).toInt() and 0x0F) * 4
        val protocol = buffer.get(startPos + 9).toInt() and 0xFF

        // 写入新源 IP 到 IP 头部
        val ipParts = newSrcIp.split(".").map { it.toInt().toByte() }
        if (ipParts.size == 4) {
            buffer.position(startPos + 12)
            buffer.put(ipParts[0])
            buffer.put(ipParts[1])
            buffer.put(ipParts[2])
            buffer.put(ipParts[3])
        }

        // 写入新源端口到 TCP/UDP 头部
        buffer.position(startPos + headerLen)
        buffer.put((newSrcPort shr 8).toByte())
        buffer.put((newSrcPort and 0xFF).toByte())

        // 重新计算校验和
        recalculateChecksums(buffer, startPos, headerLen, protocol)
        buffer.position(startPos)
    }

    /**
     * 重新计算 IP 和 TCP/UDP 校验和
     */
    private fun recalculateChecksums(buffer: ByteBuffer, startPos: Int, headerLen: Int, protocol: Int) {
        val totalLen = ((buffer.get(startPos + 2).toInt() and 0xFF) shl 8) or
                (buffer.get(startPos + 3).toInt() and 0xFF)

        // 1. IP 校验和
        buffer.putShort(startPos + 10, 0.toShort()) // 清空原有 IP 校验和
        val ipChecksum = calculateChecksum(buffer, startPos, headerLen)
        buffer.putShort(startPos + 10, ipChecksum.toShort())

        // 2. TCP/UDP 校验和
        val transportLen = totalLen - headerLen
        val transportStart = startPos + headerLen

        if (protocol == PROTO_TCP) {
            buffer.putShort(transportStart + 16, 0.toShort()) // 清空原有 TCP 校验和
            val pseudoHeader = calculatePseudoHeaderSum(buffer, startPos, protocol, transportLen)
            val tcpChecksum = calculateChecksum(buffer, transportStart, transportLen, pseudoHeader)
            buffer.putShort(transportStart + 16, tcpChecksum.toShort())
        } else if (protocol == PROTO_UDP) {
            buffer.putShort(transportStart + 6, 0.toShort()) // 清空原有 UDP 校验和
            val pseudoHeader = calculatePseudoHeaderSum(buffer, startPos, protocol, transportLen)
            val udpChecksum = calculateChecksum(buffer, transportStart, transportLen, pseudoHeader)
            buffer.putShort(transportStart + 6, udpChecksum.toShort())
        }
    }

    private fun calculatePseudoHeaderSum(buffer: ByteBuffer, ipStart: Int, protocol: Int, transportLen: Int): Int {
        var sum = 0
        // 源 IP (4 字节)
        sum += buffer.getShort(ipStart + 12).toInt() and 0xFFFF
        sum += buffer.getShort(ipStart + 14).toInt() and 0xFFFF
        // 目的 IP (4 字节)
        sum += buffer.getShort(ipStart + 16).toInt() and 0xFFFF
        sum += buffer.getShort(ipStart + 18).toInt() and 0xFFFF
        // 协议类型
        sum += protocol
        // 传输层长度
        sum += transportLen
        return sum
    }

    private fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int, initialSum: Int = 0): Int {
        var sum = initialSum
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

    /**
     * 解析 DNS 查询中的域名
     */
    private fun parseDnsQuery(buffer: ByteBuffer, offset: Int): String? {
        try {
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
     * 解析 DNS 响应中的所有 IPv4 地址
     */
    fun parseDnsResponse(payload: ByteArray): List<String> {
        val ips = mutableListOf<String>()
        try {
            val buffer = ByteBuffer.wrap(payload)
            if (buffer.remaining() < 12) return ips

            val id = buffer.getShort()
            val flags = buffer.getShort()
            val qdCount = buffer.getShort().toInt() and 0xFFFF
            val anCount = buffer.getShort().toInt() and 0xFFFF
            val nsCount = buffer.getShort()
            val arCount = buffer.getShort()

            // 跳过 Question 区域
            var pos = 12
            for (i in 0 until qdCount) {
                pos = skipDnsName(buffer, pos)
                pos += 4 // 跳过 QTYPE (2字节) 和 QCLASS (2字节)
            }

            // 解析 Answer 区域
            for (i in 0 until anCount) {
                if (pos >= buffer.limit()) break
                pos = skipDnsName(buffer, pos)
                if (pos + 10 > buffer.limit()) break

                val type = buffer.getShort(pos).toInt() and 0xFFFF
                val clazz = buffer.getShort(pos + 2).toInt() and 0xFFFF
                val ttl = buffer.getInt(pos + 4)
                val dataLen = buffer.getShort(pos + 8).toInt() and 0xFFFF
                pos += 10

                if (type == 1 && dataLen == 4) { // Type A (IPv4)
                    if (pos + 4 <= buffer.limit()) {
                        val ipBytes = ByteArray(4)
                        buffer.position(pos)
                        buffer.get(ipBytes)
                        val ipStr = ipBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                        ips.add(ipStr)
                    }
                }
                pos += dataLen
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
        return ips
    }

    private fun skipDnsName(buffer: ByteBuffer, startPos: Int): Int {
        var pos = startPos
        while (pos < buffer.limit()) {
            val len = buffer.get(pos).toInt() and 0xFF
            if (len == 0) {
                return pos + 1
            }
            if ((len and 0xC0) == 0xC0) { // 压缩指针占 2 字节
                return pos + 2
            }
            pos += 1 + len
        }
        return pos
    }
}

