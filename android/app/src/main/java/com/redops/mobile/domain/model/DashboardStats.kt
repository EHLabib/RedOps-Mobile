package com.redops.mobile.domain.model

data class DashboardStats(
    val totalEngagements: Int = 0,
    val activeEngagements: Int = 0,
    val completedEngagements: Int = 0,
    val totalFindings: Int = 0,
    val criticalFindings: Int = 0,
    val highFindings: Int = 0,
    val mediumFindings: Int = 0,
    val lowFindings: Int = 0,
    val infoFindings: Int = 0,
    val resolvedFindings: Int = 0,
    val unresolvedFindings: Int = 0,
    val totalNotes: Int = 0,
    val totalEvidence: Int = 0
) {
    val resolutionProgress: Float
        get() = if (totalFindings > 0) resolvedFindings.toFloat() / totalFindings.toFloat() else 0f
}

