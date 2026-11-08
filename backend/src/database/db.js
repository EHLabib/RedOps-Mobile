const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');
const fs = require('node:fs');
const bcrypt = require('bcryptjs');

const dbPath = path.join(__dirname, '..', '..', 'redops.sqlite');
const db = new DatabaseSync(dbPath);

// Enable foreign keys for relational consistency and WAL mode for high concurrency
db.exec('PRAGMA foreign_keys = ON;');
db.exec('PRAGMA journal_mode = WAL;');

function initDatabase() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      full_name TEXT NOT NULL,
      username TEXT UNIQUE NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'USER',
      created_at TEXT DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS assessments (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      name TEXT NOT NULL,
      client TEXT NOT NULL,
      description TEXT,
      assessment_type TEXT NOT NULL DEFAULT 'Web Application',
      start_date TEXT NOT NULL,
      end_date TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'ACTIVE',
      is_authorized INTEGER NOT NULL DEFAULT 0,
      authorized_scope TEXT,
      out_of_scope_assets TEXT,
      objectives TEXT,
      notes TEXT,
      scope_notes TEXT,
      is_demo INTEGER NOT NULL DEFAULT 0,
      created_at TEXT DEFAULT (datetime('now')),
      updated_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS targets (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      domain TEXT,
      ip_address TEXT,
      hostname TEXT,
      url TEXT,
      description TEXT,
      scope_status TEXT NOT NULL DEFAULT 'IN_SCOPE',
      notes TEXT,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS recon_observations (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      observation_type TEXT NOT NULL,
      value TEXT NOT NULL,
      notes TEXT,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS network_observations (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      host TEXT NOT NULL,
      ip_address TEXT NOT NULL,
      port INTEGER NOT NULL,
      protocol TEXT NOT NULL DEFAULT 'TCP',
      service TEXT,
      version TEXT,
      notes TEXT,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS wireless_observations (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      ssid TEXT,
      bssid TEXT NOT NULL,
      frequency INTEGER NOT NULL,
      channel INTEGER,
      signal_dbm INTEGER NOT NULL,
      security_capability TEXT NOT NULL,
      wifi_standard TEXT,
      latitude REAL,
      longitude REAL,
      notes TEXT,
      observed_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS web_tests (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      test_name TEXT NOT NULL,
      category TEXT NOT NULL,
      target_url TEXT NOT NULL,
      http_method TEXT DEFAULT 'GET',
      observation TEXT,
      result TEXT NOT NULL DEFAULT 'INCONCLUSIVE',
      notes TEXT,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS findings (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      custom_id TEXT NOT NULL,
      title TEXT NOT NULL,
      description TEXT NOT NULL,
      affected_asset TEXT NOT NULL,
      severity TEXT NOT NULL,
      cvss_score REAL DEFAULT 0.0,
      cwe_id TEXT,
      category TEXT DEFAULT 'Injection',
      technical_details TEXT,
      impact TEXT,
      remediation TEXT,
      reference_links TEXT,
      status TEXT NOT NULL DEFAULT 'OPEN',
      created_at TEXT DEFAULT (datetime('now')),
      updated_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS evidence (
      id TEXT PRIMARY KEY,
      finding_id TEXT NOT NULL,
      file_name TEXT NOT NULL,
      original_name TEXT NOT NULL,
      file_path TEXT NOT NULL,
      file_size INTEGER NOT NULL DEFAULT 0,
      mime_type TEXT NOT NULL DEFAULT 'application/octet-stream',
      sha256_hash TEXT NOT NULL,
      description TEXT,
      tags TEXT,
      latitude REAL,
      longitude REAL,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS notebook_entries (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      category TEXT NOT NULL DEFAULT 'OBSERVATION',
      tags TEXT,
      is_pinned INTEGER NOT NULL DEFAULT 0,
      is_favorite INTEGER NOT NULL DEFAULT 0,
      created_at TEXT DEFAULT (datetime('now')),
      updated_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS report_history (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      report_title TEXT NOT NULL,
      format TEXT NOT NULL DEFAULT 'PDF',
      file_path TEXT,
      file_size INTEGER DEFAULT 0,
      sha256_hash TEXT,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS activity_logs (
      id TEXT PRIMARY KEY,
      assessment_id TEXT NOT NULL,
      title TEXT NOT NULL,
      type TEXT NOT NULL,
      badge_class TEXT NOT NULL,
      icon TEXT NOT NULL,
      color TEXT NOT NULL,
      target_section TEXT NOT NULL,
      target_keyword TEXT,
      asset TEXT,
      auth_reference TEXT,
      rules_of_engagement TEXT,
      operator_title TEXT,
      description TEXT NOT NULL,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
    );

    -- Foreign key & operational performance indexes
    CREATE INDEX IF NOT EXISTS idx_assessments_user ON assessments (user_id);
    CREATE INDEX IF NOT EXISTS idx_targets_assessment ON targets (assessment_id);
    CREATE INDEX IF NOT EXISTS idx_recon_assessment ON recon_observations (assessment_id);
    CREATE INDEX IF NOT EXISTS idx_network_assessment ON network_observations (assessment_id);
    CREATE INDEX IF NOT EXISTS idx_wireless_assessment ON wireless_observations (assessment_id);
    CREATE INDEX IF NOT EXISTS idx_findings_assessment ON findings (assessment_id);
    CREATE INDEX IF NOT EXISTS idx_evidence_finding ON evidence (finding_id);
    CREATE INDEX IF NOT EXISTS idx_notebook_assessment ON notebook_entries (assessment_id, is_pinned DESC, updated_at DESC);
    CREATE INDEX IF NOT EXISTS idx_report_history ON report_history (assessment_id, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_activity_assessment ON activity_logs (assessment_id, created_at DESC);
  `);

  // Schema migrations for backward compatibility
  ensureColumn('assessments', 'authorized_scope', 'TEXT');
  ensureColumn('assessments', 'out_of_scope_assets', 'TEXT');
  ensureColumn('assessments', 'objectives', 'TEXT');
  ensureColumn('assessments', 'notes', 'TEXT');
  ensureColumn('assessments', 'updated_at', "TEXT DEFAULT (datetime('now'))");

  ensureColumn('findings', 'cvss_score', 'REAL DEFAULT 0.0');
  ensureColumn('findings', 'cwe_id', 'TEXT');
  ensureColumn('findings', 'category', "TEXT DEFAULT 'General Vulnerability'");
  ensureColumn('findings', 'technical_details', 'TEXT');
  ensureColumn('findings', 'updated_at', "TEXT DEFAULT (datetime('now'))");

  ensureColumn('evidence', 'tags', 'TEXT');

  // Recon & Tool Observations enhancements
  ensureColumn('recon_observations', 'tool_name', "TEXT DEFAULT 'Manual Observation'");
  ensureColumn('recon_observations', 'tool_category', "TEXT DEFAULT 'RECONNAISSANCE'");
  ensureColumn('recon_observations', 'title', "TEXT");
  ensureColumn('recon_observations', 'source_info', "TEXT");
  ensureColumn('recon_observations', 'result_data', "TEXT");
  ensureColumn('recon_observations', 'status', "TEXT DEFAULT 'INFORMATIONAL'");
  ensureColumn('recon_observations', 'converted_finding_id', "TEXT");
  ensureColumn('recon_observations', 'updated_at', "TEXT");

  initDefaultOperator();

  // Keep sample data only in an explicitly identified development/demo seed mechanism,
  // never as the default production dataset.
  if (process.env.SEED_DEMO_DATA === 'true') {
    seedDemoData();
  }
}

function ensureColumn(tableName, colName, colDef) {
  try {
    const info = db.prepare(`PRAGMA table_info(${tableName})`).all();
    if (!info.some(col => col.name === colName)) {
      db.exec(`ALTER TABLE ${tableName} ADD COLUMN ${colName} ${colDef};`);
    }
  } catch (err) {
    // Column may already exist or table being created
  }
}

function initDefaultOperator() {
  const existingUser = db.prepare('SELECT id, username FROM users WHERE id = ?').get('usr-admin-01');
  const salt = bcrypt.genSaltSync(10);
  const passwordHash = bcrypt.hashSync('Password123!Safe', salt);

  if (!existingUser) {
    console.log('[Database] Initializing authorized administrator operator account...');
    const insertUser = db.prepare(`
      INSERT INTO users (id, full_name, username, email, password_hash, role)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    insertUser.run('usr-admin-01', 'Shadow Operator', 'sh@dow_sec', 'shadow@redops-platform.local', passwordHash, 'ADMIN');
  } else if (existingUser.username !== 'sh@dow_sec') {
    console.log('[Database] Updating administrator credentials to sh@dow_sec...');
    db.prepare(`
      UPDATE users 
      SET username = ?, full_name = ?, email = ?, password_hash = ?
      WHERE id = ?
    `).run('sh@dow_sec', 'Shadow Operator', 'shadow@redops-platform.local', passwordHash, 'usr-admin-01');
  }
}

function logActivity(entry) {
  try {
    if (!entry || !entry.assessment_id) {
      return null;
    }
    const id = entry.id || ('act-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 6));
    const stmt = db.prepare(`
      INSERT INTO activity_logs (
        id, assessment_id, title, type, badge_class, icon, color,
        target_section, target_keyword, asset, auth_reference,
        rules_of_engagement, operator_title, description, created_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
    `);

    stmt.run(
      id,
      entry.assessment_id || null,
      entry.title || '',
      entry.type || 'ACTIVITY',
      entry.badge_class || 'badge-roe-auth',
      entry.icon || '🛡️',
      entry.color || 'rgba(0, 230, 118, 0.2)',
      entry.target_section || 'assessments',
      entry.target_keyword || '',
      entry.asset || '',
      entry.auth_reference || 'ROE Verified',
      entry.rules_of_engagement || 'Strictly Non-Disruptive',
      entry.operator_title || 'Security Specialist',
      entry.description || ''
    );
    return id;
  } catch (err) {
    console.error('[Database] Failed to log activity:', err.message);
    return null;
  }
}

function resetDatabase() {
  console.log('[Database] Purging and resetting database to clean state...');
  db.exec('PRAGMA foreign_keys = OFF;');
  db.exec(`
    DROP TABLE IF EXISTS activity_logs;
    DROP TABLE IF EXISTS report_history;
    DROP TABLE IF EXISTS notebook_entries;
    DROP TABLE IF EXISTS evidence;
    DROP TABLE IF EXISTS findings;
    DROP TABLE IF EXISTS web_tests;
    DROP TABLE IF EXISTS wireless_observations;
    DROP TABLE IF EXISTS network_observations;
    DROP TABLE IF EXISTS recon_observations;
    DROP TABLE IF EXISTS targets;
    DROP TABLE IF EXISTS assessments;
    DROP TABLE IF EXISTS users;
  `);
  db.exec('PRAGMA foreign_keys = ON;');
  initDatabase();
}

function clearAssessmentData() {
  console.log('[Database] Clearing all assessment datasets, keeping operator account...');
  db.exec('PRAGMA foreign_keys = OFF;');
  db.exec(`
    DELETE FROM activity_logs;
    DELETE FROM report_history;
    DELETE FROM notebook_entries;
    DELETE FROM evidence;
    DELETE FROM findings;
    DELETE FROM web_tests;
    DELETE FROM wireless_observations;
    DELETE FROM network_observations;
    DELETE FROM recon_observations;
    DELETE FROM targets;
    DELETE FROM assessments;
  `);
  db.exec('PRAGMA foreign_keys = ON;');
}

function seedDemoData(force = false) {
  initDefaultOperator();

  const assessCheck = db.prepare('SELECT COUNT(*) as count FROM assessments').get();
  if (assessCheck.count > 0 && !force) {
    console.log('[Database] Assessments already exist. Skipping seedDemoData.');
    return;
  }

  console.log('[Database] Explicitly populating evaluation demo dataset...');

  const userId = 'usr-admin-01';
  const assessId = 'asm-acme-2026';

  if (force) {
    db.prepare('DELETE FROM assessments WHERE id = ?').run(assessId);
  }

  const insertAssess = db.prepare(`
    INSERT INTO assessments (
      id, user_id, name, client, description, assessment_type,
      start_date, end_date, status, is_authorized,
      authorized_scope, out_of_scope_assets, objectives, notes, scope_notes, is_demo
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertAssess.run(
    assessId,
    userId,
    'Acme Corp Annual Security Audit',
    'Acme Corporation',
    'Authorized security assessment of web applications, public APIs, and external infrastructure.',
    'Web Application',
    '2026-10-01',
    '2026-10-15',
    'ACTIVE',
    1,
    'https://portal.acme.corp, https://api.acme.corp/v1, 192.168.10.0/24',
    'hr.acme.corp, payment-gateway.thirdparty.com, employee personal devices',
    'Identify critical OWASP Top 10 vulnerabilities, verify TLS configuration, evaluate wireless boundary exposure.',
    'Engagement conducted under non-disruptive testing windows between 20:00 - 04:00 UTC.',
    'Authorized under Scope Agreement #ACME-SEC-2026-09 signed by CISO.',
    1
  );

  // Targets (In-Scope & Out-of-Scope)
  const insertTarget = db.prepare(`
    INSERT INTO targets (id, assessment_id, domain, ip_address, hostname, url, description, scope_status, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertTarget.run('tgt-01', assessId, 'portal.acme.corp', '192.168.10.50', 'srv-web-01', 'https://portal.acme.corp', 'Customer authentication & account portal', 'IN_SCOPE', 'Authorized for OWASP Top 10 passive and active tests');
  insertTarget.run('tgt-02', assessId, 'api.acme.corp', '192.168.10.51', 'srv-api-01', 'https://api.acme.corp/v1', 'REST API gateway for mobile apps', 'IN_SCOPE', 'Token auth testing authorized');
  insertTarget.run('tgt-03', assessId, 'hr.acme.corp', '192.168.20.100', 'srv-hr-internal', 'https://hr.acme.corp', 'Internal HR portal - 3rd party SaaS', 'OUT_OF_SCOPE', 'DO NOT ASSESS: Third-party hosted system without vendor waiver');

  // Recon observations
  const insertRecon = db.prepare(`
    INSERT INTO recon_observations (id, assessment_id, observation_type, value, notes)
    VALUES (?, ?, ?, ?, ?)
  `);
  insertRecon.run('rc-01', assessId, 'SUBDOMAIN', 'api.acme.corp', 'Discovered via certificate transparency logs');
  insertRecon.run('rc-02', assessId, 'TECH', 'Nginx 1.24.0, Node.js, Express', 'Identified through Server headers and stack traces');
  insertRecon.run('rc-03', assessId, 'DNS', 'MX: mail.acme.corp (10), SPF: v=spf1 include:_spf.acme.corp ~all', 'Standard defensive mail records');

  // Network observations
  const insertNet = db.prepare(`
    INSERT INTO network_observations (id, assessment_id, host, ip_address, port, protocol, service, version, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertNet.run('net-01', assessId, 'portal.acme.corp', '192.168.10.50', 443, 'TCP', 'HTTPS', 'OpenSSL 3.0.8', 'TLS 1.3 enabled');
  insertNet.run('net-02', assessId, 'portal.acme.corp', '192.168.10.50', 80, 'TCP', 'HTTP', 'Nginx 1.24.0', 'Redirects 301 to HTTPS');
  insertNet.run('net-03', assessId, 'api.acme.corp', '192.168.10.51', 8443, 'TCP', 'HTTPS-ALT', 'Express API Gateway', 'JWT Bearer required');

  // Wireless observations (Passive)
  const insertWifi = db.prepare(`
    INSERT INTO wireless_observations (id, assessment_id, ssid, bssid, frequency, channel, signal_dbm, security_capability, wifi_standard, latitude, longitude, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertWifi.run('wf-01', assessId, 'Acme-Corp-Staff', '00:14:22:01:23:45', 5180, 36, -52, 'WPA3-SAE', '802.11ax (Wi-Fi 6)', 23.8103, 90.4125, 'Staff enterprise wireless network with SAE authentication');
  insertWifi.run('wf-02', assessId, 'Acme-Guest', '00:14:22:01:23:46', 2412, 1, -68, 'OPEN', '802.11n (Wi-Fi 4)', 23.8103, 90.4125, 'Unencrypted guest wireless network with captive portal');
  insertWifi.run('wf-03', assessId, 'Acme-IoT-Sensors', '00:14:22:01:23:47', 2437, 6, -74, 'WPA2-PSK', '802.11n (Wi-Fi 4)', 23.8104, 90.4126, 'Isolated IoT infrastructure SSID');

  // Findings
  const insertFinding = db.prepare(`
    INSERT INTO findings (
      id, assessment_id, custom_id, title, description, affected_asset,
      severity, cvss_score, cwe_id, category, technical_details, impact, remediation, reference_links, status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertFinding.run(
    'fnd-01',
    assessId,
    'F-001',
    'SQL Injection Vulnerability in Order Search Filter',
    'The order lookup parameter "order_ref" fails to sanitize inputs before concatenating into database query.',
    'https://portal.acme.corp/api/orders?search=',
    'CRITICAL',
    9.8,
    'CWE-89',
    'A03:2021-Injection',
    'Payload: 1\' OR 1=1-- returned full records. DBMS: PostgreSQL 15.3.',
    'Complete unauthorized database exfiltration and arbitrary data tampering.',
    'Adopt parameterized SQL statements or ORM binding. Never concatenate user input directly into SQL strings.',
    'OWASP Top 10:2021 A03 - Injection, CWE-89',
    'CONFIRMED'
  );
  insertFinding.run(
    'fnd-02',
    assessId,
    'F-002',
    'Missing HTTP Strict-Transport-Security (HSTS) Header',
    'The web server serves responses without the Strict-Transport-Security header, allowing protocol downgrade attacks.',
    'https://portal.acme.corp',
    'MEDIUM',
    5.3,
    'CWE-319',
    'A05:2021-Security Misconfiguration',
    'Curl response check confirmed header absence on port 443 response headers.',
    'Users on insecure public Wi-Fi networks can be subjected to SSL stripping and eavesdropping.',
    'Add "Strict-Transport-Security: max-age=31536000; includeSubDomains; preload" in web server headers.',
    'OWASP ASVS v4.0 Section 14.4.1',
    'OPEN'
  );
  insertFinding.run(
    'fnd-03',
    assessId,
    'F-003',
    'Unencrypted Public Guest Wi-Fi Network',
    'The Acme-Guest access point transmits all traffic in the clear without 802.11 enhanced open (OWE) or encryption.',
    'SSID: Acme-Guest (00:14:22:01:23:46)',
    'LOW',
    3.1,
    'CWE-319',
    'Wireless Security',
    'Beacon capability flag 0x0001 (ESS), no RSN information element present.',
    'Eavesdropping on unencrypted HTTP protocols transmitted by visitor devices.',
    'Deploy Wi-Fi Enhanced Open (OWE - RFC 8110) or isolate guest VLAN with client separation.',
    'NIST SP 800-153 Guidelines for Securing Wireless Local Area Networks',
    'OPEN'
  );

  // Assessment Notebook Entries
  const insertNote = db.prepare(`
    INSERT INTO notebook_entries (id, assessment_id, title, content, category, tags, is_pinned, is_favorite)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `);
  insertNote.run(
    'nb-01',
    assessId,
    'Subdomain Enumeration Strategy & Findings',
    'Utilized certificate transparency logs crt.sh and passive DNS. Identified api.acme.corp resolving to 192.168.10.51. Next step: analyze API swagger schema on /v1/docs.',
    'OBSERVATION',
    '#recon, #subdomain, #api',
    1,
    1
  );
  insertNote.run(
    'nb-02',
    assessId,
    'Testing Credentials & Token Formats',
    'Operator token issued for QA testing environment. JWT algorithm RS256 verified. Testing role escalation by modifying claims.',
    'CREDENTIALS',
    '#auth, #jwt, #tokens',
    0,
    1
  );
  insertNote.run(
    'nb-03',
    assessId,
    'Non-Disruptive Port Scanning Commands',
    'nmap -sS -Pn -p 80,443,8080,8443 -T3 --reason portal.acme.corp -oN portal_quick.txt',
    'COMMANDS',
    '#nmap, #network, #passive',
    0,
    0
  );
  insertNote.run(
    'nb-04',
    assessId,
    'OWASP Top 10 Web Assessment Methodology',
    'Following OWASP WSTG v4.2 testing framework across authenticated and unauthenticated test vectors. Non-disruptive testing windows strictly between 20:00 - 04:00 UTC.',
    'METHODOLOGY',
    '#owasp, #wstg, #methodology, #governance',
    1,
    1
  );
  insertNote.run(
    'nb-05',
    assessId,
    'Passive Infrastructure & Network Recon',
    'Enumerated ASN 13335 (Cloudflare) fronting public web endpoints. Identified mail.acme.corp, vpn.acme.corp (192.168.10.15) and internal dev.acme.corp.',
    'RECON',
    '#recon, #dns, #passive, #infrastructure',
    0,
    1
  );
  insertNote.run(
    'nb-06',
    assessId,
    'Pre-Assessment ROE & Scope Authorization Checklist',
    '[x] Written Rules of Engagement (ROE) signed by CISO.\n[x] Emergency contact escalation hotline verified (ext 9911).\n[x] Primary IP boundaries (192.168.10.0/24) verified in-scope.\n[ ] Third-party cloud notification filed.',
    'CHECKLIST',
    '#checklist, #compliance, #roe, #audit',
    0,
    1
  );
  insertNote.run(
    'nb-07',
    assessId,
    'Evidence Ingestion & SHA-256 Hashing Protocol',
    'All exported screen captures, raw HTTP response logs, and technical proofs must have immediate cryptographic SHA-256 integrity checksums calculated prior to archival.',
    'EVIDENCE',
    '#evidence, #sha256, #integrity, #forensics',
    0,
    1
  );

  // Report History Record
  const insertReportHist = db.prepare(`
    INSERT INTO report_history (id, assessment_id, report_title, format, file_path, file_size, sha256_hash)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);
  insertReportHist.run(
    'rep-01',
    assessId,
    'Acme Corp Annual Security Audit - Initial Technical Report',
    'TXT',
    'uploads/reports/redops-report-asm-acme-2026.txt',
    4120,
    'a7f45c8b9392e29302194857bdf821739c91038475628192a746592817293847'
  );

  // Persistent Activity Logs (Real database records, no mock arrays!)
  const activities = [
    {
      id: 'act-scope-01',
      assessment_id: assessId,
      title: 'Authorized Scope Agreement Verified',
      type: 'SCOPE',
      badge_class: 'badge-roe-auth',
      icon: '🛡️',
      color: 'rgba(0, 230, 118, 0.2)',
      target_section: 'assessments',
      target_keyword: 'Acme Corp Annual Security Audit',
      asset: 'Acme Corporation Primary Infrastructure',
      auth_reference: 'Scope Agreement #ACME-SEC-2026-09',
      rules_of_engagement: 'Strictly Non-Disruptive • Passive & Authorized Only',
      operator_title: 'Lead Assessment Specialist',
      description: 'Explicit Rules of Engagement and authorization declaration confirmed and timestamped. All production payment processors and third-party SaaS vendors marked as out-of-scope boundaries.'
    },
    {
      id: 'act-target-01',
      assessment_id: assessId,
      title: 'Target In-Scope: portal.acme.corp',
      type: 'TARGET',
      badge_class: 'badge-roe-target',
      icon: '🎯',
      color: 'rgba(0, 229, 255, 0.2)',
      target_section: 'targets',
      target_keyword: 'portal.acme.corp',
      asset: 'portal.acme.corp (192.168.10.50)',
      auth_reference: 'Approved Target Asset #TGT-001',
      rules_of_engagement: 'Scope Boundary Verified • Web Application & API Endpoints Only',
      operator_title: 'Senior Security Analyst',
      description: 'Primary customer authentication portal validated as an authorized in-scope asset. DNS resolving to 192.168.10.50 within designated corporate CIDR block.'
    },
    {
      id: 'act-wifi-01',
      assessment_id: assessId,
      title: 'Passive 802.11 AP Discovered: Acme-Corp-Staff',
      type: 'WIRELESS',
      badge_class: 'badge-roe-wireless',
      icon: '📶',
      color: 'rgba(179, 136, 255, 0.2)',
      target_section: 'wireless',
      target_keyword: 'Acme-Corp-Staff',
      asset: 'SSID: Acme-Corp-Staff (BSSID: 00:14:22:01:23:45)',
      auth_reference: 'Facility Passive Enumeration',
      rules_of_engagement: 'Strictly Listen-Only • Zero Probe Frames Transmitted',
      operator_title: 'Field Penetration Tester',
      description: 'Passive Wi-Fi beacon inspection identified corporate internal wireless access point operating on Channel 36 (5 GHz). Uses WPA3-SAE with 802.11ax protocol.'
    },
    {
      id: 'act-finding-01',
      assessment_id: assessId,
      title: 'Critical Finding: F-001 SQL Injection in /orders',
      type: 'FINDING',
      badge_class: 'badge-roe-finding',
      icon: '⚠️',
      color: 'rgba(255, 82, 82, 0.2)',
      target_section: 'findings',
      target_keyword: 'SQL Injection',
      asset: 'https://portal.acme.corp/api/orders?search=',
      auth_reference: 'Finding Reference #F-001',
      rules_of_engagement: 'Non-Destructive PoC Only • No Data Modification',
      operator_title: 'Application Security Engineer',
      description: 'Unauthenticated blind boolean SQL injection identified on query parameter order_ref. Vulnerability allows arbitrary database extraction without authentication. Remediation requires parameterized prepared statements.'
    },
    {
      id: 'act-note-01',
      assessment_id: assessId,
      title: 'Notebook Entry: Subdomain Enumeration Strategy',
      type: 'NOTEBOOK',
      badge_class: 'badge-roe-target',
      icon: '📓',
      color: 'rgba(0, 229, 255, 0.2)',
      target_section: 'notebook',
      target_keyword: 'Subdomain Enumeration',
      asset: 'Assessment Notebook',
      auth_reference: 'Engagement Field Notes',
      rules_of_engagement: 'Documented Technical Log',
      operator_title: 'Assessment Specialist',
      description: 'Recorded strategy for API reconnaissance and certificate transparency logs.'
    }
  ];

  activities.forEach(a => logActivity(a));

  console.log('[Database] Seed data successfully populated with real persistent entities.');
}

function sanitizeToolData(toolName, rawData) {
  if (!rawData) return null;
  let data;
  try {
    data = typeof rawData === 'string' ? JSON.parse(rawData) : JSON.parse(JSON.stringify(rawData));
  } catch {
    return String(rawData);
  }

  function deepSanitize(obj) {
    if (!obj || typeof obj !== 'object') return;
    for (const key of Object.keys(obj)) {
      const lowerKey = key.toLowerCase();
      if (
        lowerKey.includes('password') ||
        lowerKey.includes('secret') ||
        lowerKey.includes('private_key') ||
        lowerKey.includes('privkey') ||
        lowerKey.includes('api_key') ||
        lowerKey.includes('bearer') ||
        lowerKey.includes('auth_token') ||
        (lowerKey === 'raw_token' && toolName === 'JWT Decoder')
      ) {
        obj[key] = '[REDACTED_SENSITIVE_DATA]';
      } else if (typeof obj[key] === 'object') {
        deepSanitize(obj[key]);
      }
    }
  }

  if (data.input_password || data.password) {
    delete data.input_password;
    delete data.password;
    data.password_entered = '[REDACTED_LOCAL_ESTIMATE_ONLY]';
  }

  deepSanitize(data);
  return JSON.stringify(data);
}

initDatabase();

module.exports = {
  db,
  initDatabase,
  logActivity,
  resetDatabase,
  clearAssessmentData,
  seedDemoData,
  initDefaultOperator,
  sanitizeToolData
};
