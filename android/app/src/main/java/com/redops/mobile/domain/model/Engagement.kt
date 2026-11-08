package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.EngagementEntity
import java.util.UUID

data class Engagement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val client: String,
    val description: String = "",
    val assessmentType: String = "Web Application",
    val startDate: String,
    val endDate: String,
    val status: String = "Planning", // Planning, Active, On Hold, Completed, Cancelled
    val authorizedScope: String,
    val outOfScope: String = "",
    val assessmentObjectives: String = "",
    val isAuthorized: Boolean = false,
    val authorizationTimestamp: Long? = null,
    val additionalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun EngagementEntity.toDomain(): Engagement = Engagement(
    id = id,
    title = title,
    client = client,
    description = description,
    assessmentType = assessmentType,
    startDate = startDate,
    endDate = endDate,
    status = status,
    authorizedScope = authorizedScope,
    outOfScope = outOfScope,
    assessmentObjectives = assessmentObjectives,
    isAuthorized = isAuthorized,
    authorizationTimestamp = authorizationTimestamp,
    additionalNotes = additionalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Engagement.toEntity(): EngagementEntity = EngagementEntity(
    id = id,
    title = title,
    client = client,
    description = description,
    assessmentType = assessmentType,
    startDate = startDate,
    endDate = endDate,
    status = status,
    authorizedScope = authorizedScope,
    outOfScope = outOfScope,
    assessmentObjectives = assessmentObjectives,
    isAuthorized = isAuthorized,
    authorizationTimestamp = authorizationTimestamp,
    additionalNotes = additionalNotes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
