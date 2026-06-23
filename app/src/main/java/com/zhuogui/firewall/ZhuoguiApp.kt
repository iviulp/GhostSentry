package com.zhuogui.firewall

import android.app.Application
import com.zhuogui.firewall.data.AppDatabase

class ZhuoguiApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: ZhuoguiApp
            private set
    }
}
