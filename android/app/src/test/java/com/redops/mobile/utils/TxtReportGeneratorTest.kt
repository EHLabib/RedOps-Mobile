package com.redops.mobile.utils

import com.redops.mobile.domain.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtReportGeneratorTest {

    private val sampleEngagement = Engagement(
        id = "eng-test",
        title = "Corporate Portal Security Assessment",
        client = "FinSecure Ltd",
        assessmentType = "Web Application",
        startDate = "2026-11-01",
        endDate = "2026-11-05",
        status = "Completed",
        authorizedScope = "https://portal.finsecure.com",
        outOfScope = "Third-party payment gateways",
        assessmentObjectives = "Assess authentication, authorization, and data validation controls",
        isAuthorized = true,
        description = "Authorized third-party security verification.",
        additionalNotes = "Coordinated with lead security engineer"
    )

    private val sampleFindings = listOf(
        Finding(
            id = "f-1",
            customId = "FIN-001",
            engagementId = "eng-test",
            title = "Authentication Bypass in Reset Workflow",
            category = "Broken Access Control",
            severity = "Critical",
            cvssScore = 9.8,
            affectedAsset = "/api/v1/auth/reset-password",
            cweReference = "CWE-287",
            description = "Password reset tokens can be predicted due to weak PRNG.",
            technicalImpact = "Account takeover of administrative accounts.",
            evidenceReference = "poc_token_collision.py",
            pocReference = "Run script with 10 concurrent requests.",
            remediation = "Use cryptographically secure PRNG and invalidate tokens upon single use.",
            status = "Open",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        ),
        Finding(
            id = "f-2",
            customId = "FIN-002",
            engagementId = "eng-test",
            title = "Missing Content Security Policy",
            category = "Configuration",
            severity = "Low",
            cvssScore = 3.5,
            affectedAsset = "https://portal.finsecure.com/",
            cweReference = "CWE-16",
            description = "HTTP header Content-Security-Policy is not enforced.",
            technicalImpact = "Increased risk of client-side script execution in case of XSS.",
            evidenceReference = "",
            pocReference = "Inspect HTTP response headers.",
            remediation = "Implement strict Content-Security-Policy header.",
            status = "Open",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
    )

    private val sampleNotes = listOf(
        NotebookEntry(
            id = "note-1",
            engagementId = "eng-test",
            title = "Firewall Behavior Observed",
            entryType = "Technical Observation",
            description = "WAF rate-limits at 100 requests per minute per IP.",
            tags = "waf,network",
            evidenceReference = "",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
    )

    private val sampleEvidence = listOf(
        EvidenceRecord(
            id = "ev-1",
            engagementId = "eng-test",
            findingId = "f-1",
            title = "Token Entropy Analysis",
            description = "Entropy dump of reset tokens",
            category = "Proof of Concept Documentation",
            originalFilename = "token_dump.csv",
            storedFilename = "ev_123_token_dump.csv",
            uriOrPath = "/evidence/ev_123_token_dump.csv",
            fileType = "text/csv",
            fileSize = 4096L,
            sha256Checksum = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
    )

    @Test
    fun generateReport_containsRequiredSectionsByDefault() {
        val config = ReportConfiguration(
            engagementId = sampleEngagement.id,
            includeNotebookNotes = true
        )
        val reportText = TxtReportGenerator.generateReport(sampleEngagement, sampleFindings, sampleNotes, sampleEvidence, config)

        assertTrue(reportText.contains("SECURITY ASSESSMENT REPORT"))
        assertTrue(reportText.contains("FinSecure Ltd"))
        assertTrue(reportText.contains("EXECUTIVE SUMMARY"))
        assertTrue(reportText.contains("VULNERABILITY FINDINGS METRICS"))
        assertTrue(reportText.contains("DETAILED VULNERABILITY OBSERVATIONS"))
        assertTrue(reportText.contains("Authentication Bypass in Reset Workflow"))
        assertTrue(reportText.contains("CWE-287"))
        assertTrue(reportText.contains("Documented Notebook Entries"))
        assertTrue(reportText.contains("Firewall Behavior Observed"))
        assertTrue(reportText.contains("Evidence Artifacts Inventory"))
        assertTrue(reportText.contains("SHA-256: 9f86d081884c7d65"))
        assertTrue(reportText.contains("CONCLUSION"))
    }

    @Test
    fun generateReport_respectsCustomClassificationAndTitle() {
        val config = ReportConfiguration(
            engagementId = sampleEngagement.id,
            customTitle = "CUSTOM AUDIT REPORT TITLE",
            classification = ConfidentialityLevel.STRICTLY_CONFIDENTIAL,
            version = "v2.5"
        )

        val reportText = TxtReportGenerator.generateReport(sampleEngagement, sampleFindings, sampleNotes, sampleEvidence, config)

        assertTrue(reportText.contains("CUSTOM AUDIT REPORT TITLE"))
        assertTrue(reportText.contains("STRICTLY CONFIDENTIAL"))
        assertTrue(reportText.contains("v2.5"))
    }

    @Test
    fun generateReport_respectsSectionToggles() {
        val config = ReportConfiguration(
            engagementId = sampleEngagement.id,
            includeExecutiveSummary = false,
            includeNotebookNotes = false,
            includeEvidenceInventory = false
        )

        val reportText = TxtReportGenerator.generateReport(sampleEngagement, sampleFindings, sampleNotes, sampleEvidence, config)

        assertFalse(reportText.contains("1. EXECUTIVE SUMMARY"))
        assertFalse(reportText.contains("Documented Notebook Entries"))
        assertFalse(reportText.contains("Evidence Artifacts Inventory"))
        // Detailed observations should still be present
        assertTrue(reportText.contains("DETAILED VULNERABILITY OBSERVATIONS"))
    }

    @Test
    fun generateReport_executiveSummaryType_omitsDetailedFindings() {
        val config = ReportConfiguration(
            engagementId = sampleEngagement.id,
            reportType = ReportType.EXECUTIVE
        )

        val reportText = TxtReportGenerator.generateReport(sampleEngagement, sampleFindings, sampleNotes, sampleEvidence, config)

        assertTrue(reportText.contains("EXECUTIVE SUMMARY"))
        assertFalse(reportText.contains("DETAILED VULNERABILITY OBSERVATIONS"))
        assertFalse(reportText.contains("Documented Notebook Entries"))
    }

    @Test
    fun generateReport_findingsOnlyType_focusesOnCatalog() {
        val config = ReportConfiguration(
            engagementId = sampleEngagement.id,
            reportType = ReportType.FINDINGS_ONLY
        )

        val reportText = TxtReportGenerator.generateReport(sampleEngagement, sampleFindings, sampleNotes, sampleEvidence, config)

        assertFalse(reportText.contains("1. EXECUTIVE SUMMARY"))
        assertTrue(reportText.contains("VULNERABILITY FINDINGS METRICS"))
        assertTrue(reportText.contains("DETAILED VULNERABILITY OBSERVATIONS"))
    }
}
