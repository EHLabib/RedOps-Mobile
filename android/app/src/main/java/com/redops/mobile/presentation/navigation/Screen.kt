package com.redops.mobile.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Engagements : Screen("engagements")
    object EngagementDetail : Screen("engagement_detail/{engagementId}") {
        fun createRoute(engagementId: String) = "engagement_detail/$engagementId"
    }
    object Findings : Screen("findings")
    object FindingDetail : Screen("finding_detail/{findingId}") {
        fun createRoute(findingId: String) = "finding_detail/$findingId"
    }
    object Notebook : Screen("notebook")
    object NotebookDetail : Screen("notebook_detail/{entryId}") {
        fun createRoute(entryId: String) = "notebook_detail/$entryId"
    }
    object ActivityLog : Screen("activity_log")
    object Evidence : Screen("evidence")
    object EvidenceDetail : Screen("evidence_detail/{evidenceId}") {
        fun createRoute(evidenceId: String) = "evidence_detail/$evidenceId"
    }
    object Reports : Screen("reports")
    object ReportPreview : Screen("report_preview")
    object Toolkit : Screen("toolkit")
    object Observations : Screen("observations")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
    object About : Screen("about")
}

