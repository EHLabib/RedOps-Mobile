# RedOps Mobile — System Architecture & Technical Design

## 1. Executive Architectural Overview

**RedOps Mobile** ("Authorized Security Assessment Platform") is an enterprise-grade mobile application and supporting REST API backend designed for managing authorized penetration testing, vulnerability assessments, and defensive audits.

The application adheres to **Clean Architecture** principles and the **Model-View-ViewModel (MVVM)** pattern on Android, backed by an offline-first data model utilizing **Jetpack Room** as the Single Source of Truth (SSOT) and **WorkManager** for asynchronous data synchronization with a Node.js/Express REST backend.

---

## 2. High-Level System Architecture

The overall system comprises three primary tiers:
1. **Android Client Tier**: Native Kotlin application utilizing Jetpack Compose, Material 3, Room, Retrofit, CameraX, and hardware sensors.
2. **Gateway & API Tier**: Node.js / Express REST API serving authenticated endpoints over TLS with JWT validation, role-based authorization, and rate limiting.
3. **Storage Tier**: Dual-layer architecture:
   - Local: SQLite via Android Jetpack Room for full offline autonomy.
   - Remote: PostgreSQL 15 relational database for multi-user collaboration and persistence.

```mermaid
graph TB
    subgraph MobileClient["Android Mobile Client (RedOps)"]
        UI["Jetpack Compose UI (Material 3 Dark)"]
        VM["ViewModels (StateFlow / UDF)"]
        UC["Domain Use Cases & Validators"]
        Repo["Repository Layer (Offline-First)"]
        RoomDB[("Room Local DB (SQLite SSOT)")]
        DataStore["Encrypted DataStore (JWT / Session)"]
        Sensors["Hardware Services (CameraX / WifiManager / GPS)"]
        SyncMgr["Sync Engine (WorkManager)"]

        UI --> VM
        VM --> UC
        UC --> Repo
        Repo --> RoomDB
        Repo --> DataStore
        Repo --> SyncMgr
        Sensors --> Repo
    end

    subgraph Transport["Network & Security Boundary"]
        HTTPS["HTTPS (TLS 1.3/1.2) + JWT Bearer"]
    end

    subgraph BackendTier["Node.js / Express REST Backend"]
        MW["Security Middleware (Helmet / RateLimit / JWT RBAC)"]
        Router["Express REST Routers"]
        Controllers["Module Controllers"]
        Services["Business Services (Auth, Findings, Report Gen)"]
        DBPool[("PostgreSQL 15 Relational DB")]

        MW --> Router
        Router --> Controllers
        Controllers --> Services
        Services --> DBPool
    end

    SyncMgr <-->|Encrypted REST Sync| HTTPS
    HTTPS <--> MW
```

---

## 3. Mobile Client Architecture (Clean Architecture + MVVM)

The Android application implements a strict four-layer Clean Architecture model:

