package com.redops.mobile.presentation.findings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.EvidenceRepository
import com.redops.mobile.domain.repository.FindingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FindingSortOption(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    SEVERITY_HIGH_TO_LOW("Severity (High → Low)"),
    SEVERITY_LOW_TO_HIGH("Severity (Low → High)"),
    TITLE_AZ("Title (A-Z)")
}

class FindingsViewModel(
    private val findingRepository: FindingRepository,
    private val engagementRepository: EngagementRepository,
    private val evidenceRepository: EvidenceRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _severityFilter = MutableStateFlow("ALL")
    val severityFilter = _severityFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _engagementFilter = MutableStateFlow("ALL")
    val engagementFilter = _engagementFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(FindingSortOption.NEWEST)
    val sortOption = _sortOption.asStateFlow()

    val engagements: StateFlow<List<Engagement>> = engagementRepository.getAllEngagements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val findings: StateFlow<List<Finding>> = combine(
        findingRepository.getAllFindings(),
        _searchQuery,
        _severityFilter,
        _statusFilter,
        _categoryFilter
    ) { list, query, sev, st, cat ->
        FilterParams(list, query, sev, st, cat)
    }.combine(_engagementFilter) { params, engFilter ->
        Pair(params, engFilter)
    }.combine(_sortOption) { (params, engFilter), sort ->
        var filtered = params.list.filter { f ->
            val matchesQuery = params.query.isBlank() ||
                    f.title.contains(params.query, ignoreCase = true) ||
                    f.affectedAsset.contains(params.query, ignoreCase = true) ||
                    f.customId.contains(params.query, ignoreCase = true) ||
                    f.category.contains(params.query, ignoreCase = true) ||
                    (!f.cweReference.isNullOrBlank() && f.cweReference.contains(params.query, ignoreCase = true))

            val matchesSeverity = params.sev == "ALL" || f.severity.equals(params.sev, ignoreCase = true)
            val matchesStatus = params.st == "ALL" || f.status.equals(params.st, ignoreCase = true)
            val matchesCategory = params.cat == "ALL" || f.category.equals(params.cat, ignoreCase = true)
            val matchesEngagement = engFilter == "ALL" || f.engagementId == engFilter

            matchesQuery && matchesSeverity && matchesStatus && matchesCategory && matchesEngagement
        }

        filtered = when (sort) {
            FindingSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            FindingSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
            FindingSortOption.SEVERITY_HIGH_TO_LOW -> filtered.sortedByDescending { getSeverityRank(it.severity) }
            FindingSortOption.SEVERITY_LOW_TO_HIGH -> filtered.sortedBy { getSeverityRank(it.severity) }
            FindingSortOption.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterParams(
        val list: List<Finding>,
        val query: String,
        val sev: String,
        val st: String,
        val cat: String
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSeverityFilter(filter: String) {
        _severityFilter.value = filter
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setCategoryFilter(filter: String) {
        _categoryFilter.value = filter
    }

    fun setEngagementFilter(engagementId: String) {
        _engagementFilter.value = engagementId
    }

    fun setSortOption(sort: FindingSortOption) {
        _sortOption.value = sort
    }

    fun saveFinding(finding: Finding, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            findingRepository.saveFinding(finding)
            onSuccess()
        }
    }

    fun updateFinding(finding: Finding, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            findingRepository.updateFinding(finding)
            onSuccess()
        }
    }

    fun updateFindingStatus(finding: Finding, newStatus: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            findingRepository.updateFinding(finding.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
            onSuccess()
        }
    }

    fun toggleFindingResolved(finding: Finding) {
        viewModelScope.launch {
            val newStatus = if (finding.status == "Resolved") "Open" else "Resolved"
            findingRepository.updateFinding(finding.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteFinding(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            findingRepository.deleteFinding(id)
            onSuccess()
        }
    }

    fun getFindingById(id: String): Flow<Finding?> {
        return findingRepository.getFindingById(id)
    }

    fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>> {
        return evidenceRepository?.getEvidenceForFinding(findingId) ?: flowOf(emptyList())
    }

    fun getEngagementById(id: String): Flow<Engagement?> {
        return engagementRepository.getEngagementById(id)
    }

    companion object {
        fun getSeverityRank(severity: String): Int = when (severity.lowercase()) {
            "critical" -> 5
            "high" -> 4
            "medium" -> 3
            "low" -> 2
            else -> 1
        }
    }
}
