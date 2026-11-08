package com.redops.mobile.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.redops.mobile.presentation.activity.ActivityLogViewModel
import com.redops.mobile.presentation.dashboard.DashboardViewModel
import com.redops.mobile.presentation.engagements.EngagementsViewModel
import com.redops.mobile.presentation.evidence.EvidenceViewModel
import com.redops.mobile.presentation.findings.FindingsViewModel
import com.redops.mobile.presentation.notebook.NotebookViewModel
import com.redops.mobile.presentation.reports.ReportsViewModel
import com.redops.mobile.presentation.settings.SettingsViewModel
import com.redops.mobile.presentation.toolkit.ToolkitViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(container.dashboardRepository) as T
            }
            modelClass.isAssignableFrom(ActivityLogViewModel::class.java) -> {
                ActivityLogViewModel(
                    container.activityLogRepository,
                    container.engagementRepository
                ) as T
            }
            modelClass.isAssignableFrom(EngagementsViewModel::class.java) -> {
                EngagementsViewModel(
                    container.engagementRepository,
                    container.findingRepository,
                    container.notebookRepository
                ) as T
            }
            modelClass.isAssignableFrom(FindingsViewModel::class.java) -> {
                FindingsViewModel(
                    container.findingRepository,
                    container.engagementRepository,
                    container.evidenceRepository
                ) as T
            }
            modelClass.isAssignableFrom(NotebookViewModel::class.java) -> {
                NotebookViewModel(
                    container.notebookRepository,
                    container.engagementRepository
                ) as T
            }
            modelClass.isAssignableFrom(EvidenceViewModel::class.java) -> {
                EvidenceViewModel(
                    container.evidenceRepository,
                    container.engagementRepository,
                    container.findingRepository
                ) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(
                    container.engagementRepository,
                    container.findingRepository,
                    container.notebookRepository,
                    container.evidenceRepository,
                    container.dashboardRepository,
                    container.pdfReportGenerator,
                    container.reportHistoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(ToolkitViewModel::class.java) -> {
                ToolkitViewModel(
                    container.toolkitRepository,
                    container.toolObservationRepository,
                    container.engagementRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    container.userPreferencesRepository,
                    container.securityManager,
                    container.engagementRepository,
                    container.findingRepository,
                    container.notebookRepository,
                    container.evidenceRepository,
                    container.dashboardRepository,
                    container.reportHistoryRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

