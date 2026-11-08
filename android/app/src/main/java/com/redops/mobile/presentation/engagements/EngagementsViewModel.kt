package com.redops.mobile.presentation.engagements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.FindingRepository
import com.redops.mobile.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class EngagementSortOption(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    TITLE_AZ("Title (A-Z)")
}

class EngagementsViewModel(
    private val engagementRepository: EngagementRepository,
    private val findingRepository: FindingRepository,
    private val notebookRepository: NotebookRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter = _statusFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter = _typeFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(EngagementSortOption.NEWEST)
    val sortOption = _sortOption.asStateFlow()

    val engagements: StateFlow<List<Engagement>> = combine(
        engagementRepository.getAllEngagements(),
        _searchQuery,
        _statusFilter,
        _typeFilter,
        _sortOption
    ) { list, query, status, type, sort ->
        var filtered = list.filter { eng ->
            val matchesQuery = query.isBlank() ||
                    eng.title.contains(query, ignoreCase = true) ||
                    eng.client.contains(query, ignoreCase = true) ||
                    eng.authorizedScope.contains(query, ignoreCase = true)

            val matchesStatus = status == "ALL" || eng.status.equals(status, ignoreCase = true)
            val matchesType = type == "ALL" || eng.assessmentType.equals(type, ignoreCase = true)

            matchesQuery && matchesStatus && matchesType
        }

        filtered = when (sort) {
            EngagementSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            EngagementSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
            EngagementSortOption.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setSortOption(sort: EngagementSortOption) {
        _sortOption.value = sort
    }

    fun saveEngagement(engagement: Engagement, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            engagementRepository.saveEngagement(engagement)
            onSuccess()
        }
    }

    fun updateEngagement(engagement: Engagement, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            engagementRepository.updateEngagement(engagement)
            onSuccess()
        }
    }

    fun toggleEngagementStatus(engagement: Engagement) {
        viewModelScope.launch {
            val nextStatus = when (engagement.status) {
                "Planning" -> "Active"
                "Active" -> "Completed"
                "Completed" -> "On Hold"
                "On Hold" -> "Active"
                else -> "Active"
            }
            engagementRepository.updateEngagement(engagement.copy(status = nextStatus))
        }
    }

    fun deleteEngagement(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            engagementRepository.deleteEngagement(id)
            onSuccess()
        }
    }

    fun getFindingsForEngagement(id: String): Flow<List<Finding>> {
        return findingRepository.getFindingsForEngagement(id)
    }

    fun getNotesForEngagement(id: String): Flow<List<NotebookEntry>> {
        return notebookRepository.getEntriesForEngagement(id)
    }

    fun getEngagementById(id: String): Flow<Engagement?> {
        return engagementRepository.getEngagementById(id)
    }
}
