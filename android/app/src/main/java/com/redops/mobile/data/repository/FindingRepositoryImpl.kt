package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ActivityLogDao
import com.redops.mobile.data.local.dao.FindingDao
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.FindingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FindingRepositoryImpl(
    private val findingDao: FindingDao,
    private val activityLogDao: ActivityLogDao
) : FindingRepository {

    override fun getAllFindings(): Flow<List<Finding>> {
        return findingDao.getAllFindings().map { list -> list.map { it.toDomain() } }
    }

    override fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>> {
        return findingDao.getFindingsForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding> {
        return findingDao.getFindingsForEngagementDirect(engagementId).map { it.toDomain() }
    }

    override fun getFindingById(id: String): Flow<Finding?> {
        return findingDao.getFindingById(id).map { it?.toDomain() }
    }

    override suspend fun getFindingByIdDirect(id: String): Finding? {
        return findingDao.getFindingByIdDirect(id)?.toDomain()
    }

    override suspend fun saveFinding(finding: Finding) {
        findingDao.insertFinding(finding.toEntity())
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = finding.engagementId,
                title = "[${finding.severity}] Finding Logged: ${finding.title}",
                type = "FINDING",
                statusBadge = finding.severity.uppercase()
            )
        )
    }

    override suspend fun updateFinding(finding: Finding) {
        findingDao.updateFinding(finding.toEntity().copy(updatedAt = System.currentTimeMillis()))
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = finding.engagementId,
                title = "Finding Status Updated: ${finding.title} -> ${finding.status}",
                type = "FINDING",
                statusBadge = finding.status.uppercase()
            )
        )
    }

    override suspend fun deleteFinding(id: String) {
        findingDao.deleteFindingById(id)
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = null,
                title = "Finding Removed",
                type = "FINDING",
                statusBadge = "DELETED"
            )
        )
    }

    override fun searchFindings(query: String): Flow<List<Finding>> {
        return findingDao.searchFindings(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearAll() {
        findingDao.deleteAll()
    }
}

