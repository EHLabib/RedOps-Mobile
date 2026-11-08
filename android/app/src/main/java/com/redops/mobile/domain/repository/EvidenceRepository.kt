package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.EvidenceRecord
import kotlinx.coroutines.flow.Flow

interface EvidenceRepository {
    fun getAllEvidence(): Flow<List<EvidenceRecord>>
    fun getEvidenceForEngagement(engagementId: String): Flow<List<EvidenceRecord>>
    fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>>
    fun getEvidenceById(id: String): Flow<EvidenceRecord?>
    suspend fun getEvidenceByIdDirect(id: String): EvidenceRecord?
    suspend fun saveEvidence(evidence: EvidenceRecord)
    suspend fun updateEvidence(evidence: EvidenceRecord)
    suspend fun deleteEvidence(id: String)
    fun searchEvidence(query: String): Flow<List<EvidenceRecord>>
    suspend fun clearAll()
}
