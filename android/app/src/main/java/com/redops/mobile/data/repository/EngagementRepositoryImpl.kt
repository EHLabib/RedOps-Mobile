package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ActivityLogDao
import com.redops.mobile.data.local.dao.EngagementDao
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.EngagementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EngagementRepositoryImpl(
    private val engagementDao: EngagementDao,
    private val activityLogDao: ActivityLogDao
) : EngagementRepository {

    override fun getAllEngagements(): Flow<List<Engagement>> {
        return engagementDao.getAllEngagements().map { list -> list.map { it.toDomain() } }
    }

    override fun getEngagementById(id: String): Flow<Engagement?> {
        return engagementDao.getEngagementById(id).map { it?.toDomain() }
    }

    override suspend fun getEngagementByIdDirect(id: String): Engagement? {
        return engagementDao.getEngagementByIdDirect(id)?.toDomain()
    }

    override suspend fun saveEngagement(engagement: Engagement) {
        engagementDao.insertEngagement(engagement.toEntity())
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = engagement.id,
                title = "New Engagement Created: ${engagement.title}",
                type = "ENGAGEMENT",
                statusBadge = if (engagement.isAuthorized) "AUTHORIZED" else "PENDING AUTH"
            )
        )
    }

    override suspend fun updateEngagement(engagement: Engagement) {
        engagementDao.updateEngagement(engagement.toEntity().copy(updatedAt = System.currentTimeMillis()))
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = engagement.id,
                title = "Engagement Updated: ${engagement.title} (${engagement.status})",
                type = "ENGAGEMENT",
                statusBadge = engagement.status.uppercase()
            )
        )
    }

    override suspend fun deleteEngagement(id: String) {
        engagementDao.deleteEngagementById(id)
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = id,
                title = "Engagement Deleted",
                type = "ENGAGEMENT",
                statusBadge = "REMOVED"
            )
        )
    }

    override fun searchEngagements(query: String): Flow<List<Engagement>> {
        return engagementDao.searchEngagements(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearAll() {
        engagementDao.deleteAll()
    }
}

