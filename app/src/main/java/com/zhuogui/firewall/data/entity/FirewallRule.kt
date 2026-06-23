package com.zhuogui.firewall.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 防火墙规则：可以针对某个 APP 禁止某个域名/IP
 */
@Entity(tableName = "firewall_rules")
data class FirewallRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 目标 APP 包名，"*" 表示所有 APP */
    val packageName: String,
    /** 目标域名或 IP */
    val target: String,
    /** true = 阻止, false = 允许 */
    val blocked: Boolean = true,
    /** 规则类型: "domain" 或 "ip" */
    val type: String = "domain",
    val createdAt: Long = System.currentTimeMillis()
)
