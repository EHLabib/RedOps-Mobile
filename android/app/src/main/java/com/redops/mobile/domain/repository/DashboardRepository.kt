package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.DashboardStats
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardStats(): Flow<DashboardStats>
    fun getRecentActivities(): Flow<List<ActivityLog>>
    fun getRecentEngagements(limit: Int = 3): Flow<List<com.redops.mobile.domain.model.Engagement>>
    suspend fun logActivity(title: String, type: String, statusBadge: String, engagementId: String? = null)
    suspend fun clearActivities()
}

