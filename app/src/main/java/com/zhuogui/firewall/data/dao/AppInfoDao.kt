package com.zhuogui.firewall.data.dao

import androidx.room.*
import com.zhuogui.firewall.data.entity.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Query("SELECT * FROM app_info ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: Int): AppInfo?

    @Query("SELECT * FROM app_info WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppInfo)

    @Update
    suspend fun update(app: AppInfo)

    @Query("UPDATE app_info SET allowed = :allowed WHERE packageName = :pkg")
    suspend fun setAllowed(pkg: String, allowed: Boolean?)

    @Query("UPDATE app_info SET allowed = :allowed")
    suspend fun setAllAllowed(allowed: Boolean?)

    @Query("UPDATE app_info SET wifiAllowed = :w, mobileAllowed = :m WHERE packageName = :pkg")
    suspend fun setNetworkType(pkg: String, w: Boolean?, m: Boolean?)

    @Delete
    suspend fun delete(app: AppInfo)
}
