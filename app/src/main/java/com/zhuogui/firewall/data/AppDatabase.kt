package com.zhuogui.firewall.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zhuogui.firewall.data.dao.AppInfoDao
import com.zhuogui.firewall.data.dao.ConnectionLogDao
import com.zhuogui.firewall.data.dao.FirewallRuleDao
import com.zhuogui.firewall.data.entity.AppInfo
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.data.entity.FirewallRule

@Database(
    entities = [AppInfo::class, FirewallRule::class, ConnectionLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appInfoDao(): AppInfoDao
    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun connectionLogDao(): ConnectionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zhuogui_firewall.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
