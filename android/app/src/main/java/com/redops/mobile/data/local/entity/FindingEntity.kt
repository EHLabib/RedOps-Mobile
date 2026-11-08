package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = EngagementEntity::class,
            parentColumns = ["id"],
            childColumns = ["engagementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["engagementId"])]
)
data class FindingEntity(
    @PrimaryKey
    val id: String,
    val customId: String, // e.g. F-001
    val engagementId: String,
    val title: String,
    val category: String, // Authentication, Authorization, Input Validation, Injection, Configuration, Information Disclosure, Cryptography, Session Management, Business Logic, Other
    val severity: String, // Critical, High, Medium, Low, Informational
    val cvssScore: Double?,
    val affectedAsset: String,
    val cweReference: String?,
    val description: String,
    val technicalImpact: String,
    val evidenceReference: String?,
    val pocReference: String?,
    val remediation: String,
    val status: String, // Open, In Progress, Resolved, Accepted Risk, False Positive
    val discoveryDate: String = "",
    val generalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
