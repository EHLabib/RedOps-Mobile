package com.redops.mobile.domain.repository

import com.redops.mobile.domain.model.NotebookEntry
import kotlinx.coroutines.flow.Flow

interface NotebookRepository {
    fun getAllEntries(): Flow<List<NotebookEntry>>
    fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntry>>
    fun getEntryById(id: String): Flow<NotebookEntry?>
    suspend fun saveEntry(entry: NotebookEntry)
    suspend fun updateEntry(entry: NotebookEntry)
    suspend fun deleteEntry(id: String)
    fun searchEntries(query: String): Flow<List<NotebookEntry>>
    suspend fun togglePin(id: String, isPinned: Boolean)
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
    suspend fun clearAll()
}

