package com.redops.mobile.di

import android.content.Context
import com.redops.mobile.data.local.database.AppDatabase
import com.redops.mobile.data.local.preferences.UserPreferencesRepository
import com.redops.mobile.data.remote.ApiClient
import com.redops.mobile.data.repository.*
import com.redops.mobile.domain.repository.*
import com.redops.mobile.security.SecurityManager
import com.redops.mobile.utils.PdfReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

interface AppContainer {
    val database: AppDatabase
    val engagementRepository: EngagementRepository
    val findingRepository: FindingRepository
    val notebookRepository: NotebookRepository
    val evidenceRepository: EvidenceRepository
    val dashboardRepository: DashboardRepository
    val activityLogRepository: ActivityLogRepository
    val reportHistoryRepository: ReportHistoryRepository
    val toolObservationRepository: ToolObservationRepository
    val toolkitRepository: ToolkitRepository
    val userPreferencesRepository: UserPreferencesRepository
    val securityManager: SecurityManager
    val pdfReportGenerator: PdfReportGenerator
}

class RedOpsContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val engagementRepository: EngagementRepository by lazy {
        EngagementRepositoryImpl(database.engagementDao(), database.activityLogDao())
    }

    override val findingRepository: FindingRepository by lazy {
        FindingRepositoryImpl(database.findingDao(), database.activityLogDao())
    }

    override val notebookRepository: NotebookRepository by lazy {
        NotebookRepositoryImpl(database.notebookDao(), database.activityLogDao())
    }

    override val evidenceRepository: EvidenceRepository by lazy {
        EvidenceRepositoryImpl(database.evidenceDao(), database.activityLogDao())
    }

    override val dashboardRepository: DashboardRepository by lazy {
        DashboardRepositoryImpl(
            database.engagementDao(),
            database.findingDao(),
            database.notebookDao(),
            database.evidenceDao(),
            database.activityLogDao()
        )
    }

    override val activityLogRepository: ActivityLogRepository by lazy {
        ActivityLogRepositoryImpl(database.activityLogDao())
    }

    override val reportHistoryRepository: ReportHistoryRepository by lazy {
        ReportHistoryRepositoryImpl(database.reportHistoryDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val apiClient: ApiClient by lazy {
        ApiClient(
            baseUrlProvider = {
                try {
                    runBlocking {
                        userPreferencesRepository.userPreferencesFlow.first().apiBaseUrl
                    }
                } catch (_: Exception) {
                    "http://10.0.2.2:5000/api/v1/"
                }
            },
            tokenProvider = {
                try {
                    runBlocking {
                        userPreferencesRepository.userPreferencesFlow.first().authToken
                    }
                } catch (_: Exception) {
                    ""
                }
            }
        )
    }

    override val toolObservationRepository: ToolObservationRepository by lazy {
        ToolObservationRepositoryImpl(database.toolObservationDao())
    }

    override val toolkitRepository: ToolkitRepository by lazy {
        ToolkitRepositoryImpl(apiClient)
    }

    override val securityManager: SecurityManager by lazy {
        SecurityManager(context)
    }

    override val pdfReportGenerator: PdfReportGenerator by lazy {
        PdfReportGenerator(context)
    }
}

