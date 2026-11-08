package com.redops.mobile.data.local.dao

import androidx.room.*
import com.redops.mobile.data.local.entity.EvidenceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_records ORDER BY createdAt DESC")
    fun getAllEvidence(): Flow<List<EvidenceRecordEntity>>

    @Query("SELECT * FROM evidence_records WHERE engagementId = :engagementId ORDER BY createdAt DESC")
    fun getEvidenceForEngagement(engagementId: String): Flow<List<EvidenceRecordEntity>>

    @Query("SELECT * FROM evidence_records WHERE findingId = :findingId ORDER BY createdAt DESC")
    fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecordEntity>>

    @Query("SELECT * FROM evidence_records WHERE id = :id")
    fun getEvidenceById(id: String): Flow<EvidenceRecordEntity?>

    @Query("SELECT * FROM evidence_records WHERE id = :id")
    suspend fun getEvidenceByIdDirect(id: String): EvidenceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceRecordEntity)

    @Update
    suspend fun updateEvidence(evidence: EvidenceRecordEntity)

    @Delete
    suspend fun deleteEvidence(evidence: EvidenceRecordEntity)

    @Query("DELETE FROM evidence_records WHERE id = :id")
    suspend fun deleteEvidenceById(id: String)

    @Query("SELECT COUNT(*) FROM evidence_records")
    fun getTotalEvidenceCount(): Flow<Int>

    @Query("SELECT * FROM evidence_records WHERE title LIKE '%' || :query || '%' OR originalFilename LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEvidence(query: String): Flow<List<EvidenceRecordEntity>>

    @Query("DELETE FROM evidence_records")
    suspend fun deleteAll()
}