```mermaid
graph TD
    subgraph PresentationLayer["Presentation Layer (app/ui)"]
        Screens["Compose Screens (Dashboard, Scope, Recon, Findings, Reports)"]
        Components["M3 Custom Components (SeverityBadges, TargetCards, StatWidgets)"]
        Nav["Navigation Graph (Compose Navigation)"]
        ViewModels["ViewModels (StateFlow<UiState>, UiEvent)"]
        
        Screens --> ViewModels
        Components --> Screens
        Nav --> Screens
    end

    subgraph DomainLayer["Domain Layer (app/domain)"]
        UseCases["Use Cases (e.g., ValidateScopeUseCase, LogPassiveWifiUseCase, CreateFindingUseCase)"]
        DomainModels["Pure Domain Entities (Assessment, Target, Finding, Evidence)"]
        RepoInterfaces["Repository Interfaces (Contracts)"]
        
        UseCases --> DomainModels
        UseCases --> RepoInterfaces
    end

    subgraph DataLayer["Data Layer (app/data)"]
        RepoImpl["Repository Implementations"]
        LocalData["Local Data Source (Room Database & DAOs)"]
        RemoteData["Remote Data Source (Retrofit API Service & DTOs)"]
        DataStoreSource["Jetpack DataStore (Token & Preferences)"]
        Mappers["Model Mappers (DTO <-> Entity <-> Domain)"]
        
        RepoImpl --> LocalData
        RepoImpl --> RemoteData
        RepoImpl --> DataStoreSource
        LocalData --> Mappers
        RemoteData --> Mappers
    end

    subgraph ServicesLayer["Hardware & Background Services (app/services)"]
        WifiSvc["Passive Wireless Enumerator (WifiManager BroadcastReceiver)"]
        LocationSvc["Location Service (FusedLocationProviderClient)"]
        CameraSvc["CameraX Service (ImageCapture & Storage)"]
        NotifSvc["Notification Manager & WorkManager Sync Workers"]
    end

    ViewModels --> UseCases
    RepoInterfaces <|.. RepoImpl
    ServicesLayer --> RepoImpl
```

### Layer Responsibilities

1. **Presentation Layer (`com.redops.mobile.ui`)**:
   - Built entirely in **Jetpack Compose** with Material 3.
   - Enforces a cybernetic dark-mode color system with high-contrast text and neon accents (matrix green `#00E676`, cyan `#00E5FF`, warning amber `#FFAB00`, critical red `#FF5252`).
   - Follows Uni-directional Data Flow (UDF): ViewModels expose immutable `StateFlow<T>` states and receive user `UiIntent` events.
2. **Domain Layer (`com.redops.mobile.domain`)**:
   - Zero Android framework dependencies (pure Kotlin).
   - Contains business rules, such as `ValidateScopeUseCase` which prevents adding targets or observations without explicit signed scope authorization.
3. **Data Layer (`com.redops.mobile.data`)**:
   - Implements repository interfaces defined in the domain layer.
   - Orchestrates offline-first data caching: reads always originate from Room DAOs; remote updates update Room, triggering reactive Flow emissions to ViewModels.
4. **Services Layer (`com.redops.mobile.services`)**:
   - Wraps platform-specific Android hardware APIs (CameraX, WifiManager, FusedLocationProviderClient) in coroutine-friendly abstractions.

---

## 4. Use Case Specifications

```mermaid
flowchart LR
    Tester(("Security Tester / Student"))
    Admin(("Administrator"))

    subgraph RedOpsSystem["RedOps Mobile Platform"]
        UC1["Authenticate (Register / Login)"]
        UC2["Create Assessment & Sign Scope Authorization"]
        UC3["Manage Authorized Targets (In-Scope vs Out-of-Scope)"]
        UC4["Record Reconnaissance Observations"]
        UC5["Passive Wireless 802.11 Enumeration"]
        UC6["Log Web / API Vulnerability Finding"]
        UC7["Attach Photo / Image Evidence (CameraX)"]
        UC8["Tag GPS Coordinates to Observation"]
        UC9["Generate Executive / Technical PDF Report"]
        UC10["Synchronize Offline Data to Server"]
        UC11["Manage System Users & Global Logs"]
    end

    Tester --> UC1
    Tester --> UC2
    Tester --> UC3
    Tester --> UC4
    Tester --> UC5
    Tester --> UC6
    Tester --> UC7
    Tester --> UC8
    Tester --> UC9
    Tester --> UC10

    Admin --> UC1
    Admin --> UC2
    Admin --> UC3
    Admin --> UC6
    Admin --> UC9
    Admin --> UC11
```

---

## 5. Application Engagement Workflow

The following state machine diagrams the mandatory progression of an authorized security assessment within RedOps Mobile:

