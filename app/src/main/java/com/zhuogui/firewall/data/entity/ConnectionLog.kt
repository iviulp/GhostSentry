package com.zhuogui.firewall.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 连接日志：记录每次网络请求
 */
@Entity(tableName = "connection_logs")
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 来源 APP 包名 */
    val packageName: String,
    /** 来源 APP 名称 */
    val appName: String,
    /** 目标 IP */
    val destIp: String,
    /** 目标端口 */
    val destPort: Int,
    /** 目标域名 (如果能解析到) */
    val destDomain: String? = null,
    /** 协议: TCP/UDP */
    val protocol: String,
    /** 是否被阻止 */
    val blocked: Boolean = false,
    /** 时间戳 */
    val timestamp: Long = System.currentTimeMillis()
)
