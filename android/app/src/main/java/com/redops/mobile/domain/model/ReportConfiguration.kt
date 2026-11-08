package com.redops.mobile.domain.model

enum class ReportType(val displayName: String) {
    FULL("Full Assessment Report"),
    EXECUTIVE("Executive Summary Report"),
    FINDINGS_ONLY("Findings-Only Technical Report")
}

enum class ReportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF("PDF Document", "pdf", "application/pdf"),
    TXT("Plain Text (UTF-8)", "txt", "text/plain")
}

enum class ConfidentialityLevel(val displayName: String) {
    CONFIDENTIAL("Confidential"),
    STRICTLY_CONFIDENTIAL("Strictly Confidential"),
    INTERNAL_ONLY("Internal Use Only"),
    RESTRICTED("Restricted Distribution")
}

data class ReportConfiguration(
    val engagementId: String = "",
    val reportType: ReportType = ReportType.FULL,
    val format: ReportFormat = ReportFormat.PDF,
    val customTitle: String = "",
    val classification: ConfidentialityLevel = ConfidentialityLevel.CONFIDENTIAL,
    val version: String = "v1.0",
    val includeExecutiveSummary: Boolean = true,
    val includeScope: Boolean = true,
    val includeMethodology: Boolean = true,
    val includeFindingsSummary: Boolean = true,
    val includeDetailedFindings: Boolean = true,
    val includeRemediation: Boolean = true,
    val includeEvidenceInventory: Boolean = true,
    val includeNotebookNotes: Boolean = false,
    val customMethodologyText: String = ""
)

