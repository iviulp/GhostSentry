package com.zhuogui.firewall.data.dao

import androidx.room.*
import com.zhuogui.firewall.data.entity.ConnectionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionLogDao {

    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogs(): Flow<List<ConnectionLog>>

    @Query("SELECT * FROM connection_logs WHERE packageName = :pkg ORDER BY timestamp DESC LIMIT 200")
    fun getLogsForPackage(pkg: String): Flow<List<ConnectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ConnectionLog)

    @Query("DELETE FROM connection_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM connection_logs")
    suspend fun deleteAll()
}
