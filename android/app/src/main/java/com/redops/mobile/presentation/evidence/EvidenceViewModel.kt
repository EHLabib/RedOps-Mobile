package com.redops.mobile.presentation.evidence

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.EvidenceRepository
import com.redops.mobile.domain.repository.FindingRepository
import com.redops.mobile.utils.EvidenceFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val EVIDENCE_CATEGORIES = listOf(
    "Screenshot",
    "Document",
    "Log File",
    "Configuration",
    "Proof of Concept Documentation",
    "Other"
)

enum class EvidenceSortOrder {
    NEWEST,
    OLDEST,
    TITLE_AZ
}

sealed class FileImportState {
    object Idle : FileImportState()
    object Importing : FileImportState()
    data class Success(val message: String) : FileImportState()
    data class Error(val message: String) : FileImportState()
}

private data class EvidenceFilters(
    val query: String,
    val category: String,
    val engagementId: String?,
    val findingId: String?
)

class EvidenceViewModel(
    private val evidenceRepository: EvidenceRepository,
    private val engagementRepository: EngagementRepository,
    private val findingRepository: FindingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _engagementFilter = MutableStateFlow<String?>(null)
    val engagementFilter = _engagementFilter.asStateFlow()

    private val _findingFilter = MutableStateFlow<String?>(null)
    val findingFilter = _findingFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(EvidenceSortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    private val _importState = MutableStateFlow<FileImportState>(FileImportState.Idle)
    val importState = _importState.asStateFlow()

    val engagements: StateFlow<List<Engagement>> = engagementRepository.getAllEngagements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val findings: StateFlow<List<Finding>> = findingRepository.getAllFindings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filtersFlow = combine(
        _searchQuery,
        _categoryFilter,
        _engagementFilter,
        _findingFilter
    ) { q, cat, eng, fnd ->
        EvidenceFilters(q, cat, eng, fnd)
    }

    val evidenceList: StateFlow<List<EvidenceRecord>> = combine(
        evidenceRepository.getAllEvidence(),
        filtersFlow,
        _sortOrder
    ) { list, filters, sort ->
        list.filter { ev ->
            val matchesQuery = filters.query.isBlank() ||
                    ev.title.contains(filters.query, ignoreCase = true) ||
                    ev.originalFilename.contains(filters.query, ignoreCase = true) ||
                    ev.description.contains(filters.query, ignoreCase = true)

            val matchesCategory = filters.category == "ALL" || ev.category.equals(filters.category, ignoreCase = true)
            val matchesEngagement = filters.engagementId == null || ev.engagementId == filters.engagementId
            val matchesFinding = filters.findingId == null || ev.findingId == filters.findingId

            matchesQuery && matchesCategory && matchesEngagement && matchesFinding
        }.let { filtered ->
            when (sort) {
                EvidenceSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
                EvidenceSortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
                EvidenceSortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setEngagementFilter(engagementId: String?) {
        _engagementFilter.value = engagementId
    }

    fun setFindingFilter(findingId: String?) {
        _findingFilter.value = findingId
    }

    fun setSortOrder(order: EvidenceSortOrder) {
        _sortOrder.value = order
    }

    fun resetImportState() {
        _importState.value = FileImportState.Idle
    }

    fun importAndSaveEvidence(
        context: Context,
        engagementId: String,
        findingId: String?,
        title: String,
        description: String,
        category: String,
        fileUri: Uri?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _importState.value = FileImportState.Importing
            try {
                var originalFilename = ""
                var storedFilename = ""
                var localPath = ""
                var mimeType = "application/octet-stream"
                var fileSize = 0L
                var checksum = ""

                if (fileUri != null) {
                    val imported = withContext(Dispatchers.IO) {
                        EvidenceFileManager.copyUriToPrivateStorage(context, fileUri)
                    }
                    originalFilename = imported.originalFilename
                    storedFilename = imported.storedFilename
                    localPath = imported.localPath
                    mimeType = imported.mimeType
                    fileSize = imported.fileSize
                    checksum = imported.sha256Checksum
                } else {
                    originalFilename = "reference_entry.txt"
                    storedFilename = "ref_${System.currentTimeMillis()}.txt"
                    mimeType = "text/plain"
                }

                val record = EvidenceRecord(
                    engagementId = engagementId,
                    findingId = findingId,
                    title = title,
                    description = description,
                    evidenceType = category,
                    category = category,
                    originalFilename = originalFilename,
                    storedFilename = storedFilename,
                    uriOrPath = localPath,
                    fileType = mimeType,
                    fileSize = fileSize,
                    sha256Checksum = checksum,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                evidenceRepository.saveEvidence(record)
                _importState.value = FileImportState.Success("Evidence attached successfully.")
                onSuccess()
            } catch (e: Exception) {
                _importState.value = FileImportState.Error(e.message ?: "Failed to import evidence file.")
            }
        }
    }

    fun updateEvidenceMetadata(
        evidence: EvidenceRecord,
        newTitle: String,
        newDescription: String,
        newCategory: String,
        newFindingId: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = evidence.copy(
                title = newTitle.trim(),
                description = newDescription.trim(),
                category = newCategory,
                evidenceType = newCategory,
                findingId = newFindingId,
                updatedAt = System.currentTimeMillis()
            )
            evidenceRepository.updateEvidence(updated)
            onSuccess()
        }
    }

    fun deleteEvidence(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            evidenceRepository.deleteEvidence(id)
            onSuccess()
        }
    }

    fun getEvidenceById(id: String): Flow<EvidenceRecord?> {
        return evidenceRepository.getEvidenceById(id)
    }

    fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>> {
        return evidenceRepository.getEvidenceForFinding(findingId)
    }
}
