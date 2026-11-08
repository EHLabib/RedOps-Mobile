package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notebook_entries",
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
data class NotebookEntryEntity(
    @PrimaryKey
    val id: String,
    val engagementId: String?,
    val title: String,
    val category: String = "General Notes",
    val entryType: String, // Text Note, Technical Observation, Command Reference, Checklist, Meeting Note, Assessment Summary
    val description: String,
    val tags: String, // Comma-separated tags
    val evidenceReference: String?,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
