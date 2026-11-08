package com.redops.mobile.domain.model

import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.data.local.entity.EngagementEntity
import com.redops.mobile.data.local.entity.EvidenceRecordEntity
import com.redops.mobile.data.local.entity.FindingEntity
import com.redops.mobile.data.local.entity.NotebookEntryEntity
import com.redops.mobile.data.local.entity.ReportHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelMappingTest {

    @Test
    fun engagementEntity_mapsToDomainModelCorrectly() {
        val entity = EngagementEntity(
            id = "eng-001",
            title = "Alpha Web Audit",
            client = "CyberCorp",
            description = "External penetration test",
            assessmentType = "Web Application",
            startDate = "2026-11-01",
            endDate = "2026-11-10",
            status = "Active",
            authorizedScope = "*.cybercorp.com",
            outOfScope = "production-billing.cybercorp.com",
            assessmentObjectives = "Identify OWASP Top 10 vulnerabilities",
            isAuthorized = true,
            authorizationTimestamp = 1700000000000L,
            additionalNotes = "Contact POC on Slack"
        )

        val domain = entity.toDomain()

        assertEquals("eng-001", domain.id)
        assertEquals("Alpha Web Audit", domain.title)
        assertEquals("CyberCorp", domain.client)
        assertEquals("Web Application", domain.assessmentType)
        assertTrue(domain.isAuthorized)
        assertEquals(1700000000000L, domain.authorizationTimestamp)
        assertEquals("*.cybercorp.com", domain.authorizedScope)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.id, convertedEntity.id)
        assertEquals(entity.title, convertedEntity.title)
        assertEquals(entity.isAuthorized, convertedEntity.isAuthorized)
        assertEquals(entity.authorizationTimestamp, convertedEntity.authorizationTimestamp)
    }

    @Test
    fun findingEntity_mapsToDomainModelCorrectly() {
        val entity = FindingEntity(
            id = "f-101",
            customId = "F-101",
            engagementId = "eng-001",
            title = "SQL Injection in Login",
            category = "Injection",
            severity = "Critical",
            cvssScore = 9.8,
            affectedAsset = "https://api.cybercorp.com/v1/auth",
            cweReference = "CWE-89",
            description = "Unsanitized user input in username parameter",
            technicalImpact = "Full database compromise",
            evidenceReference = "evidence-01.png",
            pocReference = "' OR 1=1 --",
            remediation = "Use parameterized queries",
            status = "Open",
            discoveryDate = "2026-11-05",
            generalNotes = "Verified on staging environment"
        )

        val domain = entity.toDomain()

        assertEquals("f-101", domain.id)
        assertEquals("Critical", domain.severity)
        assertEquals(9.8, domain.cvssScore ?: 0.0, 0.001)
        assertEquals("Open", domain.status)
        assertEquals("2026-11-05", domain.discoveryDate)
        assertEquals("Verified on staging environment", domain.generalNotes)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.title, convertedEntity.title)
        assertEquals(entity.cweReference, convertedEntity.cweReference)
        assertEquals(entity.discoveryDate, convertedEntity.discoveryDate)
        assertEquals(entity.generalNotes, convertedEntity.generalNotes)
    }

    @Test
    fun notebookEntryEntity_mapsToDomainModelCorrectly() {
        val entity = NotebookEntryEntity(
            id = "note-01",
            engagementId = "eng-001",
            title = "Subdomain Enumeration Results",
            category = "Enumeration",
            entryType = "Technical Observation",
            description = "Discovered 14 exposed endpoints on staging environment.",
            tags = "recon,subdomain",
            evidenceReference = "nmap_output.txt",
            isPinned = true,
            isFavorite = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        val domain = entity.toDomain()

        assertEquals("note-01", domain.id)
        assertEquals("Enumeration", domain.category)
        assertEquals("Technical Observation", domain.entryType)
        assertEquals("recon,subdomain", domain.tags)
        assertTrue(domain.isPinned)
        assertTrue(domain.isFavorite)
        assertEquals(1700000050000L, domain.updatedAt)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.description, convertedEntity.description)
        assertEquals(entity.category, convertedEntity.category)
        assertEquals(entity.isPinned, convertedEntity.isPinned)
        assertEquals(entity.isFavorite, convertedEntity.isFavorite)
    }

    @Test
    fun evidenceRecordEntity_mapsToDomainModelCorrectly() {
        val entity = EvidenceRecordEntity(
            id = "ev-01",
            engagementId = "eng-001",
            findingId = "f-101",
            title = "SQL Error Dump",
            description = "Screenshot showing MySQL syntax error",
            evidenceType = "Screenshot",
            category = "Screenshot",
            originalFilename = "mysql_error_dump.png",
            storedFilename = "ev_1700000000000_mysql_error_dump.png",
            uriOrPath = "/data/user/0/com.redops.mobile/files/evidence/ev_1700000000000_mysql_error_dump.png",
            fileType = "image/png",
            fileSize = 204850L,
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        val domain = entity.toDomain()

        assertEquals("ev-01", domain.id)
        assertEquals("Screenshot", domain.category)
        assertEquals("mysql_error_dump.png", domain.originalFilename)
        assertEquals("ev_1700000000000_mysql_error_dump.png", domain.storedFilename)
        assertEquals(204850L, domain.fileSize)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", domain.sha256Checksum)
        assertEquals(1700000050000L, domain.updatedAt)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.uriOrPath, convertedEntity.uriOrPath)
        assertEquals(entity.originalFilename, convertedEntity.originalFilename)
        assertEquals(entity.sha256Checksum, convertedEntity.sha256Checksum)
    }

    @Test
    fun activityLogEntity_mapsToDomainModelCorrectly() {
        val entity = ActivityLogEntity(
            id = "act-001",
            entityId = "f-101",
            engagementId = "eng-001",
            title = "Logged Critical finding: SQL Injection",
            type = "FINDING",
            statusBadge = "CONFIRMED",
            timestamp = 1700000000000L
        )

        val domain = entity.toDomain()

        assertEquals("act-001", domain.id)
        assertEquals("f-101", domain.entityId)
        assertEquals("FINDING", domain.type)
        assertEquals("CONFIRMED", domain.statusBadge)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.entityId, convertedEntity.entityId)
        assertEquals(entity.statusBadge, convertedEntity.statusBadge)
    }

    @Test
    fun reportHistoryEntity_mapsToDomainModelCorrectly() {
        val entity = ReportHistoryEntity(
            id = "rep-001",
            engagementId = "eng-001",
            engagementTitle = "Alpha Web Audit",
            clientName = "CyberCorp",
            title = "Security Assessment Report: Alpha Web Audit",
            reportType = "FULL",
            format = "PDF",
            classification = "Confidential",
            filePath = "/data/user/0/com.redops.mobile/files/reports/RedOps_Report_CyberCorp_1700000000.pdf",
            fileSize = 1048576L,
            findingsCount = 5,
            reportVersion = "1.0",
            generatedAt = 1700000000000L
        )

        val domain = entity.toDomain()

        assertEquals("rep-001", domain.id)
        assertEquals("eng-001", domain.engagementId)
        assertEquals("Alpha Web Audit", domain.engagementTitle)
        assertEquals("CyberCorp", domain.clientName)
        assertEquals("Security Assessment Report: Alpha Web Audit", domain.title)
        assertEquals("FULL", domain.reportType)
        assertEquals("PDF", domain.format)
        assertEquals("Confidential", domain.classification)
        assertEquals(1048576L, domain.fileSize)
        assertEquals(5, domain.findingsCount)
        assertEquals("1.0", domain.reportVersion)
        assertEquals(1700000000000L, domain.generatedAt)

        val convertedEntity = domain.toEntity()
        assertEquals(entity.id, convertedEntity.id)
        assertEquals(entity.filePath, convertedEntity.filePath)
        assertEquals(entity.fileSize, convertedEntity.fileSize)
        assertEquals(entity.reportType, convertedEntity.reportType)
    }
}
