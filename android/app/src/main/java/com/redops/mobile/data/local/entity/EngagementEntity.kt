package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engagements")
data class EngagementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val client: String,
    val description: String,
    val assessmentType: String, // Web Application, Network, Mobile Application, Internal, External, Other
    val startDate: String,
    val endDate: String,
    val status: String, // Planning, Active, On Hold, Completed, Cancelled
    val authorizedScope: String,
    val outOfScope: String,
    val assessmentObjectives: String,
    val isAuthorized: Boolean,
    val authorizationTimestamp: Long? = null,
    val additionalNotes: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
