package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.EngagementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EngagementDao {
    @Query("SELECT * FROM engagements ORDER BY createdAt DESC")
    fun getAllEngagements(): Flow<List<EngagementEntity>>

    @Query("SELECT * FROM engagements WHERE id = :id")
    fun getEngagementById(id: String): Flow<EngagementEntity?>

    @Query("SELECT * FROM engagements WHERE id = :id")
    suspend fun getEngagementByIdDirect(id: String): EngagementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngagement(engagement: EngagementEntity)

    @Update
    suspend fun updateEngagement(engagement: EngagementEntity)

    @Delete
    suspend fun deleteEngagement(engagement: EngagementEntity)

    @Query("DELETE FROM engagements WHERE id = :id")
    suspend fun deleteEngagementById(id: String)

    @Query("SELECT COUNT(*) FROM engagements")
    fun getEngagementCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM engagements WHERE status = 'Active'")
    fun getActiveEngagementCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM engagements WHERE status = 'Completed'")
    fun getCompletedEngagementCount(): Flow<Int>

    @Query("SELECT * FROM engagements WHERE title LIKE '%' || :query || '%' OR client LIKE '%' || :query || '%' OR authorizedScope LIKE '%' || :query || '%'")
    fun searchEngagements(query: String): Flow<List<EngagementEntity>>

    @Query("DELETE FROM engagements")
    suspend fun deleteAll()
}

