# GhostSentry (幽灵哨兵/原卓GUI防火墙)

GhostSentry is a premium, high-performance Android network firewall with upstream SOCKS5 chaining and NAT transparent proxying. Gain total control over your device's traffic.

GhostSentry (幽灵哨兵) 是一款专为安卓设计、轻量且高颜值的网络拦截防火墙。支持与主流代理客户端并存的 SOCKS5 链式代理转发，以及高性能的 NAT 透明代理重写，助你彻底掌控设备后台流量。

---

## 🚀 App Icon / 应用图标

![GhostSentry App Icon](./app_icon.jpg)

---

## 🏷️ Recommended Name / 建议命名

*   **GhostSentry (幽灵哨兵)**
    *   *Why:* Translates the original "捉鬼 (Zhuogui)" theme perfectly. "Ghost" refers to hidden tracking SDKs, background telemetry, and stealth connections. "Sentry" indicates reliable, real-time protection.

---

## ⚡ Key Features / 核心亮点

### 🇺🇸 English
- 🔗 **SOCKS5 Chain Proxy Support:** Resolve VPN conflicts. Forward allowed traffic directly to an upstream local proxy (e.g. Clash, Shadowsocks, v2ray) to preserve bypass/unblocking rules.
- ⚡ **High-Performance NAT Rewriting:** Pure kernel-assisted socket redirection. Skip slow JVM TCP stack emulation and enjoy full-speed, lag-free throughput.
- 📱 **Per-App Granular Inspection:** Monitor real-time endpoints (domain, IP, port) requested by individual apps.
- 🛡️ **Fine-Grained Blocking:** Toggle specific connections, wildcards, or full domains on the fly.
- 🎨 **Sleek Modern UI:** Clean, dark-mode-first dashboard tailored for high usability.

### 🇨🇳 中文
- 🔗 **首创 SOCKS5 链式代理**：告别 VPN 冲突。防火墙拦截净化流量后，自动将允许的请求递交给本地上游代理，完美兼容代理客户端。
- ⚡ **极致性能的 NAT 重写**：利用系统级协议栈自动处理 TCP 握手与流量，相比传统 VPN 防火墙延迟更低、吞吐更高。
- 📱 **应用级细粒度监控**：实时抓取并呈现每个 APP 的网络端点（域名、IP、端口）。
- 🛡️ **连接/域名级热拦截**：支持一键对特定域名、通配符或 IP 实施即时阻断，规则热加载即刻生效。
- 🎨 **现代感极佳的 UI**：深色调极简美学设计，状态一目了然，极具高级感。

---

## 🛠️ Build & Packaging Instructions / 编译与打包指南

This project is configured as a standard Android Gradle project, including built-in configurations for signing release APKs.

### 🇺🇸 English Build Guide

#### Prerequisites
- JDK 17 or higher
- Android SDK installed with Command-line tools (or Android Studio)

#### How to Build
To package and generate the installation packages, execute the following commands in the project root directory:

*   **Build Release APK (Signed with Proguard Optimization):**
    ```bash
    ./gradlew assembleRelease
    ```
    *Output Path:* `app/build/outputs/apk/release/app-release.apk`

*   **Build Debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
    *Output Path:* `app/build/outputs/apk/debug/app-debug.apk`

*   **Clean Build Cache:**
    ```bash
    ./gradlew clean
    ```

---

### 🇨🇳 中文编译指南

#### 构建前提
- JDK 17 或更高版本
- 安装了 Android SDK（命令行工具或通过 Android Studio）

#### 如何编译打包
在项目根目录下执行以下 Gradle 命令进行编译和打包：

*   **打包 Release 正式版 APK（已签名，已开启混淆优化）：**
    ```bash
    ./gradlew assembleRelease
    ```
    *安装包输出路径:* `app/build/outputs/apk/release/app-release.apk`

*   **打包 Debug 开发测试版 APK：**
    ```bash
    ./gradlew assembleDebug
    ```
    *安装包输出路径:* `app/build/outputs/apk/debug/app-debug.apk`

*   **清理编译缓存：**
    ```bash
    ./gradlew clean
    ```
