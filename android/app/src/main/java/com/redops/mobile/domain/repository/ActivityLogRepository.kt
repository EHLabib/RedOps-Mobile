package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityLogRepository {
    fun getAllActivities(): Flow<List<ActivityLog>>
    fun getRecentActivities(limit: Int = 20): Flow<List<ActivityLog>>
    fun getActivitiesForEngagement(engagementId: String): Flow<List<ActivityLog>>
    suspend fun logActivity(
        title: String,
        type: String,
        statusBadge: String,
        entityId: String? = null,
        engagementId: String? = null
    )
    suspend fun clearActivities()
}

