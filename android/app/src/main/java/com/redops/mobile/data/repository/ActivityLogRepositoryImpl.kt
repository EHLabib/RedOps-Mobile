package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ActivityLogDao
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ActivityLogRepositoryImpl(
    private val activityLogDao: ActivityLogDao
) : ActivityLogRepository {

    override fun getAllActivities(): Flow<List<ActivityLog>> {
        return activityLogDao.getAllActivities().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentActivities(limit: Int): Flow<List<ActivityLog>> {
        return activityLogDao.getRecentActivities(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getActivitiesForEngagement(engagementId: String): Flow<List<ActivityLog>> {
        return activityLogDao.getActivitiesForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun logActivity(
        title: String,
        type: String,
        statusBadge: String,
        entityId: String?,
        engagementId: String?
    ) {
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = engagementId,
                title = title,
                type = type,
                statusBadge = statusBadge,
                entityId = entityId,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearActivities() {
        activityLogDao.clearActivities()
    }
}

