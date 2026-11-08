package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.ReportHistoryEntity
import java.util.UUID

data class ReportHistory(
    val id: String = UUID.randomUUID().toString(),
    val engagementId: String,
    val engagementTitle: String,
    val clientName: String,
    val title: String,
    val reportType: String,
    val format: String,
    val classification: String,
    val filePath: String,
    val fileSize: Long,
    val findingsCount: Int,
    val reportVersion: String = "v1.0",
    val generatedAt: Long = System.currentTimeMillis()
)

fun ReportHistoryEntity.toDomain(): ReportHistory = ReportHistory(
    id = id,
    engagementId = engagementId,
    engagementTitle = engagementTitle,
    clientName = clientName,
    title = title,
    reportType = reportType,
    format = format,
    classification = classification,
    filePath = filePath,
    fileSize = fileSize,
    findingsCount = findingsCount,
    reportVersion = reportVersion,
    generatedAt = generatedAt
)

fun ReportHistory.toEntity(): ReportHistoryEntity = ReportHistoryEntity(
    id = id,
    engagementId = engagementId,
    engagementTitle = engagementTitle,
    clientName = clientName,
    title = title,
    reportType = reportType,
    format = format,
    classification = classification,
    filePath = filePath,
    fileSize = fileSize,
    findingsCount = findingsCount,
    reportVersion = reportVersion,
    generatedAt = generatedAt
)

