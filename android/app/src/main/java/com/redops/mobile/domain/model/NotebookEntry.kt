package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.NotebookEntryEntity
import java.util.UUID

data class NotebookEntry(
    val id: String = UUID.randomUUID().toString(),
    val engagementId: String? = null,
    val title: String,
    val category: String = "General Notes",
    val entryType: String = "Technical Observation", // Text Note, Technical Observation, Command Reference, Checklist, Meeting Note, Assessment Summary
    val description: String,
    val tags: String = "",
    val evidenceReference: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun NotebookEntryEntity.toDomain(): NotebookEntry = NotebookEntry(
    id = id,
    engagementId = engagementId,
    title = title,
    category = category,
    entryType = entryType,
    description = description,
    tags = tags,
    evidenceReference = evidenceReference,
    isPinned = isPinned,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun NotebookEntry.toEntity(): NotebookEntryEntity = NotebookEntryEntity(
    id = id,
    engagementId = engagementId,
    title = title,
    category = category,
    entryType = entryType,
    description = description,
    tags = tags,
    evidenceReference = evidenceReference,
    isPinned = isPinned,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)
