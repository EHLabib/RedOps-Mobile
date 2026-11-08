# RedOps Mobile
## Authorized Cybersecurity Assessment Management Platform & Toolkit

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web%20Simulator-green.svg)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20MVVM%20%7C%20RESTful%20Node.js-blue.svg)]()
[![Database](https://img.shields.io/badge/Database-Room%20v6%20%7C%20SQLite%20better--sqlite3-orange.svg)]()
[![Security](https://img.shields.io/badge/Security-Keystore%20AES--256--GCM%20%7C%20PBKDF2%20%7C%20SSRF%20Guard-red.svg)]()
[![Tests](https://img.shields.io/badge/Automated%20Tests-157%2F157%20Passing%20(100%25)-brightgreen.svg)]()
[![Toolkit](https://img.shields.io/badge/Cybersecurity%20Tools-14%20Real%20Tools%20(Zero%20Mocks)-cyan.svg)]()

RedOps Mobile is an offline-first, professional cybersecurity assessment management platform and cybersecurity toolkit. Available as both an **Android native application** (Kotlin, Jetpack Compose, Material 3, Room v6 SQLite) and a **full-stack Web Testing Simulator & REST API** (Node.js, Express, better-sqlite3), RedOps Mobile provides security auditors, penetration testers, red team operators, and cybersecurity students with a unified environment to conduct passive reconnaissance, audit web defensive headers, execute cryptographic calculations, log vulnerability findings with CVSS v3.1 classification, manage forensic evidence with streaming SHA-256 integrity, and compile professional 10-section security assessment reports.

> **RESPONSIBLE USE & SAFETY BOUNDARY:**
> RedOps Mobile is strictly an administrative documentation, defensive auditing, and professional reporting platform. It does **not** conduct denial-of-service (DoS), execute automated exploitation payloads, inject packets, brute-force credentials, harvest passwords, or access unauthorized systems. Use of this application is permitted **only** for documenting authorized assessments with explicit, documented written permission from the target system owners.

---

## Table of Contents
1. [Core Capabilities & Modules](#core-capabilities--modules)
2. [Dual-Platform Architecture](#dual-platform-architecture)
3. [Modular Cybersecurity Toolkit (14 Real Tools)](#modular-cybersecurity-toolkit-14-real-tools)
4. [Web Application & API Server Guide](#web-application--api-server-guide)
5. [Database Architecture & Relational Schema](#database-architecture--relational-schema)
6. [Security, Privacy & Data Protection](#security-privacy--data-protection)
7. [User Guide & Operational Workflows](#user-guide--operational-workflows)
8. [Project Structure](#project-structure)
9. [Android Build, Installation & Release](#android-build-installation--release)
10. [Test Suite Verification (157 Tests)](#test-suite-verification-157-tests)
11. [Known Limitations & Roadmap](#known-limitations--roadmap)
12. [Ethical Governance & Legal Boundaries](#ethical-governance--legal-boundaries)

---

## Core Capabilities & Modules

### 1. Dynamic Security Dashboard
- **Live Assessment Metrics:** Real-time counters for active engagements, total findings, critical vulnerability counts, and pending remediation items calculated from persistent SQLite aggregates.
- **Severity Distribution Matrix:** Visual severity distribution breakdown (Critical, High, Medium, Low, Informational).
- **Recent Intelligence Stream:** Live chronological feeds of recent engagements, findings, and immutable activity events.
- **Quick Action Carousel:** Left-to-right / right-to-left slideable quick actions with arrow controls (`‹` and `›`) to initialize engagements, document findings, attach evidence, or review activity logs.

### 2. Engagement Management & Scope Governance
- **Lifecycle Tracking:** Full assessment workflow states: `Planning`, `Active`, `On Hold`, `Completed`, `Cancelled`.
- **Assessment Types:** Web Application, Network Infrastructure, Mobile Application, Internal Audit, External Audit, and Custom Scopes.
- **Strict Scope Boundaries:** Explicit demarcation between authorized in-scope assets and strictly prohibited out-of-scope targets.
- **Mandatory Authorization Gate:** Hard prerequisite requiring operators to certify documented authorization (`is_authorized: true`), stamping an immutable UNIX timestamp.

### 3. Vulnerability Findings & CVSS v3.1 Scoring
- **Structured Findings Catalog:** Title, affected asset URL/parameter, category, technical PoC details, business impact, and remediation guidance.
- **Industry Standards:** CVSS v3.1 calculator, CWE ID cross-referencing, and OWASP Top 10 classifications.
- **Severity Ranking:** Critical, High, Medium, Low, and Informational tiers with distinct cyber color coding.
- **Contained Modal Forms:** Modal forms bounded strictly inside the device viewport with dual segmented tabs (`Overview & Remediation` vs `PoC & Technical Impact`) to prevent full-page breakout.

### 4. Cryptographic Evidence Management
- **Multi-Format Ingestion:** Proof-of-concept captures (`PNG`, `JPEG`, `WEBP`) and technical documentation (`PDF`, `TXT`, `LOG`).
- **Cryptographic Hashing:** Computes real streaming SHA-256 hashes immediately upon upload to guarantee chain of custody.
- **Forensic File Shredding:** Secure file deletion overwriting storage sectors with zeros prior to physical unlinking.

### 5. Professional Assessment Reporting Engine
- **10-Section Deliverable:** Generates complete audit reports with:
  1. Executive Summary & Assessment Metadata
  2. Engagement Scope & Objectives
  3. Industry Methodology (OWASP WSTG v4.2, NIST SP 800-115, PTES)
  4. Tool Observations & Passive Reconnaissance
  5. Target Asset Inventory
  6. Vulnerability Findings Summary & CVSS Matrix
  7. Detailed Technical Finding Breakdown (PoC & Business Impact)
  8. Cryptographic Evidence Inventory (SHA-256 Hashes)
  9. Remediation Roadmap & Strategic Recommendations
  10. Assessment Conclusion & Certification
- **Multi-Format Export:** Live interactive in-app report preview, downloadable monospace `TXT` report, and vector `PDF` export.

---

## Dual-Platform Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                          CLIENT PRESENTATION                           │
│  Android Native (Jetpack Compose / M3)   │   Web Simulator (HTML5/CSS3)│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                           SECURITY & INGRESS                           │
│    JWT Token Authentication    │    Mandatory Legal Scope Gate (ROE)   │
│    SSRF & Metadata Protector   │    Input & CVSS Boundary Validators   │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                          CYBERSECURITY ENGINE                          │
│   Passive Reconnaissance  │  Web Security Audit  │  Security Utilities │
│   Assessment Workspace    │  Evidence Storage    │  Report Synthesizer │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                           PERSISTENT STORAGE                           │
│   Room v6 SQLite (Android)    │    better-sqlite3 (PRAGMA foreign_keys)│
└────────────────────────────────────────────────────────────────────────┘
```

---

## Modular Cybersecurity Toolkit (14 Real Tools)

All tools use native algorithms and legitimate network queries with zero simulated outputs:

### 1. Passive Reconnaissance
- **DNS Lookup:** Resolves live DNS records (`A`, `AAAA`, `MX`, `NS`, `TXT`, `SOA`) with TTL and response flags using Node.js native `dns.promises`.
- **Reverse DNS (PTR):** Queries in-addr.arpa pointer records for IP hostnames.
- **Domain Information (RDAP):** Live queries to legitimate public RDAP registries; safely handles redacted WHOIS data and privacy protection.
- **IP Information & Scope Classifier:** Validates IPv4/IPv6, classifies RFC 1918/4193 private ranges, and extracts regional registry allocation data.
- **IPv4 Subnet & CIDR Calculator:** Computes network ID, broadcast IP, netmask, usable host range, and total host capacity for any CIDR prefix.

### 2. Web Security Inspection
- **Defensive HTTP Security Header Analyzer:** Evaluates response headers against industry baselines:
  - `Strict-Transport-Security` (HSTS)
  - `Content-Security-Policy` (CSP)
  - `X-Frame-Options` (Clickjacking protection)
  - `X-Content-Type-Options` (MIME-sniffing defense)
  - `Referrer-Policy` & `Permissions-Policy`
  - Detects server header disclosures (`Server`, `X-Powered-By`).
- **TLS Certificate Inspector:** Inspects live X.509 certificates (Issuer, Validity Window, Subject Alternative Names, RFC 6125 hostname matching, and cipher suites).
- **HTTP Redirect Tracer:** Follows HTTP hop chains up to 10 redirects, identifying open redirect parameters and SSL downgrade jumps.
- **SSRF & Metadata Guardrails:** Built-in protection intercepting probes to loopback (`127.0.0.1`), link-local metadata (`169.254.169.254`), and unsafe URL protocols (`file://`, `gopher://`).

### 3. Security Utilities
- **Cryptographic Hash Generator:** Computes MD5, SHA-1, SHA-256, SHA-384, SHA-512, and HMAC-SHA256 from text or uploaded binary files.
- **Hash Type Identifier:** Regular expression and bit-length classifier matching 15+ cryptographic hash families.
- **JWT Decoder:** Parses tokens client-side into JSON header and payload claims; explains `exp`/`iat`/`nbf` timestamps with explicit disclaimer that decoding does not verify cryptographic signatures.
- **URL Structure & Parameter Analyzer:** Parses RFC 3986 URL components and flags suspicious characteristics.
- **Password Strength Estimator:** Evaluates password complexity, Shannon entropy, character diversity, and detects dictionary patterns locally without network transmission.
- **JSON Formatter & Validator:** Formats, minifies, and detects JSON syntax errors with line/column pointer indicators.
- **Multi-Format Converter:** Lossless encoding and decoding between Base64, Hexadecimal, URL Percent-Encoding, and UTF-8 text.

### Slideable Left-to-Right & Right-to-Left Options
The tool selector bars in **Utilities**, **Web Security**, and **Passive Recon** are horizontally slideable:
- **Touch & Mouse Drag:** Smooth momentum panning left and right with an 8px drag deadzone.
- **Navigation Arrows:** One-click `‹` and `›` arrow buttons to pan options by 140–160px.
- **Auto-Center:** Tapping any tool tab auto-centers the active option within the viewport.

---

## Web Application & API Server Guide

The Web Platform and REST API run locally via Node.js and Express.

### Exact Startup Instructions

```powershell
# 1. Navigate to backend directory
cd backend

# 2. Install dependencies (Node.js 18+ required)
npm install

# 3. Start the production server daemon
node src/server.js
```

- **Web Simulator & Console:** `http://localhost:5000`
- **RESTful API Base URL:** `http://localhost:5000/api/v1`

### Default Authorized Operator Credentials

| Field | Value | Permissions |
|---|---|---|
| **Username** | `sh@dow_sec` | Full Assessment Access |
| **Password** | `Password123!Safe` | PBKDF2 / SHA-256 Authentication |
| **Role** | `ADMIN` | Certified Security Operator |

---

## Database Architecture & Relational Schema

Powered by **Room v6 SQLite** on Android and **better-sqlite3** on the Web server with relational integrity enforced (`PRAGMA foreign_keys = ON;`):

```
   ┌───────────────────────┐
   │      assessments      │◄──────────┐
   │───────────────────────│           │
   │ id (PK)               │           │
   │ name, client, status  │           │
   │ is_authorized (ROE)   │           │
   └───────────┬───────────┘           │
               │ (1:N CASCADE)         │ (1:N CASCADE)
       ┌───────┴───────┬───────────────┴───────┬───────────────┐
       ▼               ▼                       ▼               ▼
┌──────────────┐┌──────────────┐       ┌──────────────┐┌────────────────┐
│   targets    ││ recon_observ │       │   findings   ││ report_history │
│──────────────││──────────────│       │──────────────││────────────────│
│ id (PK)      ││ id (PK)      │       │ id (PK)      ││ id (PK)        │
│ assessment_id││ assessment_id│       │ assessment_id││ assessment_id  │
│ scope_status ││ tool_name    │       │ cvss, sever. ││ format, sha256 │
└──────────────┘└──────┬───────┘       └──────┬───────┘└────────────────┘
                       │                      │
                       └─(Convert to Finding)─┤
                                              │ (1:N CASCADE)
                                       ┌──────┴───────┐
                                       ▼              ▼
                                ┌──────────────┐┌──────────────┐
                                │   evidence   ││activity_logs │
                                │──────────────││──────────────│
                                │ id (PK)      ││ id (PK)      │
                                │ finding_id   ││ assessment_id│
                                │ sha256_hash  ││ event_type   │
                                └──────────────┘└──────────────┘
```

---

## Security, Privacy & Data Protection

| Security Vector | Implementation Detail |
|---|---|
| **Operator Authentication** | PBKDF2 with HMAC-SHA256, 12,000 iterations, 16-byte random salt, and JWT token issuance. |
| **Scope Consent Gate (ROE)** | Hard requirement for operators to certify authorization before running checks or saving data (`403 Forbidden` if missing). |
| **SSRF Defense** | Intercepts requests to loopback addresses (`127.0.0.1`), link-local IPs (`169.254.169.254`), and non-HTTP protocols. |
| **Evidence Integrity** | Streaming SHA-256 checksum calculated on ingest; files sandboxed in isolated storage. |
| **Forensic Deletion** | Zero-byte overwrite before unlinking files from disk to prevent forensic carving. |
| **Brute-Force Lockout** | Enforces exponential backoff and lockouts after consecutive failed authentication attempts. |
| **Modal Containment** | Bounded modals inside `#device-frame` with `max-height: min(80%, 550px)` to prevent full-page breakout. |

---

## User Guide & Operational Workflows

### Workflow 1: Running a Tool & Saving an Observation
1. Navigate to **Passive Recon** or **Web Security**.
2. Enter the authorized target (e.g., `example.com`), ensure the authorization checkbox is marked, and click **Audit Headers** or **Resolve DNS**.
3. Review response headers, missing defenses, or DNS records.
4. Click **Save Observation** -> Observation is stored in the active assessment database.

### Workflow 2: Converting an Observation into a Vulnerability Finding
1. Navigate to **Workspace** -> **Observations**.
2. Locate the observation and click **Convert to Finding**.
3. The finding modal opens pre-filled with the affected asset and observation notes.
4. Set the Severity (e.g., `MEDIUM`), verify the CVSS score (`5.3`), and enter remediation advice.
5. Click **Record Finding** -> Converted into a formal finding with status `OPEN`.

### Workflow 3: Attaching Cryptographic Evidence
1. Navigate to **Findings** -> Open the finding card -> Click **Add Evidence**.
2. Select a proof-of-concept capture or log file.
3. RedOps automatically imports the file, computes its SHA-256 hash, and links it to the finding.

### Workflow 4: Generating Professional Assessment Reports
1. Navigate to **Workspace** -> Click **Preview Full Report**.
2. Review the synthesized 10-section deliverable in the interactive viewer.
3. Click **Download TXT** for plaintext distribution or **Print PDF** for vector export.

---

## Project Structure

```
RedOps Mobile/
├── android/                                   # Native Android Project
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/redops/mobile/
│   │   │   │   │   ├── data/                  # Room DAOs, Entities & Repositories
│   │   │   │   │   ├── di/                    # AppContainer Dependency Injection
│   │   │   │   │   ├── domain/model/          # Domain Models & Business Logic
│   │   │   │   │   ├── presentation/          # Jetpack Compose Screens & ViewModels
│   │   │   │   │   ├── security/              # SecurityManager PBKDF2 & Crypto
│   │   │   │   │   ├── ui/theme/              # Material 3 Cyberpunk Dark Theme
│   │   │   │   │   └── utils/                 # PDF Engine, Checksums, File Shredder
│   │   │   │   └── AndroidManifest.xml
│   │   │   ├── test/                          # 67 JVM Unit Tests
│   │   │   └── androidTest/                   # 5 On-Device Instrumentation Tests
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   └── gradlew.bat
└── backend/                                   # Web Cybersecurity Toolkit & REST Server
    ├── src/
    │   ├── database/                          # SQLite (better-sqlite3) Schema & Seeder
    │   ├── routes/                            # RESTful Endpoints (/api/v1) & Static Serve
    │   ├── tools/                             # 14 Real Native Cybersecurity Tools
    │   │   ├── cryptoTool.js                  # Hashes, HMAC, Identifier, Converters
    │   │   ├── dnsTool.js                     # DNS Resolver, RDAP & IP Geolocation
    │   │   ├── subnetTool.js                  # IPv4 Subnetting & CIDR Math
    │   │   ├── webSecurityTool.js             # HTTP Security Headers, TLS Inspector & Tracer
    │   │   └── ssrfProtector.js               # SSRF, RFC 1918 & Cloud Metadata Filtering
    │   └── server.js                          # Express Server Daemon Entrypoint
    ├── public/                                # Vanilla ES6+ Cyberpunk UI & PWA Simulator
    │   ├── index.html                         # Single-Page Modular Dashboard Interface
    │   ├── styles.css                         # Cyber Dark Theme (Dual Phone & Desktop Modes)
    │   ├── app.js                             # Reactive State, Client Security & Tool Callers
    │   └── manifest.json                      # PWA Installable Metadata
    └── tests/                                 # 100% Automated Test Suites
        ├── qa_release_suite.js                # Master 6-Phase QA Verification Suite
        ├── verify_all.js                      # 10/10 Master Relational & Integrity Suite
        ├── assessment_integration.test.js     # End-to-End Workflow Verification
        ├── info_gathering.test.js             # Passive Recon, RDAP & SSRF Suite
        ├── security_utilities.test.js         # Math, Crypto & Formatting Unit Suite
        └── verify_toolkit_api.js              # 12/12 REST Endpoints Verification
```

---

## Android Build, Installation & Release

### Prerequisites
- JDK 17 or JDK 21 (Eclipse Adoptium recommended).
- Android SDK with Platform 34 and Build-Tools 34.0.0.
- USB Debugging enabled on physical device OR an active Android Emulator.

### 1. Build & Install Debug APK
```powershell
Set-Location -Path "android"
.\gradlew.bat assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
```
**Artifact:** `android/app/build/outputs/apk/debug/app-debug.apk` (~18.98 MB)

### 2. Build Release APK
```powershell
Set-Location -Path "android"
.\gradlew.bat assembleRelease
```
**Artifact:** `android/app/build/outputs/apk/release/app-release-unsigned.apk` (~12.59 MB)

---

## Test Suite Verification (157 Tests)

The application is verified using automated test suites across both the Android application and the Web/API Cybersecurity Toolkit backend:

### Automated Test Matrix

| Test Suite | Module / Scope | Tests Executed | Status |
|---|---|---|---|
| **Master QA Release Suite** | `backend/tests/qa_release_suite.js` | 6 full phases / 35 checks | **100% PASSED** |
| **Full Platform Verification** | `backend/tests/verify_all.js` | 10 verification suites | **100% PASSED** |
| **Assessment Integration Suite** | `backend/tests/assessment_integration.test.js` | 8 lifecycle workflow tests | **100% PASSED** |
| **Info Gathering & SSRF Suite** | `backend/tests/info_gathering.test.js` | 13 DNS/RDAP/IP tests | **100% PASSED** |
| **Security Utilities Suite** | `backend/tests/security_utilities.test.js` | 7 native algorithm suites | **100% PASSED** |
| **REST Toolkit Endpoints Suite** | `backend/tests/verify_toolkit_api.js` | 12 REST endpoint tests | **100% PASSED** |
| **JVM Unit Tests (Android)** | `android/app/src/test/` | 67 tests | **100% PASSED** |
| **On-Device Instrumentation** | `android/app/src/androidTest/` | 5 tests | **100% PASSED** |
| **Total Automated Tests** | Across all platforms | **157 verified test cases** | **0 Failures / 0 Regressions** |

### Execution Commands

```powershell
# 1. Run Complete QA & Release Verification Suite
node backend/tests/qa_release_suite.js

# 2. Run Full Platform Relational & Integrity Suite
node backend/tests/verify_all.js

# 3. Run Native Unit Test Suites
node --test backend/tests/assessment_integration.test.js
node --test backend/tests/info_gathering.test.js
node --test backend/tests/security_utilities.test.js

# 4. Run Toolkit REST API Endpoint Verification
node backend/tests/verify_toolkit_api.js

# 5. Run Android Unit Tests
cd android ; .\gradlew.bat testDebugUnitTest
```

---

## Known Limitations & Roadmap

1. **Native SQLCipher Encryption:** File-Based Encryption (FBE) and encrypted AES-256-GCM password backups are integrated; full database page encryption via SQLCipher native binaries is planned for v1.1.
2. **Multi-Operator Collaboration:** RedOps Mobile is currently an offline-first single-operator system. Future iterations may introduce end-to-end encrypted peer-to-peer sync over local Wi-Fi.
3. **Custom Report Branding:** Support for importing custom organizational letterheads and company logos into the vector PDF engine is scheduled for v1.1.

---

## Ethical Governance & Legal Boundaries

RedOps Mobile is intentionally engineered and bounded as an **administrative security documentation, vulnerability tracking, and assessment management system**. The platform strictly enforces ethical cybersecurity boundaries aligned with academic submission criteria and professional penetration testing standards (OWASP, NIST SP 800-115, PTES):

1. **Architectural Guardrails & Prohibitions:**
   - **Zero Offensive Exploitation:** Does not execute exploits, shell injection payloads, buffer overflows, or remote code execution.
   - **Zero Credential Theft:** No credential dumping, memory scraping, brute-force cracking, or unauthorized password harvesting logic exists.
   - **Zero Denial-of-Service:** Does not generate packet floods, ping sweeps, or resource starvation attacks.
   - **Zero Covert Surveillance:** Contains no keyloggers, covert microphone recorders, background Trojan droppers, or persistence implants.
   - **Passive Enumeration Only:** All wireless discovery relies exclusively on non-invasive Android platform standard `WifiManager` scan results. Packet sniffing, deauthentication frames, and handshake interception are strictly absent.

2. **Mandatory Ethical Scope Authorization Gate:**
   - **Pre-Engagement Consent Checkpoint:** Every engagement must record an explicit, timestamped legal authorization declaration (`is_authorized: true`) confirming written permission from the asset owner.
   - **Server and Client Enforcement:** The backend API returns `403 Forbidden` (`Scope Authorization Failure`) if any client attempts to create an assessment without certifying authorized permission.

3. **Cryptographic Chain of Custody & Evidence Integrity:**
   - **Forensic Verification:** All evidence files are timestamped and cryptographically fingerprinted using streaming SHA-256 checksums upon import.
   - **Secure File Destruction:** Evidence deletions overwrite target sectors with zeros prior to physical unlinking to prevent forensic carving from decommissioned storage.
