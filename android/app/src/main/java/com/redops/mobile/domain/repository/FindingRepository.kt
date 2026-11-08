package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.Finding
import kotlinx.coroutines.flow.Flow

interface FindingRepository {
    fun getAllFindings(): Flow<List<Finding>>
    fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>>
    suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding>
    fun getFindingById(id: String): Flow<Finding?>
    suspend fun getFindingByIdDirect(id: String): Finding?
    suspend fun saveFinding(finding: Finding)
    suspend fun updateFinding(finding: Finding)
    suspend fun deleteFinding(id: String)
    fun searchFindings(query: String): Flow<List<Finding>>
    suspend fun clearAll()
}

