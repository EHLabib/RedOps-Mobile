package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.Engagement
import kotlinx.coroutines.flow.Flow

interface EngagementRepository {
    fun getAllEngagements(): Flow<List<Engagement>>
    fun getEngagementById(id: String): Flow<Engagement?>
    suspend fun getEngagementByIdDirect(id: String): Engagement?
    suspend fun saveEngagement(engagement: Engagement)
    suspend fun updateEngagement(engagement: Engagement)
    suspend fun deleteEngagement(id: String)
    fun searchEngagements(query: String): Flow<List<Engagement>>
    suspend fun clearAll()
}

