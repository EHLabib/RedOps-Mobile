package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.FindingEntity
import java.util.UUID

data class Finding(
    val id: String = UUID.randomUUID().toString(),
    val customId: String = "F-001",
    val engagementId: String,
    val title: String,
    val category: String = "Injection",
    val severity: String = "Medium", // Critical, High, Medium, Low, Informational
    val cvssScore: Double? = null,
    val affectedAsset: String,
    val cweReference: String? = null,
    val description: String,
    val technicalImpact: String = "",
    val evidenceReference: String? = null,
    val pocReference: String? = null,
    val remediation: String,
    val status: String = "Open", // Open, In Progress, Resolved, Accepted Risk, False Positive
    val discoveryDate: String = "",
    val generalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun FindingEntity.toDomain(): Finding = Finding(
    id = id,
    customId = customId,
    engagementId = engagementId,
    title = title,
    category = category,
    severity = severity,
    cvssScore = cvssScore,
    affectedAsset = affectedAsset,
    cweReference = cweReference,
    description = description,
    technicalImpact = technicalImpact,
    evidenceReference = evidenceReference,
    pocReference = pocReference,
    remediation = remediation,
    status = status,
    discoveryDate = discoveryDate,
    generalNotes = generalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Finding.toEntity(): FindingEntity = FindingEntity(
    id = id,
    customId = customId,
    engagementId = engagementId,
    title = title,
    category = category,
    severity = severity,
    cvssScore = cvssScore,
    affectedAsset = affectedAsset,
    cweReference = cweReference,
    description = description,
    technicalImpact = technicalImpact,
    evidenceReference = evidenceReference,
    pocReference = pocReference,
    remediation = remediation,
    status = status,
    discoveryDate = discoveryDate,
    generalNotes = generalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
