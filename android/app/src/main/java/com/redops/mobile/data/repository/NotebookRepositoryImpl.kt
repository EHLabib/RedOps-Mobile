package com.redops.mobile.data.repository

import com.redops.mobile.data.local.dao.ActivityLogDao
import com.redops.mobile.data.local.dao.NotebookDao
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.model.toDomain
import com.redops.mobile.domain.model.toEntity
import com.redops.mobile.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val activityLogDao: ActivityLogDao
) : NotebookRepository {

    override fun getAllEntries(): Flow<List<NotebookEntry>> {
        return notebookDao.getAllEntries().map { list -> list.map { it.toDomain() } }
    }

    override fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntry>> {
        return notebookDao.getEntriesForEngagement(engagementId).map { list -> list.map { it.toDomain() } }
    }

    override fun getEntryById(id: String): Flow<NotebookEntry?> {
        return notebookDao.getEntryById(id).map { it?.toDomain() }
    }

    override suspend fun saveEntry(entry: NotebookEntry) {
        notebookDao.insertEntry(entry.toEntity())
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = entry.id,
                engagementId = entry.engagementId,
                title = "Note Recorded: ${entry.title}",
                type = "NOTEBOOK",
                statusBadge = entry.entryType.uppercase()
            )
        )
    }

    override suspend fun updateEntry(entry: NotebookEntry) {
        notebookDao.updateEntry(entry.toEntity().copy(updatedAt = System.currentTimeMillis()))
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = entry.id,
                engagementId = entry.engagementId,
                title = "Note Updated: ${entry.title}",
                type = "NOTEBOOK",
                statusBadge = "UPDATED"
            )
        )
    }

    override suspend fun deleteEntry(id: String) {
        notebookDao.deleteEntryById(id)
        activityLogDao.insertActivity(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                entityId = id,
                engagementId = null,
                title = "Note Deleted",
                type = "NOTEBOOK",
                statusBadge = "DELETED"
            )
        )
    }

    override suspend fun togglePin(id: String, isPinned: Boolean) {
        notebookDao.updatePinStatus(id, isPinned)
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        notebookDao.updateFavoriteStatus(id, isFavorite)
    }

    override fun searchEntries(query: String): Flow<List<NotebookEntry>> {
        return notebookDao.searchEntries(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearAll() {
        notebookDao.deleteAll()
    }
}

