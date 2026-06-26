package com.zhuogui.firewall.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记录每个 APP 的联网信息
 */
@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val uid: Int,
    /** 是否允许联网 (null = 未设置 = 默认允许) */
    val allowed: Boolean? = null,
    /** 是否允许 WiFi 联网 */
    val wifiAllowed: Boolean? = null,
    /** 是否允许移动数据联网 */
    val mobileAllowed: Boolean? = null,
    /** 是否使用上游代理 (默认 false) */
    val useProxy: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
