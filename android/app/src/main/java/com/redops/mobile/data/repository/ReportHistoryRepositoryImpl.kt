package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ReportHistoryDao
import com.redops.mobile.domain.model.ReportHistory
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.ReportHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ReportHistoryRepositoryImpl(
    private val reportHistoryDao: ReportHistoryDao
) : ReportHistoryRepository {

    override fun getAllReports(): Flow<List<ReportHistory>> {
        return reportHistoryDao.getAllReports().map { list -> list.map { it.toDomain() } }
    }

    override fun getReportsForEngagement(engagementId: String): Flow<List<ReportHistory>> {
        return reportHistoryDao.getReportsForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override fun getReportById(id: String): Flow<ReportHistory?> {
        return reportHistoryDao.getReportById(id).map { it?.toDomain() }
    }

    override suspend fun getReportByIdDirect(id: String): ReportHistory? {
        return reportHistoryDao.getReportByIdDirect(id)?.toDomain()
    }

    override suspend fun recordReport(report: ReportHistory) {
        reportHistoryDao.insertReport(report.toEntity())
    }

    override suspend fun deleteReport(id: String) {
        val direct = reportHistoryDao.getReportByIdDirect(id)
        if (direct != null && direct.filePath.isNotBlank()) {
            val file = File(direct.filePath)
            if (file.exists()) file.delete()
        }
        reportHistoryDao.deleteReportById(id)
    }

    override suspend fun clearAll() {
        reportHistoryDao.clearAll()
    }
}

