package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.NotebookEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebook_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<NotebookEntryEntity>>

    @Query("SELECT * FROM notebook_entries WHERE engagementId = :engagementId ORDER BY createdAt DESC")
    fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntryEntity>>

    @Query("SELECT * FROM notebook_entries WHERE id = :id")
    fun getEntryById(id: String): Flow<NotebookEntryEntity?>

    @Query("SELECT * FROM notebook_entries WHERE isPinned = 1 ORDER BY createdAt DESC")
    fun getPinnedEntries(): Flow<List<NotebookEntryEntity>>

    @Query("UPDATE notebook_entries SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean)

    @Query("UPDATE notebook_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NotebookEntryEntity)

    @Update
    suspend fun updateEntry(entry: NotebookEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: NotebookEntryEntity)

    @Query("DELETE FROM notebook_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("SELECT COUNT(*) FROM notebook_entries")
    fun getTotalEntriesCount(): Flow<Int>

    @Query("SELECT * FROM notebook_entries WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<NotebookEntryEntity>>

    @Query("DELETE FROM notebook_entries")
    suspend fun deleteAll()
}

