package com.redops.mobile.utils

import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.repository.DashboardRepository
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.EvidenceRepository
import com.redops.mobile.domain.repository.FindingRepository
import com.redops.mobile.domain.repository.NotebookRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SampleDataGenerator {

    suspend fun populateSampleData(
        engagementRepo: EngagementRepository,
        findingRepo: FindingRepository,
        notebookRepo: NotebookRepository,
        evidenceRepo: EvidenceRepository,
        dashboardRepo: DashboardRepository
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val eng1 = Engagement(
            id = "eng-acme-corp-2026",
            title = "Acme Financial Web & API Audit",
            client = "Acme Financial Corp",
            description = "Annual authorized security assessment of customer banking portal and transaction API gateways.",
            assessmentType = "Web Application",
            startDate = "2026-10-01",
            endDate = "2026-10-25",
            status = "Active",
            authorizedScope = "https://banking.acme.corp, https://api.acme.corp/v1, 192.168.10.0/24 (dmz gateway)",
            outOfScope = "Production core banking database hosts (*.db.internal), Third-party payment gateways, Personnel workstations",
            assessmentObjectives = "Assess authentication controls, evaluate input sanitization, test API authorization tokens, and audit TLS configurations.",
            isAuthorized = true,
            additionalNotes = "Scope Agreement #ACME-SEC-2026-09 signed by Chief Information Security Officer."
        )

        val eng2 = Engagement(
            id = "eng-health-portal-2026",
            title = "MediVault Health Record System Review",
            client = "MediVault Healthcare Systems",
            description = "Pre-deployment HIPAA compliance and penetration test of clinician mobile API and patient access portal.",
            assessmentType = "API Security",
            startDate = "2026-09-10",
            endDate = "2026-09-30",
            status = "Completed",
            authorizedScope = "https://patient.medivault.health, https://api.medivault.health/fhir/v4",
            outOfScope = "Live patient electronic health record database servers, Telehealth WebRTC streaming servers",
            assessmentObjectives = "Verify FHIR API access control tokens, test patient data segmentation, identify potential IDOR vulnerabilities.",
            isAuthorized = true,
            additionalNotes = "All testing performed in designated staging environment with synthetic patient records."
        )

        engagementRepo.saveEngagement(eng1)
        engagementRepo.saveEngagement(eng2)

        // Findings for Engagement 1
        val f1 = Finding(
            id = "fnd-01",
            customId = "F-001",
            engagementId = eng1.id,
            title = "SQL Injection in Order Search Parameter",
            category = "Injection",
            severity = "Critical",
            cvssScore = 9.8,
            affectedAsset = "https://banking.acme.corp/api/v1/transactions?search=",
            cweReference = "CWE-89",
            description = "The transaction history query parameter 'search' concatenates unsanitized user input into raw SQL queries without parameterized binding.",
            technicalImpact = "Allows unauthorized read access to transaction logs and potential extraction of sensitive customer account numbers.",
            evidenceReference = "HTTP GET /api/v1/transactions?search=' OR 1=1-- returned all customer rows.",
            pocReference = "payload: ' UNION SELECT null, account_num, balance FROM accounts--",
            remediation = "Use parameterized SQL queries with PreparedStatement or ORM query bindings. Enforce strict input validation regex.",
            status = "Open"
        )

        val f2 = Finding(
            id = "fnd-02",
            customId = "F-002",
            engagementId = eng1.id,
            title = "Broken Object-Level Authorization (BOLA/IDOR)",
            category = "Broken Access Control",
            severity = "High",
            cvssScore = 8.5,
            affectedAsset = "https://api.acme.corp/v1/statements/{statementId}",
            cweReference = "CWE-639",
            description = "Changing the statementId parameter in statement download requests allows authenticated users to access billing statements of other customers.",
            technicalImpact = "Cross-tenant data exposure violating customer financial privacy.",
            evidenceReference = "User A (ID 4021) successfully fetched statement for User B (ID 4022).",
            pocReference = "GET /v1/statements/STMT-9921 with Bearer token for User A returned User B's statement PDF.",
            remediation = "Enforce server-side authorization checks verifying that the requesting user's tenant ID matches the requested resource owner.",
            status = "In Progress"
        )

        val f3 = Finding(
            id = "fnd-03",
            customId = "F-003",
            engagementId = eng1.id,
            title = "Missing HTTP Strict Transport Security (HSTS)",
            category = "Security Misconfiguration",
            severity = "Low",
            cvssScore = 3.5,
            affectedAsset = "https://banking.acme.corp",
            cweReference = "CWE-319",
            description = "The web server response headers do not include the Strict-Transport-Security header, allowing potential SSL-stripping attacks on insecure networks.",
            technicalImpact = "Users on untrusted Wi-Fi could have initial HTTP connections intercepted before redirection to HTTPS.",
            remediation = "Configure Nginx/Apache to include 'Strict-Transport-Security: max-age=31536000; includeSubDomains; preload'.",
            status = "Resolved"
        )

        val f4 = Finding(
            id = "fnd-04",
            customId = "F-004",
            engagementId = eng2.id,
            title = "JWT Token Weak Algorithm Support (alg: none)",
            category = "Authentication",
            severity = "Critical",
            cvssScore = 9.1,
            affectedAsset = "https://api.medivault.health/fhir/v4/auth",
            cweReference = "CWE-347",
            description = "The JWT verification library accepted unsigned tokens with the algorithm header set to 'none', allowing arbitrary claim forging.",
            technicalImpact = "Complete bypass of API authentication allowing privilege escalation to hospital administrator role.",
            remediation = "Explicitly configure JWT library to require RS256/ES256 and reject the 'none' algorithm unconditionally.",
            status = "Resolved"
        )

        findingRepo.saveFinding(f1)
        findingRepo.saveFinding(f2)
        findingRepo.saveFinding(f3)
        findingRepo.saveFinding(f4)

        // Notebook entries
        val n1 = NotebookEntry(
            id = "note-01",
            engagementId = eng1.id,
            title = "Kickoff Meeting & RoE Clarification",
            entryType = "Meeting Record",
            description = "Met with Acme SecOps team (lead: Alice CISO). Confirmed testing hours are 20:00 to 04:00 UTC. Emergency escalation contact: security-ops@acme.corp.",
            tags = "Kickoff, RoE, Contacts"
        )

        val n2 = NotebookEntry(
            id = "note-02",
            engagementId = eng1.id,
            title = "TLS Cipher Suite Evaluation",
            entryType = "Technical Observation",
            description = "Evaluated HTTPS endpoint using sslscan. TLS 1.2 and TLS 1.3 supported. No deprecated CBC ciphers observed on main gateway.",
            tags = "TLS, Cryptography, SSLScan"
        )

        val n3 = NotebookEntry(
            id = "note-03",
            engagementId = eng2.id,
            title = "FHIR Patient API Rate Limiting Test",
            entryType = "Assessment Note",
            description = "Observed 429 Too Many Requests response after 100 requests in 60 seconds. Rate limiter correctly configured per client IP.",
            tags = "RateLimiting, API, DefensiveControls"
        )

        notebookRepo.saveEntry(n1)
        notebookRepo.saveEntry(n2)
        notebookRepo.saveEntry(n3)

        // Evidence records
        val ev1 = EvidenceRecord(
            id = "ev-01",
            engagementId = eng1.id,
            findingId = f1.id,
            title = "SQL Injection HTTP Request Evidence",
            description = "Raw HTTP request and response showing SQL database syntax error in order lookup.",
            evidenceType = "HTTP Request/Response",
            uriOrPath = "evidence/fnd_01_sqli_poc.txt",
            fileType = "text/plain",
            fileSize = 1420L
        )

        val ev2 = EvidenceRecord(
            id = "ev-02",
            engagementId = eng1.id,
            findingId = f2.id,
            title = "Cross-Tenant Statement IDOR Screenshot",
            description = "Captured screen showing User A viewing User B statement details.",
            evidenceType = "Screenshot / Image",
            uriOrPath = "evidence/fnd_02_idor_screen.png",
            fileType = "image/png",
            fileSize = 254100L
        )

        evidenceRepo.saveEvidence(ev1)
        evidenceRepo.saveEvidence(ev2)

        dashboardRepo.logActivity("Sample Course Data Loaded", "SECURITY", "AUTHORIZED", eng1.id)
    }
}

