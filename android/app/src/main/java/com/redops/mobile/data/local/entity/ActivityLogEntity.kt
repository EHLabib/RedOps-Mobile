package com.redops.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey
    val id: String,
    val engagementId: String?,
    val title: String,
    val type: String, // ENGAGEMENT, FINDING, NOTEBOOK, EVIDENCE, REPORT, SECURITY
    val statusBadge: String, // CREATED, UPDATED, DELETED, RESOLVED, PINNED, EXECUTED
    val entityId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
