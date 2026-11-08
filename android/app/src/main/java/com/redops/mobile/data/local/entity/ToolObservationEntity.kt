package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tool_observations",
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
data class ToolObservationEntity(
    @PrimaryKey
    val id: String,
    val engagementId: String,
    val toolName: String,
    val toolCategory: String, // Reconnaissance, Web Security, Security Utilities
    val target: String,
    val title: String,
    val summary: String,
    val sourceInfo: String,
    val rawResultJson: String,
    val status: String, // Unverified, Verified, Converted, Dismissed
    val convertedFindingId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

