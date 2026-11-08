package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ActivityLogDao
import com.redops.mobile.data.local.dao.EvidenceDao
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.EvidenceRepository
import com.redops.mobile.utils.EvidenceFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EvidenceRepositoryImpl(
    private val evidenceDao: EvidenceDao,
    private val activityLogDao: ActivityLogDao
) : EvidenceRepository {

    override fun getAllEvidence(): Flow<List<EvidenceRecord>> {
        return evidenceDao.getAllEvidence().map { list -> list.map { it.toDomain() } }
    }

    override fun getEvidenceForEngagement(engagementId: String): Flow<List<EvidenceRecord>> {
        return evidenceDao.getEvidenceForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>> {
        return evidenceDao.getEvidenceForFinding(findingId).map { list -> list.map { it.toDomain() } }
    }

    override fun getEvidenceById(id: String): Flow<EvidenceRecord?> {
        return evidenceDao.getEvidenceById(id).map { it?.toDomain() }
    }

    override suspend fun getEvidenceByIdDirect(id: String): EvidenceRecord? {
        return evidenceDao.getEvidenceByIdDirect(id)?.toDomain()
    }

    override suspend fun saveEvidence(evidence: EvidenceRecord) {
        evidenceDao.insertEvidence(evidence.toEntity())
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = evidence.id,
                engagementId = evidence.engagementId,
                title = "Evidence Imported: ${evidence.title}",
                type = "EVIDENCE",
                statusBadge = evidence.category.uppercase()
            )
        )
    }

    override suspend fun updateEvidence(evidence: EvidenceRecord) {
        evidenceDao.updateEvidence(evidence.toEntity().copy(updatedAt = System.currentTimeMillis()))
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = evidence.id,
                engagementId = evidence.engagementId,
                title = "Evidence Updated: ${evidence.title}",
                type = "EVIDENCE",
                statusBadge = "UPDATED"
            )
        )
    }

    override suspend fun deleteEvidence(id: String) {
        val existing = evidenceDao.getEvidenceByIdDirect(id)
        if (existing != null) {
            EvidenceFileManager.deleteFile(existing.uriOrPath)
        }
        evidenceDao.deleteEvidenceById(id)
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = id,
                engagementId = existing?.engagementId,
                title = "Evidence Deleted",
                type = "EVIDENCE",
                statusBadge = "DELETED"
            )
        )
    }

    override fun searchEvidence(query: String): Flow<List<EvidenceRecord>> {
        return evidenceDao.searchEvidence(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearAll() {
        evidenceDao.deleteAll()
    }
}
