package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.FindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings ORDER BY createdAt DESC")
    fun getAllFindings(): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE engagementId = :engagementId ORDER BY createdAt DESC")
    fun getFindingsForEngagement(engagementId: String): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE engagementId = :engagementId ORDER BY createdAt DESC")
    suspend fun getFindingsForEngagementDirect(engagementId: String): List<FindingEntity>

    @Query("SELECT * FROM findings WHERE id = :id")
    fun getFindingById(id: String): Flow<FindingEntity?>

    @Query("SELECT * FROM findings WHERE id = :id")
    suspend fun getFindingByIdDirect(id: String): FindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: FindingEntity)

    @Update
    suspend fun updateFinding(finding: FindingEntity)

    @Delete
    suspend fun deleteFinding(finding: FindingEntity)

    @Query("DELETE FROM findings WHERE id = :id")
    suspend fun deleteFindingById(id: String)

    @Query("SELECT COUNT(*) FROM findings")
    fun getTotalFindingsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM findings WHERE severity = :severity")
    fun getFindingsCountBySeverity(severity: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM findings WHERE status = 'Resolved'")
    fun getResolvedFindingsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM findings WHERE status != 'Resolved'")
    fun getUnresolvedFindingsCount(): Flow<Int>

    @Query("SELECT * FROM findings WHERE title LIKE '%' || :query || '%' OR affectedAsset LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchFindings(query: String): Flow<List<FindingEntity>>

    @Query("DELETE FROM findings")
    suspend fun deleteAll()
}

