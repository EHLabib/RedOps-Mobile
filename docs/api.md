# RedOps Mobile — REST API Specification

All endpoints are served under `/api/v1` and communicate using `application/json` (except multipart file upload endpoints). Authentication uses standard HTTP Bearer tokens: `Authorization: Bearer <jwt_token>`.

---

## 1. Authentication Endpoints

### 1.1 Register Operator
- **POST** `/api/v1/auth/register`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "full_name": "Shadow Security",
    "username": "sh@dow_sec",
    "email": "shadow@example.com",
    "password": "Password123!Safe",
    "role": "USER"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "status": "success",
    "data": {
      "user": {
        "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
        "full_name": "Shadow Security",
        "username": "sh@dow_sec",
        "email": "shadow@example.com",
        "role": "USER",
        "created_at": "2026-09-30T23:00:00.000Z"
      },
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
  ```

### 1.2 Login Operator
- **POST** `/api/v1/auth/login`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "username": "sh@dow_sec",
    "password": "Password123!Safe"
  }
  ```
- **Response 200 OK**:
  ```json
  {
    "status": "success",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
      "username": "sh@dow_sec",
      "role": "USER"
    }
  }
  ```

---

## 2. Assessment Management

### 2.1 List Assessments
- **GET** `/api/v1/assessments`
- **Auth**: Bearer Token
- **Query Params**: `?status=ACTIVE&type=Web`
- **Response 200 OK**:
  ```json
  {
    "status": "success",
    "count": 1,
    "data": [
      {
        "id": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
        "name": "Acme Corp Web Penetration Test",
        "client": "Acme Corporation",
        "assessment_type": "Web Application",
        "is_authorized": true,
        "status": "ACTIVE",
        "start_date": "2026-10-01",
        "end_date": "2026-10-15"
      }
    ]
  }
  ```

### 2.2 Create Assessment
- **POST** `/api/v1/assessments`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "name": "Acme Corp Web Penetration Test",
    "client": "Acme Corporation",
    "description": "Annual security audit of internal API and customer portal",
    "start_date": "2026-10-01",
    "end_date": "2026-10-15",
    "assessment_type": "Web Application",
    "is_authorized": true,
    "scope_notes": "Explicit authorization confirmed via engagement letter dated 2026-09-25."
  }
  ```
- **Response 201 Created**: JSON with created assessment details.

---

## 3. Target Management

### 3.1 Add Target
- **POST** `/api/v1/assessments/:assessmentId/targets`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "domain": "portal.acme.corp",
    "ip_address": "192.168.10.50",
    "hostname": "srv-portal-01",
    "url": "https://portal.acme.corp",
    "description": "Production customer web portal",
    "scope_status": "IN_SCOPE",
    "notes": "Authorized for passive header checks and OWASP Top 10 manual testing"
  }
  ```
- **Response 201 Created**

---

## 4. Findings & Evidence Endpoints

### 4.1 Log Finding
- **POST** `/api/v1/assessments/:assessmentId/findings`
- **Auth**: Bearer Token
- **Request Body**:
  ```json
  {
    "title": "Missing HTTP Strict-Transport-Security (HSTS) Header",
    "severity": "MEDIUM",
    "affected_asset": "https://portal.acme.corp",
    "description": "The web application fails to enforce transport layer security via HSTS headers.",
    "impact": "Vulnerable to SSL stripping and man-in-the-middle attacks.",
    "remediation": "Configure web server to return Strict-Transport-Security: max-age=31536000; includeSubDomains",
    "status": "OPEN"
  }
  ```

### 4.2 Upload Evidence File
- **POST** `/api/v1/findings/:findingId/evidence`
- **Auth**: Bearer Token
- **Content-Type**: `multipart/form-data`
- **Fields**:
  - `file`: (Binary image/screenshot file)
  - `description`: "Captured response headers missing HSTS directive"
  - `latitude`: 23.8103
  - `longitude`: 90.4125
