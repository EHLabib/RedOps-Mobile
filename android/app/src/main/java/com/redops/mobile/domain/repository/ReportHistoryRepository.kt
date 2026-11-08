package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.ReportHistory
import kotlinx.coroutines.flow.Flow

interface ReportHistoryRepository {
    fun getAllReports(): Flow<List<ReportHistory>>
    fun getReportsForEngagement(engagementId: String): Flow<List<ReportHistory>>
    fun getReportById(id: String): Flow<ReportHistory?>
    suspend fun getReportByIdDirect(id: String): ReportHistory?
    suspend fun recordReport(report: ReportHistory)
    suspend fun deleteReport(id: String)
    suspend fun clearAll()
}

