package com.redops.mobile.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.redops.mobile.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class PdfReportGenerator(private val context: Context?) {

    // Standard A4 dimensions at 72 DPI: 595 x 842 points
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f

    open fun generateReport(
        engagement: Engagement,
        findings: List<Finding>,
        notes: List<NotebookEntry> = emptyList(),
        evidence: List<EvidenceRecord> = emptyList(),
        config: ReportConfiguration = ReportConfiguration()
    ): File {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        // 1. COVER PAGE
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        drawCoverPage(page1.canvas, engagement, findings.size, config)
        pdfDocument.finishPage(page1)

        // 2. EXECUTIVE SUMMARY & SCOPE (if included)
        if (config.includeExecutiveSummary && config.reportType != ReportType.FINDINGS_ONLY) {
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            drawExecutiveSummaryPage(page2.canvas, engagement, findings, pageNumber - 1, config)
            pdfDocument.finishPage(page2)
        }

        // 3. FINDINGS OVERVIEW & METRICS (if included)
        if (config.includeFindingsSummary && config.reportType != ReportType.EXECUTIVE) {
            val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page3 = pdfDocument.startPage(pageInfo3)
            drawFindingsOverviewPage(page3.canvas, engagement, findings, pageNumber - 1, config)
            pdfDocument.finishPage(page3)
        }

        // 4. DETAILED FINDINGS PAGES (if included)
        if (config.includeDetailedFindings && config.reportType != ReportType.EXECUTIVE && findings.isNotEmpty()) {
            val findingsPerPage = 2
            val findingChunks = findings.chunked(findingsPerPage)
            findingChunks.forEachIndexed { chunkIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val page = pdfDocument.startPage(pageInfo)
                drawFindingsDetailPage(page.canvas, chunk, pageNumber - 1, chunkIndex == 0, config)
                pdfDocument.finishPage(page)
            }
        }

        // 5. REMEDIATION & CONCLUSION PAGE (if included)
        if (config.reportType != ReportType.FINDINGS_ONLY) {
            val pageInfoConclusion = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val pageConclusion = pdfDocument.startPage(pageInfoConclusion)
            drawConclusionPage(pageConclusion.canvas, engagement, findings, pageNumber - 1, config)
            pdfDocument.finishPage(pageConclusion)
        }

        // 6. APPENDIX PAGE (Evidence & Notebook entries)
        if (config.includeEvidenceInventory || config.includeNotebookNotes) {
            val pageInfoAppendix = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val pageAppendix = pdfDocument.startPage(pageInfoAppendix)
            drawAppendixPage(pageAppendix.canvas, notes, evidence, pageNumber, config)
            pdfDocument.finishPage(pageAppendix)
        }

        // Ensure reports directory exists
        val baseDir = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        val reportsDir = File(baseDir, "reports").apply { mkdirs() }
        val cleanClient = engagement.client.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val fileName = "RedOps_Report_${cleanClient}_${System.currentTimeMillis()}.${config.format.extension}"
        val reportFile = File(reportsDir, fileName)

        FileOutputStream(reportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return reportFile
    }

    private fun drawCoverPage(canvas: Canvas, engagement: Engagement, totalFindings: Int, config: ReportConfiguration) {
        val bgPaint = Paint().apply { color = Color.parseColor("#0B0F0E") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Header Accent Strip
        val accentPaint = Paint().apply { color = Color.parseColor("#00E676") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, accentPaint)

        // Brand Banner
        val brandPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("REDOPS MOBILE", margin, 110f, brandPaint)

        val subBrandPaint = Paint().apply {
            color = Color.parseColor("#69F0AE")
            textSize = 11.5f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val typeHeader = when (config.reportType) {
            ReportType.FULL -> "FULL SECURITY ASSESSMENT REPORT"
            ReportType.EXECUTIVE -> "EXECUTIVE SECURITY SUMMARY"
            ReportType.FINDINGS_ONLY -> "TECHNICAL FINDINGS SPECIFICATION"
        }
        canvas.drawText(typeHeader, margin, 132f, subBrandPaint)

        // Rule
        val rulePaint = Paint().apply { color = Color.parseColor("#1B2934"); strokeWidth = 2f }
        canvas.drawLine(margin, 150f, pageWidth - margin, 150f, rulePaint)

        // Main Target Info Card
        val cardPaint = Paint().apply { color = Color.parseColor("#141B18") }
        val cardRect = RectF(margin, 175f, pageWidth - margin, 420f)
        canvas.drawRoundRect(cardRect, 12f, 12f, cardPaint)

        val labelPaint = Paint().apply {
            color = Color.parseColor("#A7B0AA")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 210f
        canvas.drawText("ASSESSMENT REPORT TITLE:", margin + 20f, y, labelPaint)
        val titleText = config.customTitle.ifBlank { engagement.title }
        canvas.drawText(titleText.take(50), margin + 20f, y + 18f, valuePaint)

        y += 48f
        canvas.drawText("TARGET CLIENT / ORGANIZATION:", margin + 20f, y, labelPaint)
        canvas.drawText(engagement.client, margin + 20f, y + 18f, valuePaint)

        y += 48f
        canvas.drawText("ASSESSMENT TYPE & TIMELINE:", margin + 20f, y, labelPaint)
        canvas.drawText("${engagement.assessmentType}  |  ${engagement.startDate} to ${engagement.endDate}", margin + 20f, y + 18f, valuePaint)

        y += 48f
        canvas.drawText("OBSERVED VULNERABILITY FINDINGS:", margin + 20f, y, labelPaint)
        val statsPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("$totalFindings documented findings  (Version: ${config.version})", margin + 20f, y + 18f, statsPaint)

        // Classification & Authorization Badges
        val classBgPaint = Paint().apply { color = Color.parseColor("#182C2A") }
        val badgeRect = RectF(margin, 445f, pageWidth - margin, 545f)
        canvas.drawRoundRect(badgeRect, 8f, 8f, classBgPaint)

        val classColor = when (config.classification) {
            ConfidentialityLevel.STRICTLY_CONFIDENTIAL -> "#FF5252"
            ConfidentialityLevel.RESTRICTED -> "#FF9100"
            else -> "#00E676"
        }
        val classPaint = Paint().apply {
            color = Color.parseColor(classColor)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("[!] CLASSIFICATION: ${config.classification.displayName.uppercase()}", margin + 18f, 478f, classPaint)

        val detailPaint = Paint().apply { color = Color.parseColor("#D0D8D4"); textSize = 10f }
        canvas.drawText("Authorization: ${if (engagement.isAuthorized) "CONFIRMED & DECLARED" else "UNCONFIRMED"}", margin + 18f, 502f, detailPaint)
        canvas.drawText("Testing was restricted exclusively to mutually agreed in-scope assets.", margin + 18f, 522f, detailPaint)

        // Confidentiality Notice Box
        val noticeRect = RectF(margin, 675f, pageWidth - margin, 775f)
        canvas.drawRoundRect(noticeRect, 8f, 8f, cardPaint)

        val noticeHeadPaint = Paint().apply {
            color = Color.parseColor("#FF5252")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CONFIDENTIALITY & CLASSIFICATION NOTICE", margin + 16f, 702f, noticeHeadPaint)

        val noticeBodyPaint = Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 9.5f }
        canvas.drawText("This document contains proprietary and sensitive security assessment findings.", margin + 16f, 722f, noticeBodyPaint)
        canvas.drawText("Unauthorized distribution, duplication, or dissemination is strictly prohibited.", margin + 16f, 738f, noticeBodyPaint)
        canvas.drawText("Synthesized offline via RedOps Mobile Platform on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}.", margin + 16f, 754f, noticeBodyPaint)
    }

    private fun drawExecutiveSummaryPage(canvas: Canvas, engagement: Engagement, findings: List<Finding>, pageNum: Int, config: ReportConfiguration) {
        drawPageBackgroundAndHeader(canvas, "01. EXECUTIVE SUMMARY & SCOPE", pageNum, config.classification)

        var y = 90f
        val bodyPaint = Paint().apply { color = Color.parseColor("#D0D8D4"); textSize = 10.5f }
        val headingPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        // Overview
        canvas.drawText("1.1 Executive Overview", margin, y, headingPaint)
        y += 18f
        val descText = engagement.description.ifBlank {
            "An authorized technical security assessment was conducted for ${engagement.client} targeting in-scope assets."
        }
        y = drawWrappedText(canvas, descText, margin, y, pageWidth - (margin * 2), bodyPaint) + 14f

        // Objectives
        canvas.drawText("1.2 Assessment Objectives", margin, y, headingPaint)
        y += 18f
        val objText = engagement.assessmentObjectives.ifBlank {
            "Identify observable security weaknesses, evaluate risk severity, and deliver prioritized remediation recommendations."
        }
        y = drawWrappedText(canvas, objText, margin, y, pageWidth - (margin * 2), bodyPaint) + 14f

        // Authorized Scope
        canvas.drawText("1.3 Authorized In-Scope Targets", margin, y, headingPaint)
        y += 18f
        y = drawWrappedText(canvas, engagement.authorizedScope, margin, y, pageWidth - (margin * 2), bodyPaint) + 14f

        // Out-of-Scope Restrictions
        if (engagement.outOfScope.isNotBlank()) {
            val dangerHeading = Paint().apply { color = Color.parseColor("#FF5252"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("1.4 Explicit Out-of-Scope Assets", margin, y, dangerHeading)
            y += 18f
            y = drawWrappedText(canvas, engagement.outOfScope, margin, y, pageWidth - (margin * 2), bodyPaint) + 14f
        }

        // Methodology
        canvas.drawText("1.5 Testing Methodology & Safety Boundaries", margin, y, headingPaint)
        y += 18f
        val methodology = config.customMethodologyText.ifBlank {
            "Testing followed industry standards including OWASP WSTG and PTES guidelines. All findings represent documented observations. No destructive exploits, denial-of-service, or unauthorized persistence were executed."
        }
        drawWrappedText(canvas, methodology, margin, y, pageWidth - (margin * 2), bodyPaint)
    }

    private fun drawFindingsOverviewPage(canvas: Canvas, engagement: Engagement, findings: List<Finding>, pageNum: Int, config: ReportConfiguration) {
        drawPageBackgroundAndHeader(canvas, "02. FINDINGS METRICS & SUMMARY TABLE", pageNum, config.classification)

        val critical = findings.count { it.severity.equals("Critical", true) }
        val high = findings.count { it.severity.equals("High", true) }
        val medium = findings.count { it.severity.equals("Medium", true) }
        val low = findings.count { it.severity.equals("Low", true) }
        val info = findings.count { it.severity.equals("Informational", true) || it.severity.equals("Info", true) }
        val resolved = findings.count { it.status.equals("Resolved", true) }

        var y = 90f
        val boxWidth = (pageWidth - (margin * 2) - 30f) / 4f
        val boxHeight = 55f

        drawStatBox(canvas, margin, y, boxWidth, boxHeight, "TOTAL", findings.size.toString(), "#00E676")
        drawStatBox(canvas, margin + boxWidth + 10f, y, boxWidth, boxHeight, "CRITICAL", critical.toString(), "#FF5252")
        drawStatBox(canvas, margin + (boxWidth + 10f) * 2, y, boxWidth, boxHeight, "HIGH", high.toString(), "#FF9100")
        drawStatBox(canvas, margin + (boxWidth + 10f) * 3, y, boxWidth, boxHeight, "RESOLVED", resolved.toString(), "#69F0AE")

        y += 80f
        val headingPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("2.1 Severity Distribution Table", margin, y, headingPaint)

        y += 18f
        val thPaint = Paint().apply { color = Color.parseColor("#1B2934") }
        canvas.drawRect(margin, y, pageWidth - margin, y + 24f, thPaint)

        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("SEVERITY", margin + 10f, y + 16f, thTextPaint)
        canvas.drawText("COUNT", margin + 130f, y + 16f, thTextPaint)
        canvas.drawText("PERCENTAGE", margin + 230f, y + 16f, thTextPaint)
        canvas.drawText("REMEDIATION PRIORITY", margin + 350f, y + 16f, thTextPaint)

        y += 24f
        val severities = listOf(
            Triple("Critical", critical, "#FF5252"),
            Triple("High", high, "#FF9100"),
            Triple("Medium", medium, "#FFD740"),
            Triple("Low", low, "#69F0AE"),
            Triple("Informational", info, "#57E6D4")
        )

        val trPaint = Paint().apply { color = Color.parseColor("#141B18") }
        val trAltPaint = Paint().apply { color = Color.parseColor("#0F1614") }
        val textPaint = Paint().apply { color = Color.WHITE; textSize = 9.5f }

        severities.forEachIndexed { i, (sev, count, colorHex) ->
            canvas.drawRect(margin, y, pageWidth - margin, y + 22f, if (i % 2 == 0) trPaint else trAltPaint)
            val badgePaint = Paint().apply { color = Color.parseColor(colorHex); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 9.5f }
            canvas.drawText(sev, margin + 10f, y + 15f, badgePaint)
            canvas.drawText(count.toString(), margin + 130f, y + 15f, textPaint)
            val pct = if (findings.isNotEmpty()) "%.1f%%".format((count.toFloat() / findings.size.toFloat()) * 100f) else "0.0%"
            canvas.drawText(pct, margin + 230f, y + 15f, textPaint)
            val priority = when (sev) {
                "Critical" -> "Immediate (< 24h)"
                "High" -> "High Priority (< 7d)"
                "Medium" -> "Standard (< 30d)"
                "Low" -> "Planned Cycle (< 90d)"
                else -> "Informational Review"
            }
            canvas.drawText(priority, margin + 350f, y + 15f, textPaint)
            y += 22f
        }

        y += 24f
        canvas.drawText("2.2 Summary Vulnerability Index", margin, y, headingPaint)
        y += 18f

        findings.take(8).forEach { f ->
            val bulletPaint = Paint().apply { color = Color.parseColor(severityColorHex(f.severity)); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("[${f.customId}] [${f.severity.uppercase()}]", margin, y, bulletPaint)
            val titlePaint = Paint().apply { color = Color.WHITE; textSize = 9.5f }
            val truncatedTitle = if (f.title.length > 50) f.title.take(47) + "..." else f.title
            canvas.drawText(truncatedTitle, margin + 130f, y, titlePaint)
            val statusPaint = Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 9f }
            canvas.drawText(f.status, pageWidth - margin - 60f, y, statusPaint)
            y += 18f
        }
    }

    private fun drawFindingsDetailPage(canvas: Canvas, chunk: List<Finding>, pageNum: Int, isFirst: Boolean, config: ReportConfiguration) {
        val title = if (isFirst) "03. DETAILED VULNERABILITY FINDINGS" else "03. DETAILED FINDINGS (CONT.)"
        drawPageBackgroundAndHeader(canvas, title, pageNum, config.classification)

        var y = 90f
        chunk.forEach { f ->
            val cardRect = RectF(margin, y, pageWidth - margin, y + 330f)
            val cardPaint = Paint().apply { color = Color.parseColor("#141B18") }
            canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

            val idPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 11.5f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
            canvas.drawText("${f.customId}: ${f.title.take(45)}", margin + 14f, y + 24f, idPaint)

            val sevColor = Color.parseColor(severityColorHex(f.severity))
            val sevPaint = Paint().apply { color = sevColor; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("Severity: ${f.severity}   |   CVSS: ${f.cvssScore ?: "N/A"}", margin + 14f, y + 44f, sevPaint)

            val statusPaint = Paint().apply { color = Color.parseColor("#69F0AE"); textSize = 9.5f }
            canvas.drawText("Status: ${f.status}   |   Category: ${f.category}", margin + 200f, y + 44f, statusPaint)

            val lblPaint = Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val valPaint = Paint().apply { color = Color.WHITE; textSize = 9f }

            var innerY = y + 68f
            canvas.drawText("AFFECTED ASSET:", margin + 14f, innerY, lblPaint)
            canvas.drawText(f.affectedAsset.take(65), margin + 120f, innerY, valPaint)

            if (!f.cweReference.isNullOrBlank()) {
                innerY += 15f
                canvas.drawText("CWE REFERENCE:", margin + 14f, innerY, lblPaint)
                canvas.drawText(f.cweReference, margin + 120f, innerY, valPaint)
            }

            innerY += 16f
            canvas.drawText("VULNERABILITY DESCRIPTION:", margin + 14f, innerY, lblPaint)
            innerY += 13f
            innerY = drawWrappedText(canvas, f.description, margin + 14f, innerY, pageWidth - (margin * 2) - 28f, valPaint)

            if (f.technicalImpact.isNotBlank()) {
                innerY += 10f
                canvas.drawText("TECHNICAL IMPACT:", margin + 14f, innerY, lblPaint)
                innerY += 13f
                innerY = drawWrappedText(canvas, f.technicalImpact, margin + 14f, innerY, pageWidth - (margin * 2) - 28f, valPaint)
            }

            innerY += 10f
            val remLblPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("RECOMMENDED REMEDIATION:", margin + 14f, innerY, remLblPaint)
            innerY += 13f
            drawWrappedText(canvas, f.remediation, margin + 14f, innerY, pageWidth - (margin * 2) - 28f, valPaint)

            y += 345f
        }
    }

    private fun drawConclusionPage(canvas: Canvas, engagement: Engagement, findings: List<Finding>, pageNum: Int, config: ReportConfiguration) {
        drawPageBackgroundAndHeader(canvas, "04. REMEDIATION ROADMAP & CONCLUSION", pageNum, config.classification)

        var y = 90f
        val headingPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint = Paint().apply { color = Color.parseColor("#D0D8D4"); textSize = 10f }

        // Remediation Roadmap
        canvas.drawText("4.1 Remediation Prioritization", margin, y, headingPaint)
        y += 18f
        val critHigh = findings.filter { it.severity.equals("Critical", true) || it.severity.equals("High", true) }
        if (critHigh.isNotEmpty()) {
            canvas.drawText("Immediate Priority (< 7 days):", margin, y, Paint().apply { color = Color.parseColor("#FF5252"); textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            y += 15f
            critHigh.take(3).forEach { f ->
                canvas.drawText("• [${f.customId}] ${f.title}: ${f.remediation.take(65)}...", margin + 10f, y, bodyPaint)
                y += 15f
            }
            y += 8f
        }

        // Conclusion
        canvas.drawText("4.2 Engagement Conclusion", margin, y, headingPaint)
        y += 18f
        val resolvedCount = findings.count { it.status.equals("Resolved", true) }
        val conclusionText = "The assessment engagement '${engagement.title}' for client '${engagement.client}' concluded on ${engagement.endDate}. A total of ${findings.size} findings were documented ($resolvedCount resolved). Mitigating identified exposures will substantially reduce attack surface."
        y = drawWrappedText(canvas, conclusionText, margin, y, pageWidth - (margin * 2), bodyPaint) + 20f

        // Attestation
        canvas.drawText("4.3 Responsible Assessment Attestation", margin, y, headingPaint)
        y += 18f
        val attestation = "The evaluator certifies that testing was performed strictly within documented authorization bounds. No unauthorized exploitation, denial-of-service, or lateral movement outside verified targets was conducted. Records are preserved locally on the secure device."
        y = drawWrappedText(canvas, attestation, margin, y, pageWidth - (margin * 2), bodyPaint) + 30f

        // Sign-off Card
        val signBox = RectF(margin, y, pageWidth - margin, y + 90f)
        val boxPaint = Paint().apply { color = Color.parseColor("#141B18") }
        canvas.drawRoundRect(signBox, 8f, 8f, boxPaint)

        val signLabel = Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 9.5f }
        val signValue = Paint().apply { color = Color.WHITE; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        canvas.drawText("LEAD EVALUATOR SIGNATURE:", margin + 16f, y + 28f, signLabel)
        canvas.drawText("AUTHORIZED SECURITY ASSESSOR — REDOPS PLATFORM", margin + 16f, y + 48f, signValue)
        canvas.drawText("DATE OF REPORT ISSUANCE: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}", margin + 16f, y + 70f, signLabel)
    }

    private fun drawAppendixPage(canvas: Canvas, notes: List<NotebookEntry>, evidence: List<EvidenceRecord>, pageNum: Int, config: ReportConfiguration) {
        drawPageBackgroundAndHeader(canvas, "05. APPENDIX & EVIDENCE INVENTORY", pageNum, config.classification)

        var y = 90f
        val headingPaint = Paint().apply { color = Color.parseColor("#00E676"); textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint = Paint().apply { color = Color.parseColor("#D0D8D4"); textSize = 9.5f }

        if (config.includeEvidenceInventory) {
            canvas.drawText("5.1 Attached Evidence Artifacts (${evidence.size} Records)", margin, y, headingPaint)
            y += 18f
            if (evidence.isEmpty()) {
                canvas.drawText("No evidence artifacts attached to this engagement scope.", margin + 10f, y, bodyPaint)
                y += 18f
            } else {
                evidence.take(6).forEach { ev ->
                    val hash = if (ev.sha256Checksum.isNotBlank()) " | Hash: ${ev.sha256Checksum.take(12)}..." else ""
                    canvas.drawText("• [${ev.category}] ${ev.title} (${ev.fileType}, ${ev.fileSize / 1024} KB$hash)", margin + 10f, y, bodyPaint)
                    y += 16f
                }
            }
            y += 14f
        }

        if (config.includeNotebookNotes && notes.isNotEmpty()) {
            canvas.drawText("5.2 Documented Assessment Notes (${notes.size} Entries)", margin, y, headingPaint)
            y += 18f
            notes.take(4).forEach { n ->
                canvas.drawText("• [${n.category}] ${n.title} (${n.entryType})", margin + 10f, y, bodyPaint)
                y += 14f
                val snippet = if (n.description.length > 80) n.description.take(77) + "..." else n.description
                canvas.drawText("  $snippet", margin + 14f, y, Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 9f })
                y += 16f
            }
            y += 14f
        }

        canvas.drawText("5.3 Severity Rating Standard Definitions", margin, y, headingPaint)
        y += 18f
        val defs = listOf(
            "CRITICAL: Direct exploitation leading to remote code execution or complete system takeover.",
            "HIGH: Significant unauthorized data access, privilege escalation, or integrity breach.",
            "MEDIUM: Vulnerabilities requiring specific conditions, CSRF, or sensitive information disclosure.",
            "LOW: Minor configuration deviations, missing non-critical security headers, or informational issues."
        )
        defs.forEach { d ->
            canvas.drawText("• $d", margin + 10f, y, bodyPaint)
            y += 16f
        }
    }

    private fun drawPageBackgroundAndHeader(canvas: Canvas, headerTitle: String, pageNumber: Int, classification: ConfidentialityLevel = ConfidentialityLevel.CONFIDENTIAL) {
        val bgPaint = Paint().apply { color = Color.parseColor("#0B0F0E") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Top Header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("REDOPS MOBILE ASSESSMENT REPORT", margin, 34f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headerTitle, margin, 54f, titlePaint)

        val linePaint = Paint().apply { color = Color.parseColor("#1B2934"); strokeWidth = 1f }
        canvas.drawLine(margin, 66f, pageWidth - margin, 66f, linePaint)

        // Bottom Footer
        canvas.drawLine(margin, pageHeight - 40f, pageWidth - margin, pageHeight - 40f, linePaint)
        val footerPaint = Paint().apply { color = Color.parseColor("#A7B0AA"); textSize = 8.5f }
        canvas.drawText("CONFIDENTIALITY: ${classification.displayName.uppercase()}", margin, pageHeight - 24f, footerPaint)
        val pageStr = "Page $pageNumber"
        canvas.drawText(pageStr, pageWidth - margin - 35f, pageHeight - 24f, footerPaint)
    }

    private fun drawStatBox(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, label: String, value: String, accentHex: String) {
        val rect = RectF(x, y, x + width, y + height)
        val bgPaint = Paint().apply { color = Color.parseColor("#141B18") }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val valPaint = Paint().apply {
            color = Color.parseColor(accentHex)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 12f, y + 28f, valPaint)

        val lblPaint = Paint().apply {
            color = Color.parseColor("#A7B0AA")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(label, x + 12f, y + 44f, lblPaint)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint): Float {
        var currentY = startY
        val words = text.split("\\s+".toRegex())
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width < maxWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += 14f
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += 14f
        }
        return currentY
    }

    private fun severityColorHex(severity: String): String = when (severity.lowercase()) {
        "critical" -> "#FF5252"
        "high" -> "#FF9100"
        "medium" -> "#FFD740"
        "low" -> "#69F0AE"
        else -> "#57E6D4"
    }
}
