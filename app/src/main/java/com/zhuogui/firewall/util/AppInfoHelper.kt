package com.zhuogui.firewall.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.zhuogui.firewall.data.entity.AppInfo

/**
 * 获取已安装 APP 信息
 */
object AppInfoHelper {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.uid >= 10000 } // 过滤系统应用
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    uid = app.uid
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    fun getAppByUid(context: Context, uid: Int): AppInfo? {
        try {
            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid)
            if (packages != null && packages.isNotEmpty()) {
                val pkgName = packages[0]
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                return AppInfo(
                    packageName = pkgName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    uid = uid
                )
            }
        } catch (e: Exception) {
            // 忽略找不到包的情况
        }
        return null
    }
}
