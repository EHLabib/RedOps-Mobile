package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ToolObservationDao
import com.redops.mobile.domain.model.ToolObservation
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.ToolObservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ToolObservationRepositoryImpl(
    private val dao: ToolObservationDao
) : ToolObservationRepository {

    override fun getAllObservations(): Flow<List<ToolObservation>> {
        return dao.getAllObservations().map { list -> list.map { it.toDomain() } }
    }

    override fun getObservationsForEngagement(engagementId: String): Flow<List<ToolObservation>> {
        return dao.getObservationsForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getObservationById(id: String): ToolObservation? {
        return dao.getObservationById(id)?.toDomain()
    }

    override suspend fun saveObservation(observation: ToolObservation) {
        dao.insertObservation(observation.toEntity())
    }

    override suspend fun updateObservation(observation: ToolObservation) {
        dao.updateObservation(observation.toEntity())
    }

    override suspend fun deleteObservation(id: String) {
        dao.deleteObservationById(id)
    }
}

