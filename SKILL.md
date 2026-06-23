# 卓 GUI 防火墙 - 需求规格与架构说明

> 本文档封装完整项目需求，供 AI 代码审查和后续开发参考。

---

## 1. 项目概述

**卓 GUI 防火墙** 是一个 Android 网络防火墙应用，基于 VPN Service 捕获全局流量，按 APP 分类展示，支持域名/IP 级别的访问控制。

## 2. 核心需求

### 2.1 流量捕获（不冲突现有 VPN）
- 使用 `VpnService` 创建本地 TUN 接口捕获所有流量
- **关键约束**：必须支持上游 SOCKS5 代理，以便流量可以链式经过用户已有的 VPN/代理
- 当用户配置了上游代理时，所有放行的 TCP 连接通过 SOCKS5 代理转发
- 应用自身包名被排除在 VPN 外（`addDisallowedApplication`），避免死循环

### 2.2 APP 分类
- 通过 `/proc/net/tcp` 和 `/proc/net/udp` 将源 IP:Port 映射到 UID
- 通过 `PackageManager` 将 UID 映射到包名和应用名
- 缓存映射关系以减少文件读取

### 2.3 域名识别
- 拦截 DNS 查询包（UDP 53），解析查询域名
- 建立 IP → 域名映射表
- 后续 TCP/UDP 连接可通过目标 IP 反查域名

### 2.4 三种显示模式（APP 详情页）
| 模式 | 显示内容 | 说明 |
|------|---------|------|
| 域名模式 | 仅显示域名 | 无域名的连接显示 IP |
| IP 模式 | 仅显示 IP | 有域名的通过 DNS 解析为 IP 展示 |
| 全部模式 | 域名 + IP | 同时展示域名和 IP |

### 2.5 排序支持
- 可按域名字母序排序
- 可按 IP 数值排序
- 可按时间戳排序（默认）

### 2.6 每条连接的控制
- 每条连接右侧有一个 **禁用/放开** 按钮
- 点击禁用：创建一条 `FirewallRule`（阻止该 APP 访问该域名/IP）
- 点击放开：删除对应的 `FirewallRule`
- 规则立即生效（通过 Channel 信号通知 VPN 服务热加载）

### 2.7 APP 级别控制
- 主页 APP 列表每个 APP 有开关，可一键完全阻止/允许该 APP 所有联网
- 点击 APP 进入详情页查看该 APP 的所有连接

## 3. 架构设计

### 3.1 整体架构
```
┌──────────────────────────────────────────────────┐
│ UI Layer (MVVM)                                   │
│  MainActivity → AppListFragment → AppDetailFragment│
│       ↕ ViewModel + LiveData/StateFlow             │
├──────────────────────────────────────────────────┤
│ Data Layer (Room)                                  │
│  AppInfo / ConnectionLog / FirewallRule            │
├──────────────────────────────────────────────────┤
│ VPN Service Layer                                  │
│  FirewallVpnService                                │
│    ├── PacketHandler (IP/TCP/UDP/DNS parsing)      │
│    ├── ConnectionManager (UID mapping, /proc/net)  │
│    ├── SocketForwarder (TCP/UDP forwarding)        │
│    │     └── Socks5Proxy (upstream proxy)          │
│    └── RuleEngine (blocking logic)                 │
└──────────────────────────────────────────────────┘
```

### 3.2 数据流
```
App 发出请求
  → Android 系统路由到 TUN 接口
  → FirewallVpnService 读取 IP 包
  → PacketHandler 解析协议/地址/端口
  → ConnectionManager 查 UID → 包名
  → 检查 DNS 查询，记录域名
  → RuleEngine 匹配规则：
      ├── 阻止 → 丢弃包，记录日志
      └── 放行 → SocketForwarder 转发
            ├── 无代理 → 直接连接
            └── 有代理 → SOCKS5 代理连接
```

### 3.3 数据库表设计
```sql
-- APP 信息
app_info (packageName TEXT PK, appName TEXT, uid INT, allowed INT?, ...)

-- 防火墙规则
firewall_rules (id INT PK AUTO, packageName TEXT, target TEXT, 
                blocked INT, type TEXT, createdAt INT)

-- 连接日志
connection_logs (id INT PK AUTO, packageName TEXT, appName TEXT,
                 destIp TEXT, destPort INT, destDomain TEXT?,
                 protocol TEXT, blocked INT, timestamp INT)
```

## 4. UI 导航流程

```
MainActivity
  └── AppListFragment (APP 列表)
       ├── 每个 APP 行：名称 + 包名 + 阻止开关
       └── 点击 APP → 进入 AppDetailFragment
            ├── 顶部：APP 名称 + 返回按钮
            ├── 模式切换：域名 | IP | 全部
            ├── 排序切换：按域名 | 按IP | 按时间
            └── 连接列表
                 ├── 每条连接：域名/IP + 端口 + 协议 + 时间
                 └── 每条连接：禁用/放开 按钮
```

## 5. 关键约束

### 5.1 VPN 共存
- Android 仅允许一个 VpnService 同时运行
- 通过 SOCKS5 上游代理实现链式代理
- 代理配置存储在 SharedPreferences
- 代理连接通过 `protect()` 保护，绕过自身 VPN

### 5.2 Android 10+ 限制
- `/proc/net/tcp` 在 Android 10+ 可能无法读取
- 需要处理读取失败的情况，显示 "Unknown" APP
- 备选方案：`NetworkStatsManager` 或 `ConnectivityManager`

### 5.3 性能
- 连接日志最多保留 500 条在内存中
- 数据库日志定期清理（保留最近 10000 条）
- UID 映射缓存避免频繁读取 /proc/net

## 6. 代码审查检查清单

AI 审查代码时应检查以下要点：

- [ ] VpnService 是否正确实现了 `protect()` 保护代理连接
- [ ] SOCKS5 代理是否正确处理了握手和 CONNECT 命令
- [ ] 规则热加载是否通过 Channel 机制正确触发
- [ ] UI 三种模式切换是否正确过滤/转换数据
- [ ] 排序逻辑是否支持域名、IP、时间三种排序
- [ ] 阻止按钮是否正确创建/删除 FirewallRule
- [ ] /proc/net 解析是否处理了异常情况
- [ ] DNS 解析是否在后台线程执行
- [ ] 数据库操作是否在协程中执行
- [ ] 连接日志是否正确记录了 blocked 状态