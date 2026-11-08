package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 10): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE engagementId = :engagementId ORDER BY timestamp DESC")
    fun getActivitiesForEngagement(engagementId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivities()
}
