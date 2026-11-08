package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.ActivityLogEntity
import java.util.UUID

data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),
    val engagementId: String? = null,
    val title: String,
    val type: String,
    val statusBadge: String,
    val entityId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

fun ActivityLogEntity.toDomain(): ActivityLog = ActivityLog(
    id = id,
    engagementId = engagementId,
    title = title,
    type = type,
    statusBadge = statusBadge,
    entityId = entityId,
    timestamp = timestamp
)

fun ActivityLog.toEntity(): ActivityLogEntity = ActivityLogEntity(
    id = id,
    engagementId = engagementId,
    title = title,
    type = type,
    statusBadge = statusBadge,
    entityId = entityId,
    timestamp = timestamp
)
