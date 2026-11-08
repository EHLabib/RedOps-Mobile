package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.ToolObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolObservationDao {
    @Query("SELECT * FROM tool_observations WHERE engagementId = :engagementId ORDER BY createdAt DESC")
    fun getObservationsForEngagement(engagementId: String): Flow<List<ToolObservationEntity>>

    @Query("SELECT * FROM tool_observations ORDER BY createdAt DESC")
    fun getAllObservations(): Flow<List<ToolObservationEntity>>

    @Query("SELECT * FROM tool_observations WHERE id = :id")
    suspend fun getObservationById(id: String): ToolObservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ToolObservationEntity)

    @Update
    suspend fun updateObservation(observation: ToolObservationEntity)

    @Delete
    suspend fun deleteObservation(observation: ToolObservationEntity)

    @Query("DELETE FROM tool_observations WHERE id = :id")
    suspend fun deleteObservationById(id: String)
}

