package com.redops.mobile.presentation.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NotebookSortOrder {
    NEWEST,
    OLDEST,
    TITLE_AZ
}

private data class NotebookFilters(
    val query: String,
    val category: String,
    val type: String,
    val engagementId: String?,
    val onlyFavorites: Boolean
)

class NotebookViewModel(
    private val notebookRepository: NotebookRepository,
    private val engagementRepository: EngagementRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL")
    val typeFilter = _typeFilter.asStateFlow()

    private val _engagementFilter = MutableStateFlow<String?>(null)
    val engagementFilter = _engagementFilter.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites = _onlyFavorites.asStateFlow()

    private val _sortOrder = MutableStateFlow(NotebookSortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    val engagements: StateFlow<List<Engagement>> = engagementRepository.getAllEngagements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filtersFlow = combine(
        _searchQuery,
        _categoryFilter,
        _typeFilter,
        _engagementFilter,
        _onlyFavorites
    ) { q, cat, type, engId, fav ->
        NotebookFilters(q, cat, type, engId, fav)
    }

    val entries: StateFlow<List<NotebookEntry>> = combine(
        notebookRepository.getAllEntries(),
        filtersFlow,
        _sortOrder
    ) { list, filters, sort ->
        list.filter { entry ->
            val matchesQuery = filters.query.isBlank() ||
                    entry.title.contains(filters.query, ignoreCase = true) ||
                    entry.description.contains(filters.query, ignoreCase = true) ||
                    entry.tags.contains(filters.query, ignoreCase = true)

            val matchesCategory = filters.category == "ALL" || entry.category.equals(filters.category, ignoreCase = true)
            val matchesType = filters.type == "ALL" || entry.entryType.equals(filters.type, ignoreCase = true)
            val matchesEngagement = filters.engagementId == null || entry.engagementId == filters.engagementId
            val matchesFav = !filters.onlyFavorites || entry.isFavorite

            matchesQuery && matchesCategory && matchesType && matchesEngagement && matchesFav
        }.let { filtered ->
            when (sort) {
                NotebookSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
                NotebookSortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
                NotebookSortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedEntries: StateFlow<List<NotebookEntry>> = entries.map { list ->
        list.filter { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpinnedEntries: StateFlow<List<NotebookEntry>> = entries.map { list ->
        list.filter { !it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setEngagementFilter(engagementId: String?) {
        _engagementFilter.value = engagementId
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun setSortOrder(order: NotebookSortOrder) {
        _sortOrder.value = order
    }

    fun togglePin(entry: NotebookEntry) {
        viewModelScope.launch {
            notebookRepository.togglePin(entry.id, !entry.isPinned)
        }
    }

    fun toggleFavorite(entry: NotebookEntry) {
        viewModelScope.launch {
            notebookRepository.toggleFavorite(entry.id, !entry.isFavorite)
        }
    }

    fun saveEntry(entry: NotebookEntry, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            notebookRepository.saveEntry(entry)
            onSuccess()
        }
    }

    fun updateEntry(entry: NotebookEntry, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            notebookRepository.updateEntry(entry)
            onSuccess()
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            notebookRepository.deleteEntry(id)
        }
    }

    fun getEntryById(id: String): Flow<NotebookEntry?> {
        return notebookRepository.getEntryById(id)
    }
}
