package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence_records",
    foreignKeys = [
        ForeignKey(
            entity = EngagementEntity::class,
            parentColumns = ["id"],
            childColumns = ["engagementId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FindingEntity::class,
            parentColumns = ["id"],
            childColumns = ["findingId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["engagementId"]),
        Index(value = ["findingId"])
    ]
)
data class EvidenceRecordEntity(
    @PrimaryKey
    val id: String,
    val engagementId: String,
    val findingId: String?,
    val title: String,
    val description: String,
    val evidenceType: String = "Screenshot",
    val category: String = "Screenshot", // Screenshot, Document, Log File, Configuration, Proof of Concept Documentation, Other
    val originalFilename: String = "",
    val storedFilename: String = "",
    val uriOrPath: String,
    val fileType: String,
    val fileSize: Long,
    val sha256Checksum: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
