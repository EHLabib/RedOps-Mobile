package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.*
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.DashboardStats
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class DashboardRepositoryImpl(
    private val engagementDao: EngagementDao,
    private val findingDao: FindingDao,
    private val notebookDao: NotebookDao,
    private val evidenceDao: EvidenceDao,
    private val activityLogDao: ActivityLogDao
) : DashboardRepository {

    override fun getDashboardStats(): Flow<DashboardStats> {
        val engagementsFlow = engagementDao.getAllEngagements()
        val findingsFlow = findingDao.getAllFindings()
        val notesCountFlow = notebookDao.getTotalEntriesCount()
        val evidenceCountFlow = evidenceDao.getTotalEvidenceCount()

        return combine(
            engagementsFlow,
            findingsFlow,
            notesCountFlow,
            evidenceCountFlow
        ) { engagements, findings, notesCount, evidenceCount ->
            val totalEngagements = engagements.size
            val activeEngagements = engagements.count { it.status == "Active" }
            val completedEngagements = engagements.count { it.status == "Completed" }

            val totalFindings = findings.size
            val criticalFindings = findings.count { it.severity.equals("Critical", ignoreCase = true) }
            val highFindings = findings.count { it.severity.equals("High", ignoreCase = true) }
            val mediumFindings = findings.count { it.severity.equals("Medium", ignoreCase = true) }
            val lowFindings = findings.count { it.severity.equals("Low", ignoreCase = true) }
            val infoFindings = findings.count { it.severity.equals("Informational", ignoreCase = true) || it.severity.equals("Info", ignoreCase = true) }

            val resolvedFindings = findings.count { it.status.equals("Resolved", ignoreCase = true) }
            val unresolvedFindings = totalFindings - resolvedFindings

            DashboardStats(
                totalEngagements = totalEngagements,
                activeEngagements = activeEngagements,
                completedEngagements = completedEngagements,
                totalFindings = totalFindings,
                criticalFindings = criticalFindings,
                highFindings = highFindings,
                mediumFindings = mediumFindings,
                lowFindings = lowFindings,
                infoFindings = infoFindings,
                resolvedFindings = resolvedFindings,
                unresolvedFindings = unresolvedFindings,
                totalNotes = notesCount,
                totalEvidence = evidenceCount
            )
        }
    }

    override fun getRecentActivities(): Flow<List<ActivityLog>> {
        return activityLogDao.getRecentActivities(limit = 10).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentEngagements(limit: Int): Flow<List<com.redops.mobile.domain.model.Engagement>> {
        return engagementDao.getAllEngagements().map { list ->
            list.take(limit).map { it.toDomain() }
        }
    }

    override suspend fun logActivity(title: String, type: String, statusBadge: String, engagementId: String?) {
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                engagementId = engagementId,
                title = title,
                type = type,
                statusBadge = statusBadge
            )
        )
    }

    override suspend fun clearActivities() {
        activityLogDao.clearActivities()
    }
}

