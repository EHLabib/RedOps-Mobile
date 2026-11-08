-- ==============================================================================
-- RedOps Mobile: Database Schema (PostgreSQL 15)
-- "Authorized Security Assessment Platform"
-- ==============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------------------------
-- 1. Users Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(150) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 2. Assessments Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS assessments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    client VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    assessment_type VARCHAR(50) NOT NULL CHECK (
        assessment_type IN ('Web Application', 'API', 'Network', 'Wireless', 'Internal', 'External', 'Other')
    ),
    is_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    scope_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 3. Targets Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS targets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    domain VARCHAR(255),
    ip_address VARCHAR(45),
    hostname VARCHAR(255),
    url TEXT,
    description TEXT,
    scope_status VARCHAR(20) NOT NULL DEFAULT 'IN_SCOPE' CHECK (scope_status IN ('IN_SCOPE', 'OUT_OF_SCOPE')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 4. Reconnaissance Observations Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recon_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    observation_type VARCHAR(50) NOT NULL CHECK (
        observation_type IN ('DOMAIN', 'SUBDOMAIN', 'IP', 'PORT', 'TECH', 'SERVER', 'FRAMEWORK', 'ENDPOINT', 'DNS')
    ),
    value TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 5. Network Enumeration Observations Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS network_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    host VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    port INTEGER NOT NULL CHECK (port >= 1 AND port <= 65535),
    protocol VARCHAR(10) NOT NULL DEFAULT 'TCP' CHECK (protocol IN ('TCP', 'UDP')),
    service VARCHAR(100),
    version VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 6. Wireless Observations Table (Passive Only)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wireless_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    ssid VARCHAR(100),
    bssid VARCHAR(50) NOT NULL,
    frequency INTEGER NOT NULL,
    channel INTEGER,
    signal_dbm INTEGER NOT NULL,
    security_capability VARCHAR(100) NOT NULL,
    wifi_standard VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    notes TEXT,
    observed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 7. Web / API Assessment Test Notes Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS web_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    test_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (
        category IN ('Authentication', 'Authorization', 'Input Validation', 'Session Management', 'Security Headers', 'API Security', 'Access Control', 'Configuration', 'Other')
    ),
    target_url TEXT NOT NULL,
    http_method VARCHAR(10) DEFAULT 'GET' CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD')),
    observation TEXT,
    result VARCHAR(20) NOT NULL DEFAULT 'INCONCLUSIVE' CHECK (result IN ('PASS', 'FAIL', 'INCONCLUSIVE')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 8. Findings Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS findings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    custom_id VARCHAR(50) NOT NULL, -- e.g. F-001
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    affected_asset VARCHAR(255) NOT NULL,
    impact TEXT,
    remediation TEXT,
    reference_links TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (
        status IN ('OPEN', 'CONFIRMED', 'REMEDIATED', 'ACCEPTED', 'FALSE_POSITIVE')
    ),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 9. Evidence Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    finding_id UUID NOT NULL REFERENCES findings(id) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    file_type VARCHAR(50) NOT NULL DEFAULT 'IMAGE' CHECK (file_type IN ('IMAGE', 'SCREENSHOT', 'TEXT', 'LOG')),
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 10. Reports Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    executive_summary TEXT,
    scope_summary TEXT,
    methodology TEXT,
    risk_summary TEXT,
    recommendations TEXT,
    conclusion TEXT,
    pdf_path TEXT,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 11. Tasks Table (Deadlines / Reminders)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_assessments_user ON assessments(user_id);
CREATE INDEX IF NOT EXISTS idx_targets_assessment ON targets(assessment_id);
CREATE INDEX IF NOT EXISTS idx_findings_assessment ON findings(assessment_id);
CREATE INDEX IF NOT EXISTS idx_evidence_finding ON evidence(finding_id);
CREATE INDEX IF NOT EXISTS idx_recon_assessment ON recon_observations(assessment_id);
CREATE INDEX IF NOT EXISTS idx_wireless_assessment ON wireless_observations(assessment_id);