```mermaid
stateDiagram-v2
    [*] --> Unauthenticated
    Unauthenticated --> Authenticated: Login / Register (JWT Issued)
    
    Authenticated --> Dashboard: View Engagement Overview
    
    Dashboard --> CreateAssessment: New Engagement
    state CreateAssessment {
        [*] --> InputDetails: Name, Client, Type, Dates
        InputDetails --> ScopeReview: Define In-Scope Boundaries
        ScopeReview --> AuthorizationCheck: Explicit Signature Checkpoint
        AuthorizationCheck --> ActiveAssessment: Confirmed [✓ Authorized]
        AuthorizationCheck --> LockedAssessment: Unconfirmed [❌ Access Denied]
    }

    ActiveAssessment --> ExecutionHub: Engagement Unlocked
    
    state ExecutionHub {
        direction LR
        Targets: Target Management
        Recon: Recon Notebook
        Wireless: Passive WiFi Audit
        WebAPI: Web / API Test Notes
        Findings: Vulnerability Findings
        Evidence: CameraX Evidence
    }

    ExecutionHub --> RiskScoring: Aggregate CVSS / Severity Matrix
    RiskScoring --> ReportGeneration: Compile Sections (1 to 11)
    ReportGeneration --> PDFExport: Export & Share PDF
    PDFExport --> [*]
```

---

## 6. Offline-First & Data Synchronization Architecture

To support field engagements in high-security air-gapped zones or basements with zero cellular coverage, RedOps Mobile operates fully offline.

```mermaid
sequenceDiagram
    autonumber
    actor Tester as Security Tester
    participant UI as Compose UI
    participant VM as ViewModel
    participant Repo as AssessmentRepository
    participant Room as Room DB (SSOT)
    participant Worker as SyncWorker (WorkManager)
    participant API as Express REST API
    participant PG as PostgreSQL DB

    Tester->>UI: Add Target / Finding
    UI->>VM: Submit form
    VM->>Repo: saveFinding(finding)
    Note over Repo,Room: Mark finding as sync_pending = true
    Repo->>Room: insert(findingEntity)
    Room-->>VM: Flow emits updated list
    VM-->>UI: UI renders new finding instantly (Optimistic UI)

    alt Device is Online
        Worker->>Room: Query where sync_pending == true
        Room-->>Worker: List of dirty entities
        Worker->>API: POST /api/findings (with Bearer JWT)
        API->>PG: INSERT into findings
        PG-->>API: 201 Created (confirmed server ID & timestamp)
        API-->>Worker: HTTP 201 Created
        Worker->>Room: Update entity (sync_pending = false, server_id, synced_at)
        Room-->>VM: Flow emits synchronized status badge (ONLINE)
    else Device is Offline
        Worker-->>Worker: Retry scheduled via exponential backoff
        VM-->>UI: Status badge shows (OFFLINE - Pending Sync)
    end
```

### Sync State Machine
Entities in Room maintain three synchronization metadata columns:
- `sync_pending: Boolean`: `true` when created or modified locally before reaching the server.
- `server_id: String?`: UUID assigned by the PostgreSQL backend upon successful sync.
- `last_modified: Long`: Epoch timestamp used for Last-Write-Wins (LWW) conflict resolution.

---

## 7. Passive Wireless Enumeration Architecture

The wireless subsystem is strictly designed around **passive 802.11 beacon frame discovery** via standard Android operating system APIs:

```mermaid
graph TD
    subgraph AndroidOS["Android OS Framework"]
        WifiMgr["android.net.wifi.WifiManager"]
        ScanResults["List<ScanResult> (Open Airwaves)"]
        WifiReceiver["BroadcastReceiver (SCAN_RESULTS_AVAILABLE_ACTION)"]
    end

    subgraph RedOpsService["RedOps Mobile Wireless Subsystem"]
        Permissions["Permission Gatekeeper (FINE_LOCATION & NEARBY_WIFI_DEVICES)"]
        Scanner["PassiveWifiScannerImpl"]
        Parser["WifiSecurityParser (SSID, BSSID, RSSI, Frequency, Capabilities)"]
        Filter["Filter & Deduplication Engine"]
        Storage["WirelessObservationRepository"]
    end

    Permissions -->|Granted| Scanner
    Scanner -->|startScan() request| WifiMgr
    WifiMgr -->|Broadcast| WifiReceiver
    WifiReceiver -->|getScanResults()| ScanResults
    ScanResults --> Parser
    Parser --> Filter
    Filter --> Storage
```

