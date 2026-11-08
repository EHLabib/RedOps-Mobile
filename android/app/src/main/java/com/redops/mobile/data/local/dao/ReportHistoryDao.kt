package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.ReportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportHistoryDao {
    @Query("SELECT * FROM report_history ORDER BY generatedAt DESC")
    fun getAllReports(): Flow<List<ReportHistoryEntity>>

    @Query("SELECT * FROM report_history WHERE engagementId = :engagementId ORDER BY generatedAt DESC")
    fun getReportsForEngagement(engagementId: String): Flow<List<ReportHistoryEntity>>

    @Query("SELECT * FROM report_history WHERE id = :id")
    fun getReportById(id: String): Flow<ReportHistoryEntity?>

    @Query("SELECT * FROM report_history WHERE id = :id")
    suspend fun getReportByIdDirect(id: String): ReportHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportHistoryEntity)

    @Query("DELETE FROM report_history WHERE id = :id")
    suspend fun deleteReportById(id: String)

    @Query("DELETE FROM report_history")
    suspend fun clearAll()
}

