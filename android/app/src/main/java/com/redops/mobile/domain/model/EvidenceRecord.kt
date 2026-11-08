package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.EvidenceRecordEntity
import java.util.UUID

data class EvidenceRecord(
    val id: String = UUID.randomUUID().toString(),
    val engagementId: String,
    val findingId: String? = null,
    val title: String,
    val description: String = "",
    val evidenceType: String = "Screenshot",
    val category: String = "Screenshot",
    val originalFilename: String = "",
    val storedFilename: String = "",
    val uriOrPath: String = "",
    val fileType: String = "image/png",
    val fileSize: Long = 0L,
    val sha256Checksum: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun EvidenceRecordEntity.toDomain(): EvidenceRecord = EvidenceRecord(
    id = id,
    engagementId = engagementId,
    findingId = findingId,
    title = title,
    description = description,
    evidenceType = evidenceType,
    category = category,
    originalFilename = originalFilename,
    storedFilename = storedFilename,
    uriOrPath = uriOrPath,
    fileType = fileType,
    fileSize = fileSize,
    sha256Checksum = sha256Checksum,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EvidenceRecord.toEntity(): EvidenceRecordEntity = EvidenceRecordEntity(
    id = id,
    engagementId = engagementId,
    findingId = findingId,
    title = title,
    description = description,
    evidenceType = evidenceType,
    category = category,
    originalFilename = originalFilename,
    storedFilename = storedFilename,
    uriOrPath = uriOrPath,
    fileType = fileType,
    fileSize = fileSize,
    sha256Checksum = sha256Checksum,
    createdAt = createdAt,
    updatedAt = updatedAt
)
