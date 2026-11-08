package com.redops.mobile.utils

import com.redops.mobile.domain.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TxtReportGenerator {

    fun generateReport(
        engagement: Engagement,
        findings: List<Finding>,
        notes: List<NotebookEntry> = emptyList(),
        evidence: List<EvidenceRecord> = emptyList(),
        config: ReportConfiguration = ReportConfiguration()
    ): String = buildString {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val reportTitle = config.customTitle.ifBlank { "SECURITY ASSESSMENT REPORT: ${engagement.title.uppercase()}" }

        // A. COVER / HEADER
        appendLine("================================================================================")
        appendLine("           REDOPS MOBILE — OFFENSE-INFORMED ASSESSMENT REPORT")
        appendLine("================================================================================")
        appendLine("Report Title:      $reportTitle")
        appendLine("Client / Target:   ${engagement.client}")
        appendLine("Engagement:        ${engagement.title}")
        appendLine("Assessment Type:   ${engagement.assessmentType}")
        appendLine("Timeline:          ${engagement.startDate} to ${engagement.endDate}")
        appendLine("Classification:    ${config.classification.displayName.uppercase()}")
        appendLine("Report Version:    ${config.version}")
        appendLine("Generated Date:    $dateStr")
        appendLine("Authorization:     ${if (engagement.isAuthorized) "CONFIRMED & DECLARED" else "UNCONFIRMED"}")
        appendLine("================================================================================")
        appendLine()

        val criticalCount = findings.count { it.severity.equals("Critical", true) }
        val highCount = findings.count { it.severity.equals("High", true) }
        val mediumCount = findings.count { it.severity.equals("Medium", true) }
        val lowCount = findings.count { it.severity.equals("Low", true) }
        val infoCount = findings.count { it.severity.equals("Informational", true) || it.severity.equals("Info", true) }
        val resolvedCount = findings.count { it.status.equals("Resolved", true) }
        val unresolvedCount = findings.size - resolvedCount

        // B. EXECUTIVE SUMMARY
        if (config.includeExecutiveSummary && config.reportType != ReportType.FINDINGS_ONLY) {
            appendLine("1. EXECUTIVE SUMMARY")
            appendLine("--------------------------------------------------------------------------------")
            appendLine(engagement.description.ifBlank {
                "An authorized security assessment was conducted for ${engagement.client} targeting in-scope assets."
            })
            appendLine()
            appendLine("Assessment Objectives:")
            appendLine(engagement.assessmentObjectives.ifBlank {
                "Identify, validate, and document observable security exposures and configuration weaknesses."
            })
            appendLine()
            appendLine("Overall Posture & Findings Summary:")
            appendLine("A total of ${findings.size} security observations were identified across the engagement.")
            if (criticalCount > 0 || highCount > 0) {
                appendLine("WARNING: $criticalCount Critical and $highCount High severity findings were documented and require immediate remediation.")
            } else {
                appendLine("No Critical or High severity findings were identified during the assessment window.")
            }
            appendLine("Remediation Status: $resolvedCount Resolved / $unresolvedCount Actionable or In-Progress.")
            appendLine()
        }

        // C. ASSESSMENT SCOPE
        if (config.includeScope && config.reportType != ReportType.FINDINGS_ONLY) {
            appendLine("2. ASSESSMENT SCOPE SPECIFICATION")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Assessment Type: ${engagement.assessmentType}")
            appendLine("Testing Window:  ${engagement.startDate} to ${engagement.endDate}")
            appendLine()
            appendLine("Authorized In-Scope Targets:")
            appendLine(engagement.authorizedScope.ifBlank { "As designated in the formal statement of work." })
            appendLine()
            if (engagement.outOfScope.isNotBlank()) {
                appendLine("Explicit Out-of-Scope Restrictions:")
                appendLine(engagement.outOfScope)
                appendLine()
            }
        }

        // D. METHODOLOGY
        if (config.includeMethodology && config.reportType != ReportType.FINDINGS_ONLY) {
            appendLine("3. METHODOLOGY & TESTING FRAMEWORK")
            appendLine("--------------------------------------------------------------------------------")
            val methodology = config.customMethodologyText.ifBlank {
                defaultMethodologyText(engagement.assessmentType)
            }
            appendLine(methodology)
            appendLine()
            appendLine("Attestation: All assessment activities were executed strictly within the declared rules")
            appendLine("of engagement. RedOps Mobile records solely documented evaluator observations.")
            appendLine()
        }

        // E. FINDINGS SUMMARY MATRIX
        if (config.includeFindingsSummary && config.reportType != ReportType.EXECUTIVE) {
            appendLine("4. VULNERABILITY FINDINGS METRICS")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Severity Distribution Breakdown:")
            appendLine("  [!] Critical:      $criticalCount")
            appendLine("  [*] High:          $highCount")
            appendLine("  [#] Medium:        $mediumCount")
            appendLine("  [-] Low:           $lowCount")
            appendLine("  [i] Informational: $infoCount")
            appendLine("  ----------------------------")
            appendLine("  Total Findings:    ${findings.size}")
            appendLine()

            appendLine("Findings Inventory Table:")
            appendLine(String.format(Locale.getDefault(), "%-8s | %-12s | %-6s | %-12s | %s", "ID", "SEVERITY", "CVSS", "STATUS", "TITLE"))
            appendLine("--------------------------------------------------------------------------------")
            if (findings.isEmpty()) {
                appendLine("No security findings recorded.")
            } else {
                findings.forEach { f ->
                    val cvssStr = f.cvssScore?.toString() ?: "N/A"
                    appendLine(String.format(Locale.getDefault(), "%-8s | %-12s | %-6s | %-12s | %s",
                        f.customId.take(8),
                        f.severity.take(12),
                        cvssStr.take(6),
                        f.status.take(12),
                        f.title
                    ))
                }
            }
            appendLine()
        }

        // F. DETAILED FINDINGS
        if (config.includeDetailedFindings && config.reportType != ReportType.EXECUTIVE) {
            appendLine("5. DETAILED VULNERABILITY OBSERVATIONS")
            appendLine("--------------------------------------------------------------------------------")
            if (findings.isEmpty()) {
                appendLine("No individual findings to display.")
            } else {
                findings.forEachIndexed { idx, f ->
                    appendLine("[${idx + 1}] [${f.customId}] ${f.title}")
                    appendLine("    Severity:           ${f.severity.uppercase()}")
                    if (f.cvssScore != null) appendLine("    CVSS v3.1 Score:    ${f.cvssScore}")
                    if (!f.cweReference.isNullOrBlank()) appendLine("    CWE Classification: ${f.cweReference}")
                    appendLine("    Category:           ${f.category}")
                    appendLine("    Lifecycle Status:   ${f.status}")
                    appendLine("    Affected Asset:     ${f.affectedAsset}")
                    if (f.discoveryDate.isNotBlank()) appendLine("    Discovery Date:     ${f.discoveryDate}")
                    appendLine()
                    appendLine("    Vulnerability Description:")
                    appendLine("    ${f.description.replace("\n", "\n    ")}")
                    appendLine()
                    if (f.technicalImpact.isNotBlank()) {
                        appendLine("    Potential Technical & Business Impact:")
                        appendLine("    ${f.technicalImpact.replace("\n", "\n    ")}")
                        appendLine()
                    }
                    if (!f.pocReference.isNullOrBlank()) {
                        appendLine("    Proof of Concept / Technical Details:")
                        appendLine("    ${f.pocReference.replace("\n", "\n    ")}")
                        appendLine()
                    }
                    if (!f.evidenceReference.isNullOrBlank()) {
                        appendLine("    Evidence Reference: ${f.evidenceReference}")
                        appendLine()
                    }
                    appendLine("    Recommended Remediation:")
                    appendLine("    ${f.remediation.replace("\n", "\n    ")}")
                    appendLine("    ----------------------------------------------------------------------------")
                    appendLine()
                }
            }
        }

        // G. REMEDIATION SUMMARY
        if (config.includeRemediation && config.reportType != ReportType.FINDINGS_ONLY) {
            appendLine("6. REMEDIATION & MITIGATION ROADMAP")
            appendLine("--------------------------------------------------------------------------------")
            val criticalFindings = findings.filter { it.severity.equals("Critical", true) }
            val highFindings = findings.filter { it.severity.equals("High", true) }
            val otherFindings = findings.filter { !it.severity.equals("Critical", true) && !it.severity.equals("High", true) }

            if (criticalFindings.isNotEmpty() || highFindings.isNotEmpty()) {
                appendLine("Immediate Priority Actions (Critical & High Severity):")
                (criticalFindings + highFindings).forEach { f ->
                    appendLine("• [${f.customId}] ${f.title}: ${f.remediation}")
                }
                appendLine()
            }

            if (otherFindings.isNotEmpty()) {
                appendLine("Scheduled Remediation (Medium, Low & Informational):")
                otherFindings.forEach { f ->
                    appendLine("• [${f.customId}] ${f.title}: ${f.remediation}")
                }
                appendLine()
            }
        }

        // H. CONCLUSION
        if (config.reportType != ReportType.FINDINGS_ONLY) {
            appendLine("7. CONCLUSION")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("The assessment of ${engagement.client} concluded with ${findings.size} documented findings,")
            appendLine("of which $resolvedCount have been resolved and $unresolvedCount remain open for mitigation.")
            appendLine("Prioritizing high-risk remediation actions will substantially reduce the observable attack")
            appendLine("surface and improve the defensive resilience of the evaluated environment.")
            appendLine()
        }

        // I. APPENDIX
        appendLine("8. APPENDIX")
        appendLine("--------------------------------------------------------------------------------")
        if (config.includeEvidenceInventory) {
            appendLine("Evidence Artifacts Inventory (${evidence.size} files):")
            if (evidence.isEmpty()) {
                appendLine("  No evidence files attached.")
            } else {
                evidence.forEach { ev ->
                    val hash = if (ev.sha256Checksum.isNotBlank()) " | SHA-256: ${ev.sha256Checksum.take(16)}..." else ""
                    appendLine("  • [${ev.category}] ${ev.title} (${ev.originalFilename.ifBlank { ev.uriOrPath.substringAfterLast("/") }}, ${ev.fileSize / 1024} KB$hash)")
                }
            }
            appendLine()
        }

        if (config.includeNotebookNotes && notes.isNotEmpty()) {
            appendLine("Documented Notebook Entries (${notes.size} entries):")
            notes.forEach { n ->
                appendLine("  • [${n.category}] ${n.title} (${n.entryType})")
                appendLine("    ${n.description.replace("\n", "\n    ")}")
            }
            appendLine()
        }

        appendLine("Severity Rating Standard Reference:")
        appendLine("  • CRITICAL: Direct compromise, remote code execution, or full authentication bypass.")
        appendLine("  • HIGH: Significant data exposure, privilege escalation, or access to sensitive components.")
        appendLine("  • MEDIUM: Conditional exploits, CSRF, or sensitive information leakage.")
        appendLine("  • LOW: Minor configuration issues, verbose headers, or defense-in-depth deviations.")
        appendLine("  • INFORMATIONAL: Observations, design notices, and hardening recommendations.")
        appendLine()
        appendLine("================================================================================")
        appendLine("                     END OF REPORT — REDOPS MOBILE")
        appendLine("================================================================================")
    }

    private fun defaultMethodologyText(assessmentType: String): String {
        return """
            The assessment was conducted in accordance with industry-standard methodologies including the
            OWASP Web Security Testing Guide (WSTG) and the Penetration Testing Execution Standard (PTES).
            Testing procedures combined passive reconnaissance, non-destructive validation, and observed
            behavioral analysis to verify exposures within the explicitly authorized scope for $assessmentType.
        """.trimIndent()
    }
}
