package com.redops.mobile.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.DashboardStats
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val stats: DashboardStats,
        val recentEngagements: List<Engagement>,
        val recentActivities: List<ActivityLog>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    val stats: StateFlow<DashboardStats> = dashboardRepository.getDashboardStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    val recentEngagements: StateFlow<List<Engagement>> = dashboardRepository.getRecentEngagements(3)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentActivities: StateFlow<List<ActivityLog>> = dashboardRepository.getRecentActivities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<DashboardUiState> = combine(
        stats,
        recentEngagements,
        recentActivities
    ) { statsVal, engagementsVal, activitiesVal ->
        DashboardUiState.Success(
            stats = statsVal,
            recentEngagements = engagementsVal,
            recentActivities = activitiesVal
        ) as DashboardUiState
    }.catch { e ->
        emit(DashboardUiState.Error(e.message ?: "Failed to load security dashboard data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun logQuickAction(actionName: String) {
        viewModelScope.launch {
            dashboardRepository.logActivity(actionName, "USER_ACTION", "EXECUTED")
        }
    }
}
