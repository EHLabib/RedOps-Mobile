package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.ToolObservation
import kotlinx.coroutines.flow.Flow

interface ToolObservationRepository {
    fun getAllObservations(): Flow<List<ToolObservation>>
    fun getObservationsForEngagement(engagementId: String): Flow<List<ToolObservation>>
    suspend fun getObservationById(id: String): ToolObservation?
    suspend fun saveObservation(observation: ToolObservation)
    suspend fun updateObservation(observation: ToolObservation)
    suspend fun deleteObservation(id: String)
}