### Safety and Compliance Guardrails:
1. **No Raw Sockets or Monitor Mode**: Operates within non-rooted Android sandbox.
2. **No Deauthentication Frames**: No transmission of 802.11 management frames.
3. **Strictly Read-Only**: Extracts metadata only (SSID, BSSID, Signal dBm, Bandwidth, WPA2/WPA3 capabilities).

---

## 8. Backend REST API Architecture

The backend is structured as a modular Express.js application designed to scale:

```mermaid
graph LR
    subgraph Ingress["Ingress & Middleware Stack"]
        Req["HTTP Request"] --> Helmet["Helmet (Security Headers)"]
        Helmet --> CORS["CORS Handler"]
        CORS --> RateLimit["Express Rate Limiter"]
        RateLimit --> BodyParse["Body Parser & JSON Validator"]
        BodyParse --> AuthMW["JWT Authentication & RBAC Middleware"]
    end

    subgraph CoreRouters["Express Routers"]
        AuthMW --> AuthRouter["/api/auth"]
        AuthMW --> AssessRouter["/api/assessments"]
        AuthMW --> TargetRouter["/api/targets"]
        AuthMW --> ReconRouter["/api/recon"]
        AuthMW --> WifiRouter["/api/wireless"]
        AuthMW --> FindingRouter["/api/findings"]
        AuthMW --> EvidenceRouter["/api/evidence"]
        AuthMW --> ReportRouter["/api/reports"]
    end

    subgraph ServiceLayer["Business Logic Layer"]
        AuthService["AuthService (bcrypt + JWT)"]
        AssessService["AssessmentService"]
        FindingService["FindingService (CVSS calc)"]
        ReportService["ReportService (PDF generation)"]
    end

    subgraph DataStore["Persistence"]
        Postgres[("PostgreSQL 15 Pool")]
    end

    AuthRouter --> AuthService
    AssessRouter --> AssessService
    FindingRouter --> FindingService
    ReportRouter --> ReportService

    AuthService --> Postgres
    AssessService --> Postgres
    FindingService --> Postgres
    ReportService --> Postgres
```

---

## 9. Modular Directory Structure

```
RedOps-Mobile/
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/redops/mobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── RedOpsApplication.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── RedOpsDatabase.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   └── entity/
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── RedOpsApiService.kt
│   │   │   │   │   │   ├── AuthInterceptor.kt
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── preferences/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── usecase/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   ├── theme/
│   │   │   │   │   ├── components/
│   │   │   │   │   └── screens/
│   │   │   │   ├── services/
│   │   │   │   │   ├── wireless/
│   │   │   │   │   ├── location/
│   │   │   │   │   ├── camera/
│   │   │   │   │   └── sync/
│   │   │   │   └── utils/
│   │   │   └── res/
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── backend/
│   ├── src/
│   │   ├── server.js
│   │   ├── config/
│   │   ├── controllers/
│   │   ├── middleware/
│   │   ├── models/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── database/
│   │   └── utils/
│   ├── tests/
│   ├── package.json
│   └── Dockerfile
├── database/
│   ├── schema.sql
│   └── seeds/
├── docs/
│   ├── architecture.md
│   ├── database.md
│   ├── api.md
│   ├── security.md
│   ├── wireless-enumeration.md
│   ├── testing.md
│   └── deployment.md
├── docker-compose.yml
└── README.md
```

This concludes the architectural specifications for **Phase 1**.
