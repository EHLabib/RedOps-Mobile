package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.ToolObservationEntity
import java.util.UUID

data class ToolObservation(
    val id: String = UUID.randomUUID().toString(),
    val engagementId: String,
    val toolName: String,
    val toolCategory: String,
    val target: String,
    val title: String,
    val summary: String,
    val sourceInfo: String = "Automated Toolkit Scan",
    val rawResultJson: String = "",
    val status: String = "Unverified", // Unverified, Verified, Converted, Dismissed
    val convertedFindingId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ToolObservationEntity.toDomain(): ToolObservation = ToolObservation(
    id = id,
    engagementId = engagementId,
    toolName = toolName,
    toolCategory = toolCategory,
    target = target,
    title = title,
    summary = summary,
    sourceInfo = sourceInfo,
    rawResultJson = rawResultJson,
    status = status,
    convertedFindingId = convertedFindingId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ToolObservation.toEntity(): ToolObservationEntity = ToolObservationEntity(
    id = id,
    engagementId = engagementId,
    toolName = toolName,
    toolCategory = toolCategory,
    target = target,
    title = title,
    summary = summary,
    sourceInfo = sourceInfo,
    rawResultJson = rawResultJson,
    status = status,
    convertedFindingId = convertedFindingId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

