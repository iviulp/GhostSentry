package com.zhuogui.firewall.data.dao

import androidx.room.*
import com.zhuogui.firewall.data.entity.FirewallRule
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallRuleDao {

    @Query("SELECT * FROM firewall_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<FirewallRule>>

    @Query("SELECT * FROM firewall_rules WHERE packageName = :pkg OR packageName = '*'")
    fun getRulesForPackage(pkg: String): Flow<List<FirewallRule>>

    @Query(
        """
        SELECT * FROM firewall_rules
        WHERE (packageName = :pkg OR packageName = '*')
        AND blocked = 1
    """
    )
    suspend fun getBlockRulesForPackage(pkg: String): List<FirewallRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FirewallRule): Long

    @Update
    suspend fun update(rule: FirewallRule)

    @Delete
    suspend fun delete(rule: FirewallRule)

    @Query("DELETE FROM firewall_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM firewall_rules WHERE packageName = :pkg AND target = :target")
    suspend fun deleteByTarget(pkg: String, target: String)
}
