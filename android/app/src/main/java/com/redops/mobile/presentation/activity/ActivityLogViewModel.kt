package com.redops.mobile.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.repository.ActivityLogRepository
import com.redops.mobile.domain.repository.EngagementRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActivityLogViewModel(
    private val activityLogRepository: ActivityLogRepository,
    private val engagementRepository: EngagementRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter = _typeFilter.asStateFlow()

    private val _engagementFilter = MutableStateFlow<String?>(null)
    val engagementFilter = _engagementFilter.asStateFlow()

    val engagements: StateFlow<List<Engagement>> = engagementRepository.getAllEngagements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<ActivityLog>> = combine(
        activityLogRepository.getAllActivities(),
        _searchQuery,
        _typeFilter,
        _engagementFilter
    ) { logs, query, type, engagementId ->
        logs.filter { log ->
            val matchesQuery = query.isBlank() ||
                    log.title.contains(query, ignoreCase = true) ||
                    log.type.contains(query, ignoreCase = true) ||
                    log.statusBadge.contains(query, ignoreCase = true)

            val matchesType = type == "ALL" || log.type.equals(type, ignoreCase = true)

            val matchesEngagement = engagementId == null || log.engagementId == engagementId

            matchesQuery && matchesType && matchesEngagement
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    fun setEngagementFilter(engagementId: String?) {
        _engagementFilter.value = engagementId
    }

    fun clearActivities() {
        viewModelScope.launch {
            activityLogRepository.clearActivities()
        }
    }
}

