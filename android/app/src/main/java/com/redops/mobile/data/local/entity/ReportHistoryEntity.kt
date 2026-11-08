package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "report_history",
    foreignKeys = [
        ForeignKey(
            entity = EngagementEntity::class,
            parentColumns = ["id"],
            childColumns = ["engagementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["engagementId"])
    ]
)
data class ReportHistoryEntity(
    @PrimaryKey
    val id: String,
    val engagementId: String,
    val engagementTitle: String,
    val clientName: String,
    val title: String,
    val reportType: String, // FULL, EXECUTIVE, FINDINGS_ONLY
    val format: String, // PDF, TXT
    val classification: String, // Confidential, Strictly Confidential, Internal Use Only, Restricted
    val filePath: String,
    val fileSize: Long,
    val findingsCount: Int,
    val reportVersion: String = "v1.0",
    val generatedAt: Long = System.currentTimeMillis()
)

