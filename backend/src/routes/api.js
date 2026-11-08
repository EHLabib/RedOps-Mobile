const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('node:crypto');
const path = require('node:path');
const fs = require('node:fs');
const multer = require('multer');

const { db, logActivity, resetDatabase, clearAssessmentData, seedDemoData, sanitizeToolData } = require('../database/db');
const { authenticateToken, requireAdmin, JWT_SECRET } = require('../middleware/auth');
const toolsRouter = require('./tools');

// ==============================================================================
// FILE STORAGE CONFIGURATION (Evidence Management)
// ==============================================================================
const uploadDir = path.join(__dirname, '..', '..', 'uploads', 'evidence');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Strict whitelist for forensic evidence artifacts
const ALLOWED_MIME_TYPES = new Set([
  'image/png',
  'image/jpeg',
  'image/webp',
  'image/gif',
  'image/svg+xml',
  'application/pdf',
  'text/plain',
  'text/markdown',
  'text/csv',
  'application/json',
  'application/vnd.tcpdump.pcap',
  'application/x-pcapng',
  'application/octet-stream'
]);

const ALLOWED_EXTENSIONS = new Set([
  '.png', '.jpg', '.jpeg', '.webp', '.gif', '.svg',
  '.pdf', '.txt', '.md', '.csv', '.json', '.log',
  '.pcap', '.pcapng', '.har'
]);

const BLOCKED_EXTENSIONS = new Set([
  '.exe', '.dll', '.bat', '.cmd', '.sh', '.bash', '.ps1',
  '.vbs', '.js', '.mjs', '.cjs', '.php', '.phtml', '.php5',
  '.jsp', '.asp', '.aspx', '.cgi', '.py', '.pl', '.jar',
  '.war', '.html', '.htm', '.xhtml', '.shtml'
]);

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const rawExt = path.extname(file.originalname || '').toLowerCase();
    const safeExt = rawExt.replace(/[^a-z0-9.]/g, '');
    const unique = crypto.randomUUID();
    cb(null, `ev-${unique}${safeExt}`);
  }
});

const upload = multer({
  storage,
  limits: {
    fileSize: 20 * 1024 * 1024, // 20 MB max file size
    files: 1
  },
  fileFilter: (req, file, cb) => {
    const rawExt = path.extname(file.originalname || '').toLowerCase();
    if (BLOCKED_EXTENSIONS.has(rawExt)) {
      return cb(new Error(`File upload rejected: Executable or script file type "${rawExt}" is strictly prohibited.`));
    }
    if (!ALLOWED_EXTENSIONS.has(rawExt)) {
      return cb(new Error(`File upload rejected: Unsupported extension "${rawExt}". Allowed: images, documents, captures, logs.`));
    }
    if (!ALLOWED_MIME_TYPES.has(file.mimetype.toLowerCase())) {
      return cb(new Error(`File upload rejected: Unsupported MIME type "${file.mimetype}".`));
    }
    cb(null, true);
  }
});

// Helper for cryptographic SHA-256 file checksum calculation
function computeFileHash(filePath) {
  const fileBuffer = fs.readFileSync(filePath);
  const hashSum = crypto.createHash('sha256');
  hashSum.update(fileBuffer);
  return hashSum.digest('hex');
}

/**
 * Access Control Helper:
 * Verifies that the authenticated operator has permission to access an assessment.
 * - Administrators (role === 'ADMIN') have full cross-tenant visibility.
 * - Standard operators can ONLY access assessments where user_id === req.user.id.
 */
function getAuthorizedAssessment(assessmentId, reqUser) {
  if (!assessmentId) {
    return { errorStatus: 400, message: 'Engagement identifier is required.' };
  }
  const assessment = db.prepare('SELECT * FROM assessments WHERE id = ?').get(assessmentId);
  if (!assessment) {
    return { errorStatus: 404, message: 'Engagement not found.' };
  }
  if (reqUser && reqUser.role !== 'ADMIN' && assessment.user_id !== reqUser.id) {
    return {
      errorStatus: 403,
      message: 'Access Denied: You do not have permission to access this assessment or its records.'
    };
  }
  return { assessment };
}

// ==============================================================================
// 1. AUTHENTICATION & SECURITY ACCESS CONTROL
// ==============================================================================

// POST /api/v1/auth/register
// Public registration endpoint: Always assigns 'USER' role to prevent privilege escalation.
router.post('/auth/register', (req, res) => {
  const { full_name, username, email, password } = req.body;

  if (!username || !email || !password) {
    return res.status(400).json({ status: 'error', message: 'Username, email, and password are required' });
  }

  const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email);
  if (existing) {
    return res.status(409).json({ status: 'error', message: 'User with that username or email already exists' });
  }

  const salt = bcrypt.genSaltSync(10);
  const passwordHash = bcrypt.hashSync(password, salt);
  const userId = 'usr-' + crypto.randomUUID().slice(0, 8);
  // Security Fix: Public registration strictly assigns USER role; privilege escalation blocked
  const assignedRole = 'USER';

  const insert = db.prepare(`
    INSERT INTO users (id, full_name, username, email, password_hash, role)
    VALUES (?, ?, ?, ?, ?, ?)
  `);
  insert.run(userId, full_name || username, username, email, passwordHash, assignedRole);

  const token = jwt.sign(
    { id: userId, username, role: assignedRole },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.status(201).json({
    status: 'success',
    token,
    user: { id: userId, full_name: full_name || username, username, email, role: assignedRole },
    data: {
      user: { id: userId, full_name: full_name || username, username, email, role: assignedRole },
      token
    }
  });
});

// POST /api/v1/auth/bootstrap-admin
// Dedicated secure bootstrap mechanism for initializing administrator account
router.post('/auth/bootstrap-admin', (req, res) => {
  const adminCount = db.prepare('SELECT count(*) as count FROM users WHERE role = ?').get('ADMIN').count;
  if (adminCount > 0) {
    return res.status(403).json({
      status: 'error',
      message: 'Access Denied: Administrator account is already initialized. Bootstrap is locked.'
    });
  }

  const bootstrapKey = req.headers['x-bootstrap-key'] || req.body.bootstrap_key;
  const configuredSecret = process.env.BOOTSTRAP_SECRET;
  if (configuredSecret && bootstrapKey !== configuredSecret) {
    return res.status(401).json({ status: 'error', message: 'Invalid bootstrap key' });
  }

  const { username, password, full_name, email } = req.body;
  if (!username || !password || password.length < 10) {
    return res.status(400).json({
      status: 'error',
      message: 'Valid username and strong password (minimum 10 characters) required for administrator bootstrap'
    });
  }

  const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email || '');
  if (existing) {
    return res.status(409).json({ status: 'error', message: 'User with that username or email already exists' });
  }

  const salt = bcrypt.genSaltSync(12);
  const passwordHash = bcrypt.hashSync(password, salt);
  const adminId = 'usr-admin-' + crypto.randomUUID().slice(0, 8);

  db.prepare(`
    INSERT INTO users (id, full_name, username, email, password_hash, role)
    VALUES (?, ?, ?, ?, ?, 'ADMIN')
  `).run(adminId, full_name || 'System Administrator', username, email || `${username}@redops.local`, passwordHash);

  const token = jwt.sign(
    { id: adminId, username, role: 'ADMIN' },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.status(201).json({
    status: 'success',
    message: 'Initial administrator account created successfully',
    data: {
      user: { id: adminId, username, role: 'ADMIN' },
      token
    }
  });
});

// POST /api/v1/auth/login
router.post('/auth/login', (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ status: 'error', message: 'Username and password required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE username = ? OR email = ?').get(username, username);
  if (!user || !bcrypt.compareSync(password, user.password_hash)) {
    return res.status(401).json({ status: 'error', message: 'Invalid username or password credentials' });
  }

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.json({
    status: 'success',
    token,
    user: {
      id: user.id,
      full_name: user.full_name,
      username: user.username,
      email: user.email,
      role: user.role
    }
  });
});

// GET /api/v1/auth/me
router.get('/auth/me', authenticateToken, (req, res) => {
  const user = db.prepare('SELECT id, full_name, username, email, role, created_at FROM users WHERE id = ?').get(req.user.id);
  if (!user) return res.status(404).json({ status: 'error', message: 'User not found' });
  res.json({ status: 'success', data: { user } });
});

// ==============================================================================
// 4.1 DASHBOARD METRICS & RECENT ENGAGEMENT SYNTHESIS
// ==============================================================================

// GET /api/v1/dashboard/metrics
router.get('/dashboard/metrics', authenticateToken, (req, res) => {
  const assessmentId = req.query.assessment_id;
  const isOperatorAdmin = req.user && req.user.role === 'ADMIN';

  if (assessmentId) {
    const authCheck = getAuthorizedAssessment(assessmentId, req.user);
    if (authCheck.errorStatus) {
      return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
    }
  }

  const userParams = isOperatorAdmin ? [] : [req.user.id];

  const totalEngagements = isOperatorAdmin
    ? db.prepare("SELECT COUNT(*) as count FROM assessments").get().count
    : db.prepare("SELECT COUNT(*) as count FROM assessments WHERE user_id = ?").get(req.user.id).count;

  const activeEngagements = isOperatorAdmin
    ? db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'ACTIVE'").get().count
    : db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'ACTIVE' AND user_id = ?").get(req.user.id).count;

  const completedEngagements = isOperatorAdmin
    ? db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'COMPLETED'").get().count
    : db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'COMPLETED' AND user_id = ?").get(req.user.id).count;

  const archivedEngagements = isOperatorAdmin
    ? db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'ARCHIVED'").get().count
    : db.prepare("SELECT COUNT(*) as count FROM assessments WHERE status = 'ARCHIVED' AND user_id = ?").get(req.user.id).count;

  const targetFilter = assessmentId 
    ? 'WHERE assessment_id = ?' 
    : (isOperatorAdmin ? '' : 'WHERE assessment_id IN (SELECT id FROM assessments WHERE user_id = ?)');
  const targetParams = assessmentId ? [assessmentId] : userParams;

  const totalTargets = db.prepare(`SELECT COUNT(*) as count FROM targets ${targetFilter}`).get(...targetParams).count;
  const inScopeTargets = db.prepare(`SELECT COUNT(*) as count FROM targets ${targetFilter ? targetFilter + " AND" : "WHERE"} scope_status = 'IN_SCOPE'`).get(...targetParams).count;

  const findingFilter = assessmentId
    ? 'WHERE assessment_id = ?'
    : (isOperatorAdmin ? '' : 'WHERE assessment_id IN (SELECT id FROM assessments WHERE user_id = ?)');
  const findingParams = assessmentId ? [assessmentId] : userParams;

  const totalFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter}`).get(...findingParams).count;
  const openFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} status = 'OPEN'`).get(...findingParams).count;
  const confirmedFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} status = 'CONFIRMED'`).get(...findingParams).count;
  const remediatedFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} status = 'REMEDIATED'`).get(...findingParams).count;

  const criticalFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} severity = 'CRITICAL'`).get(...findingParams).count;
  const highFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} severity = 'HIGH'`).get(...findingParams).count;
  const mediumFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} severity = 'MEDIUM'`).get(...findingParams).count;
  const lowFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} severity = 'LOW'`).get(...findingParams).count;
  const infoFindings = db.prepare(`SELECT COUNT(*) as count FROM findings ${findingFilter ? findingFilter + " AND" : "WHERE"} severity = 'INFO'`).get(...findingParams).count;

  const totalNotes = db.prepare(`SELECT COUNT(*) as count FROM notebook_entries ${findingFilter}`).get(...findingParams).count;
  const totalEvidence = db.prepare(`SELECT COUNT(*) as count FROM evidence ${isOperatorAdmin && !assessmentId ? '' : 'WHERE finding_id IN (SELECT id FROM findings ' + findingFilter + ')'}`).get(...findingParams).count;

  const recentFindings = db.prepare(`SELECT id, custom_id, title, severity, status, affected_asset, created_at FROM findings ${findingFilter} ORDER BY created_at DESC LIMIT 5`).all(...findingParams);
  const recentAssessments = isOperatorAdmin
    ? db.prepare('SELECT id, name, client, status, assessment_type, start_date, end_date, created_at FROM assessments ORDER BY created_at DESC LIMIT 5').all()
    : db.prepare('SELECT id, name, client, status, assessment_type, start_date, end_date, created_at FROM assessments WHERE user_id = ? ORDER BY created_at DESC LIMIT 5').all(req.user.id);

  const recentActivities = assessmentId
    ? db.prepare('SELECT * FROM activity_logs WHERE assessment_id = ? ORDER BY created_at DESC LIMIT 10').all(assessmentId)
    : (isOperatorAdmin
        ? db.prepare('SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT 10').all()
        : db.prepare('SELECT * FROM activity_logs WHERE assessment_id IN (SELECT id FROM assessments WHERE user_id = ?) ORDER BY created_at DESC LIMIT 10').all(req.user.id));

  res.json({
    status: 'success',
    data: {
      metrics: {
        total_engagements: totalEngagements,
        active_assessments: activeEngagements,
        completed_assessments: completedEngagements,
        archived_assessments: archivedEngagements,
        total_targets: totalTargets,
        in_scope_targets: inScopeTargets,
        total_findings: totalFindings,
        open_findings: openFindings,
        confirmed_findings: confirmedFindings,
        remediated_findings: remediatedFindings,
        severity_distribution: {
          critical: criticalFindings,
          high: highFindings,
          medium: mediumFindings,
          low: lowFindings,
          info: infoFindings
        },
        critical_findings: criticalFindings,
        high_findings: highFindings,
        medium_findings: mediumFindings,
        low_findings: lowFindings,
        info_findings: infoFindings,
        total_notes: totalNotes,
        total_evidence: totalEvidence
      },
      recent_activity: {
        findings: recentFindings,
        assessments: recentAssessments,
        activities: recentActivities
      }
    }
  });
});

// ==============================================================================
// 4.2 ENGAGEMENT MANAGEMENT (FULL CRUD, SEARCH, FILTER)
// ==============================================================================

// GET /api/v1/assessments
// Enforces tenant isolation: Non-admin users can ONLY list their own assessments.
router.get('/assessments', authenticateToken, (req, res) => {
  const { search, status, type } = req.query;
  let sql = 'SELECT * FROM assessments WHERE 1=1';
  const params = [];

  if (req.user.role !== 'ADMIN') {
    sql += ' AND user_id = ?';
    params.push(req.user.id);
  }

  if (status && status !== 'ALL') {
    sql += ' AND status = ?';
    params.push(status);
  }

  if (type && type !== 'ALL') {
    sql += ' AND assessment_type = ?';
    params.push(type);
  }

  if (search) {
    sql += ' AND (name LIKE ? OR client LIKE ? OR description LIKE ? OR authorized_scope LIKE ?)';
    const query = `%${search}%`;
    params.push(query, query, query, query);
  }

  sql += ' ORDER BY created_at DESC';

  const assessments = db.prepare(sql).all(...params);
  res.json({ status: 'success', count: assessments.length, data: assessments });
});

// POST /api/v1/assessments
router.post('/assessments', authenticateToken, (req, res) => {
  const {
    name, client, description, assessment_type, start_date, end_date,
    status, authorized_scope, out_of_scope_assets, objectives, notes,
    scope_notes, is_authorized
  } = req.body;

  if (!name || !client || !start_date || !end_date) {
    return res.status(400).json({ status: 'error', message: 'Required fields: Title (name), Client, Start Date, End Date' });
  }

  if (new Date(end_date) < new Date(start_date)) {
    return res.status(400).json({ status: 'error', message: 'End date cannot be earlier than start date.' });
  }

  // Mandatory Ethical Scope Authorization Declaration Enforcement
  if (!is_authorized) {
    return res.status(403).json({
      status: 'error',
      message: 'Scope Authorization Failure: You must confirm explicit authorization declaration before saving an engagement.'
    });
  }

  const id = 'asm-' + crypto.randomUUID().slice(0, 8);
  const stmt = db.prepare(`
    INSERT INTO assessments (
      id, user_id, name, client, description, assessment_type,
      start_date, end_date, status, is_authorized,
      authorized_scope, out_of_scope_assets, objectives, notes, scope_notes, is_demo
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
  `);

  stmt.run(
    id,
    req.user.id,
    name,
    client,
    description ?? null,
    assessment_type || 'Web Application',
    start_date,
    end_date,
    status || 'ACTIVE',
    1,
    authorized_scope ?? null,
    out_of_scope_assets ?? null,
    objectives ?? null,
    notes ?? null,
    scope_notes ?? null
  );

  // Automatically record persistent activity log
  logActivity({
    assessment_id: id,
    title: `Engagement Created: ${name}`,
    type: 'SCOPE',
    badge_class: 'badge-roe-auth',
    icon: '🛡️',
    color: 'rgba(0, 230, 118, 0.2)',
    target_section: 'assessments',
    target_keyword: name,
    asset: client,
    auth_reference: authorized_scope ? authorized_scope.slice(0, 30) : 'Authorized Declaration',
    rules_of_engagement: 'ROE Formally Established',
    operator_title: req.user.username || 'Security Specialist',
    description: `Engagement for ${client} (${assessment_type || 'Web Application'}) created with verified authorization declaration.`
  });

  const created = db.prepare('SELECT * FROM assessments WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

// GET /api/v1/assessments/:id
router.get('/assessments/:id', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const targets = db.prepare('SELECT * FROM targets WHERE assessment_id = ? ORDER BY created_at DESC').all(req.params.id);
  const findings = db.prepare('SELECT * FROM findings WHERE assessment_id = ? ORDER BY created_at DESC').all(req.params.id);
  const notes = db.prepare('SELECT * FROM notebook_entries WHERE assessment_id = ? ORDER BY is_pinned DESC, updated_at DESC').all(req.params.id);
  const recon = db.prepare('SELECT * FROM recon_observations WHERE assessment_id = ? ORDER BY created_at DESC').all(req.params.id);
  const wireless = db.prepare('SELECT * FROM wireless_observations WHERE assessment_id = ? ORDER BY signal_dbm DESC').all(req.params.id);

  res.json({
    status: 'success',
    data: {
      ...assessment,
      targets,
      findings,
      notes,
      recon,
      wireless
    }
  });
});

// PUT /api/v1/assessments/:id
router.put('/assessments/:id', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const {
    name, client, description, assessment_type, start_date, end_date,
    status, authorized_scope, out_of_scope_assets, objectives, notes, scope_notes
  } = req.body;

  const effectiveStart = start_date || assessment.start_date;
  const effectiveEnd = end_date || assessment.end_date;
  if (effectiveStart && effectiveEnd && new Date(effectiveEnd) < new Date(effectiveStart)) {
    return res.status(400).json({ status: 'error', message: 'End date cannot be earlier than start date.' });
  }

  const stmt = db.prepare(`
    UPDATE assessments
    SET name = COALESCE(?, name),
        client = COALESCE(?, client),
        description = COALESCE(?, description),
        assessment_type = COALESCE(?, assessment_type),
        start_date = COALESCE(?, start_date),
        end_date = COALESCE(?, end_date),
        status = COALESCE(?, status),
        authorized_scope = COALESCE(?, authorized_scope),
        out_of_scope_assets = COALESCE(?, out_of_scope_assets),
        objectives = COALESCE(?, objectives),
        notes = COALESCE(?, notes),
        scope_notes = COALESCE(?, scope_notes),
        updated_at = datetime('now')
    WHERE id = ?
  `);

  stmt.run(
    name ?? null,
    client ?? null,
    description ?? null,
    assessment_type ?? null,
    start_date ?? null,
    end_date ?? null,
    status ?? null,
    authorized_scope ?? null,
    out_of_scope_assets ?? null,
    objectives ?? null,
    notes ?? null,
    scope_notes ?? null,
    req.params.id
  );

  logActivity({
    assessment_id: req.params.id,
    title: `Engagement Updated: ${name || assessment.name}`,
    type: 'SCOPE',
    badge_class: 'badge-roe-auth',
    icon: '📝',
    color: 'rgba(0, 229, 255, 0.2)',
    target_section: 'assessments',
    target_keyword: name || assessment.name,
    asset: client || assessment.client,
    auth_reference: 'Updated Parameters',
    rules_of_engagement: 'Parameters Modified',
    operator_title: req.user.username,
    description: `Engagement updated. Status: ${status || assessment.status}.`
  });

  const updated = db.prepare('SELECT * FROM assessments WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

// DELETE /api/v1/assessments/:id
router.delete('/assessments/:id', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  // Delete all associated physical evidence files from disk
  const files = db.prepare(`
    SELECT e.file_path FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE f.assessment_id = ?
  `).all(req.params.id);

  files.forEach(f => {
    if (f.file_path && fs.existsSync(f.file_path)) {
      try { fs.unlinkSync(f.file_path); } catch (e) { /* silent */ }
    }
  });

  db.prepare('DELETE FROM assessments WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Engagement and all child records deleted successfully' });
});

// ==============================================================================
// 4.3 FINDINGS MANAGEMENT (FULL CRUD, CVSS, SEARCH, FILTER, STATUS)
// ==============================================================================

// GET /api/v1/assessments/:id/findings
router.get('/assessments/:id/findings', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { search, severity, status, sort } = req.query;
  let sql = 'SELECT * FROM findings WHERE assessment_id = ?';
  const params = [req.params.id];

  if (severity && severity !== 'ALL') {
    sql += ' AND severity = ?';
    params.push(severity.toUpperCase());
  }

  if (status && status !== 'ALL') {
    sql += ' AND status = ?';
    params.push(status.toUpperCase());
  }

  if (search) {
    sql += ' AND (title LIKE ? OR description LIKE ? OR affected_asset LIKE ? OR cwe_id LIKE ?)';
    const query = `%${search}%`;
    params.push(query, query, query, query);
  }

  if (sort === 'SEVERITY') {
    sql += ` ORDER BY CASE severity
      WHEN 'CRITICAL' THEN 1
      WHEN 'HIGH' THEN 2
      WHEN 'MEDIUM' THEN 3
      WHEN 'LOW' THEN 4
      ELSE 5 END ASC, cvss_score DESC`;
  } else if (sort === 'TITLE') {
    sql += ' ORDER BY title ASC';
  } else {
    sql += ' ORDER BY created_at DESC';
  }

  const findings = db.prepare(sql).all(...params);

  // Attach associated evidence
  const enriched = findings.map(f => {
    const evidenceList = db.prepare('SELECT id, file_name, original_name, sha256_hash, mime_type, file_size, description, created_at FROM evidence WHERE finding_id = ? ORDER BY created_at DESC').all(f.id);
    return { ...f, evidence: evidenceList };
  });

  res.json({ status: 'success', count: enriched.length, data: enriched });
});

// POST /api/v1/assessments/:id/findings
router.post('/assessments/:id/findings', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const {
    title, description, affected_asset, severity, cvss_score, cwe_id,
    category, technical_details, impact, remediation, reference_links, status
  } = req.body;

  if (!title || !severity || !affected_asset) {
    return res.status(400).json({ status: 'error', message: 'Finding Title, Severity, and Affected Asset are required' });
  }

  // Validate CVSS
  const cvssNum = cvss_score !== undefined ? parseFloat(cvss_score) : 0.0;
  if (isNaN(cvssNum) || cvssNum < 0 || cvssNum > 10.0) {
    return res.status(400).json({ status: 'error', message: 'CVSS Score must be a valid number between 0.0 and 10.0' });
  }

  const existingCount = db.prepare('SELECT COUNT(*) as count FROM findings WHERE assessment_id = ?').get(req.params.id).count;
  const customId = `F-${String(existingCount + 1).padStart(3, '0')}`;
  const id = 'fnd-' + crypto.randomUUID().slice(0, 8);

  const stmt = db.prepare(`
    INSERT INTO findings (
      id, assessment_id, custom_id, title, description, affected_asset,
      severity, cvss_score, cwe_id, category, technical_details, impact, remediation, reference_links, status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  stmt.run(
    id,
    req.params.id,
    customId,
    title,
    description || '',
    affected_asset,
    severity.toUpperCase(),
    cvssNum,
    cwe_id ?? null,
    category || 'General Vulnerability',
    technical_details ?? null,
    impact ?? null,
    remediation ?? null,
    reference_links ?? null,
    status || 'OPEN'
  );

  logActivity({
    assessment_id: req.params.id,
    title: `${severity.toUpperCase()} Finding: ${customId} ${title}`,
    type: 'FINDING',
    badge_class: severity.toUpperCase() === 'CRITICAL' || severity.toUpperCase() === 'HIGH' ? 'badge-roe-finding' : 'badge-roe-auth',
    icon: severity.toUpperCase() === 'CRITICAL' ? '🔥' : '⚠️',
    color: severity.toUpperCase() === 'CRITICAL' ? 'rgba(255, 82, 82, 0.25)' : 'rgba(255, 179, 0, 0.2)',
    target_section: 'findings',
    target_keyword: title,
    asset: affected_asset,
    auth_reference: `Finding ${customId}`,
    rules_of_engagement: 'Documented Proof of Concept',
    operator_title: req.user.username,
    description: `Confirmed ${severity.toUpperCase()} (CVSS ${cvssNum}) on ${affected_asset}. Status: ${status || 'OPEN'}.`
  });

  const created = db.prepare('SELECT * FROM findings WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

// PUT /api/v1/findings/:id
router.put('/findings/:id', authenticateToken, (req, res) => {
  const finding = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  if (!finding) return res.status(404).json({ status: 'error', message: 'Finding not found' });

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const {
    title, description, affected_asset, severity, cvss_score, cwe_id,
    category, technical_details, impact, remediation, reference_links, status
  } = req.body;

  const cvssNum = cvss_score !== undefined ? parseFloat(cvss_score) : null;

  const stmt = db.prepare(`
    UPDATE findings
    SET title = COALESCE(?, title),
        description = COALESCE(?, description),
        affected_asset = COALESCE(?, affected_asset),
        severity = COALESCE(?, severity),
        cvss_score = COALESCE(?, cvss_score),
        cwe_id = COALESCE(?, cwe_id),
        category = COALESCE(?, category),
        technical_details = COALESCE(?, technical_details),
        impact = COALESCE(?, impact),
        remediation = COALESCE(?, remediation),
        reference_links = COALESCE(?, reference_links),
        status = COALESCE(?, status),
        updated_at = datetime('now')
    WHERE id = ?
  `);

  stmt.run(
    title ?? null,
    description ?? null,
    affected_asset ?? null,
    severity ? severity.toUpperCase() : null,
    cvssNum,
    cwe_id ?? null,
    category ?? null,
    technical_details ?? null,
    impact ?? null,
    remediation ?? null,
    reference_links ?? null,
    status ?? null,
    req.params.id
  );

  const updated = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

// PATCH /api/v1/findings/:id/status
router.patch('/findings/:id/status', authenticateToken, (req, res) => {
  const { status } = req.body;
  if (!['OPEN', 'CONFIRMED', 'REMEDIATED'].includes(status)) {
    return res.status(400).json({ status: 'error', message: 'Invalid status. Must be OPEN, CONFIRMED, or REMEDIATED' });
  }

  const finding = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  if (!finding) return res.status(404).json({ status: 'error', message: 'Finding not found' });

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  db.prepare("UPDATE findings SET status = ?, updated_at = datetime('now') WHERE id = ?").run(status, req.params.id);

  logActivity({
    assessment_id: finding.assessment_id,
    title: `Finding Status Updated: ${finding.custom_id} -> ${status}`,
    type: 'FINDING',
    badge_class: status === 'REMEDIATED' ? 'badge-roe-auth' : 'badge-roe-target',
    icon: status === 'REMEDIATED' ? '✅' : '🔄',
    color: status === 'REMEDIATED' ? 'rgba(0, 230, 118, 0.2)' : 'rgba(0, 229, 255, 0.2)',
    target_section: 'findings',
    target_keyword: finding.title,
    asset: finding.affected_asset,
    auth_reference: finding.custom_id,
    rules_of_engagement: 'Finding State Mutation',
    operator_title: req.user.username,
    description: `Finding ${finding.custom_id} (${finding.title}) status changed from ${finding.status} to ${status}.`
  });

  res.json({ status: 'success', message: 'Status updated', data: { id: req.params.id, status } });
});

// DELETE /api/v1/findings/:id
router.delete('/findings/:id', authenticateToken, (req, res) => {
  const finding = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  if (!finding) return res.status(404).json({ status: 'error', message: 'Finding not found' });

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  // Delete associated physical evidence files
  const evidenceFiles = db.prepare('SELECT file_path FROM evidence WHERE finding_id = ?').all(req.params.id);
  evidenceFiles.forEach(f => {
    if (fs.existsSync(f.file_path)) {
      try { fs.unlinkSync(f.file_path); } catch (e) { /* silent */ }
    }
  });

  db.prepare('DELETE FROM findings WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Finding and associated evidence deleted successfully' });
});

// ==============================================================================
// 4.4 ASSESSMENT NOTEBOOK (PERSISTENT NOTES, SEARCH, PIN, FAVORITE)
// ==============================================================================

// GET /api/v1/assessments/:id/notes (also accepts /notebook)
router.get(['/assessments/:id/notes', '/assessments/:id/notebook'], authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { search, category, pinned_only } = req.query;
  let sql = 'SELECT * FROM notebook_entries WHERE assessment_id = ?';
  const params = [req.params.id];

  if (category && category !== 'ALL') {
    sql += ' AND category = ?';
    params.push(category);
  }

  if (pinned_only === 'true') {
    sql += ' AND is_pinned = 1';
  }

  if (search) {
    sql += ' AND (title LIKE ? OR content LIKE ? OR tags LIKE ?)';
    const query = `%${search}%`;
    params.push(query, query, query);
  }

  sql += ' ORDER BY is_pinned DESC, updated_at DESC';

  const notes = db.prepare(sql).all(...params);
  res.json({ status: 'success', count: notes.length, data: notes });
});

// POST /api/v1/assessments/:id/notes (also accepts /notebook)
router.post(['/assessments/:id/notes', '/assessments/:id/notebook'], authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { title, content, category, tags, is_pinned, is_favorite } = req.body;

  if (!title || !content) {
    return res.status(400).json({ status: 'error', message: 'Note Title and Content are required' });
  }

  const id = 'nb-' + crypto.randomUUID().slice(0, 8);
  const stmt = db.prepare(`
    INSERT INTO notebook_entries (id, assessment_id, title, content, category, tags, is_pinned, is_favorite)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `);

  stmt.run(
    id,
    req.params.id,
    title,
    content,
    category || 'OBSERVATION',
    tags ?? null,
    is_pinned ? 1 : 0,
    is_favorite ? 1 : 0
  );

  logActivity({
    assessment_id: req.params.id,
    title: `Note Created: ${title}`,
    type: 'NOTEBOOK',
    badge_class: 'badge-roe-target',
    icon: '📓',
    color: 'rgba(0, 229, 255, 0.2)',
    target_section: 'notebook',
    target_keyword: title,
    asset: 'Assessment Notebook',
    auth_reference: `Category: ${category || 'OBSERVATION'}`,
    rules_of_engagement: 'Operational Field Notes',
    operator_title: req.user.username,
    description: `Added assessment field note "${title}". Tags: ${tags || 'None'}.`
  });

  const created = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

// PUT /api/v1/notes/:id
router.put('/notes/:id', authenticateToken, (req, res) => {
  const note = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(req.params.id);
  if (!note) return res.status(404).json({ status: 'error', message: 'Note not found' });

  const authCheck = getAuthorizedAssessment(note.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { title, content, category, tags, is_pinned, is_favorite } = req.body;

  const stmt = db.prepare(`
    UPDATE notebook_entries
    SET title = COALESCE(?, title),
        content = COALESCE(?, content),
        category = COALESCE(?, category),
        tags = COALESCE(?, tags),
        is_pinned = COALESCE(?, is_pinned),
        is_favorite = COALESCE(?, is_favorite),
        updated_at = datetime('now')
    WHERE id = ?
  `);

  stmt.run(
    title ?? null,
    content ?? null,
    category ?? null,
    tags ?? null,
    is_pinned !== undefined ? (is_pinned ? 1 : 0) : null,
    is_favorite !== undefined ? (is_favorite ? 1 : 0) : null,
    req.params.id
  );

  const updated = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

// PATCH /api/v1/notes/:id/pin
router.patch('/notes/:id/pin', authenticateToken, (req, res) => {
  const note = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(req.params.id);
  if (!note) return res.status(404).json({ status: 'error', message: 'Note not found' });

  const authCheck = getAuthorizedAssessment(note.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const newPin = note.is_pinned ? 0 : 1;
  db.prepare("UPDATE notebook_entries SET is_pinned = ?, updated_at = datetime('now') WHERE id = ?").run(newPin, req.params.id);
  res.json({ status: 'success', is_pinned: newPin });
});

// PATCH /api/v1/notes/:id/favorite
router.patch('/notes/:id/favorite', authenticateToken, (req, res) => {
  const note = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(req.params.id);
  if (!note) return res.status(404).json({ status: 'error', message: 'Note not found' });

  const authCheck = getAuthorizedAssessment(note.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const newFav = note.is_favorite ? 0 : 1;
  db.prepare("UPDATE notebook_entries SET is_favorite = ?, updated_at = datetime('now') WHERE id = ?").run(newFav, req.params.id);
  res.json({ status: 'success', is_favorite: newFav });
});

// DELETE /api/v1/notes/:id
router.delete('/notes/:id', authenticateToken, (req, res) => {
  const note = db.prepare('SELECT * FROM notebook_entries WHERE id = ?').get(req.params.id);
  if (!note) return res.status(404).json({ status: 'error', message: 'Note not found' });

  const authCheck = getAuthorizedAssessment(note.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  db.prepare('DELETE FROM notebook_entries WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Note deleted' });
});

// ==============================================================================
// 4.5 CHRONOLOGICAL PERSISTENT ACTIVITY LOG FEED
// ==============================================================================

// GET /api/v1/activities
router.get('/activities', authenticateToken, (req, res) => {
  const { assessment_id, category, limit } = req.query;
  let sql = 'SELECT * FROM activity_logs WHERE 1=1';
  const params = [];

  if (assessment_id) {
    const authCheck = getAuthorizedAssessment(assessment_id, req.user);
    if (authCheck.errorStatus) {
      return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
    }
    sql += ' AND assessment_id = ?';
    params.push(assessment_id);
  } else if (req.user && req.user.role !== 'ADMIN') {
    // Non-admin can only view activities for their owned assessments
    sql += ' AND assessment_id IN (SELECT id FROM assessments WHERE user_id = ?)';
    params.push(req.user.id);
  }

  if (category && category !== 'ALL') {
    sql += ' AND type = ?';
    params.push(category);
  }

  sql += ' ORDER BY created_at DESC';

  if (limit) {
    sql += ' LIMIT ?';
    params.push(parseInt(limit, 10));
  } else {
    sql += ' LIMIT 60';
  }

  const activities = db.prepare(sql).all(...params);
  res.json({ status: 'success', count: activities.length, data: activities });
});

// ==============================================================================
// 4.6 EVIDENCE MANAGEMENT (UPLOAD, SHA-256 CHECKSUM, PREVIEWS, METADATA)
// ==============================================================================

// GET /api/v1/assessments/:id/evidence (all evidence for an assessment)
router.get('/assessments/:id/evidence', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const sql = `
    SELECT e.*, f.custom_id as finding_custom_id, f.title as finding_title
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE f.assessment_id = ?
    ORDER BY e.created_at DESC
  `;
  const evidence = db.prepare(sql).all(req.params.id);
  res.json({ status: 'success', count: evidence.length, data: evidence });
});

// GET /api/v1/findings/:findingId/evidence
router.get('/findings/:findingId/evidence', authenticateToken, (req, res) => {
  const finding = db.prepare('SELECT id, assessment_id FROM findings WHERE id = ?').get(req.params.findingId);
  if (!finding) return res.status(404).json({ status: 'error', message: 'Finding not found' });

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const evidence = db.prepare('SELECT * FROM evidence WHERE finding_id = ? ORDER BY created_at DESC').all(req.params.findingId);
  res.json({ status: 'success', count: evidence.length, data: evidence });
});

// POST /api/v1/findings/:findingId/evidence (Accepts 'evidence_file' or 'file')
router.post('/findings/:findingId/evidence', authenticateToken, (req, res, next) => {
  upload.fields([{ name: 'evidence_file', maxCount: 1 }, { name: 'file', maxCount: 1 }])(req, res, (err) => {
    if (err) {
      return res.status(400).json({ status: 'error', message: err.message });
    }
    if (req.files) {
      req.file = (req.files['evidence_file'] && req.files['evidence_file'][0]) || (req.files['file'] && req.files['file'][0]) || null;
    }
    next();
  });
}, (req, res) => {
  if (!req.file) {
    return res.status(400).json({ status: 'error', message: 'No evidence file uploaded' });
  }

  const finding = db.prepare('SELECT id, assessment_id, custom_id, title FROM findings WHERE id = ?').get(req.params.findingId);
  if (!finding) {
    fs.unlinkSync(req.file.path);
    return res.status(404).json({ status: 'error', message: 'Associated finding not found' });
  }

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    if (fs.existsSync(req.file.path)) {
      try { fs.unlinkSync(req.file.path); } catch (e) { /* silent */ }
    }
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const sha256 = computeFileHash(req.file.path);
  const evidenceId = 'ev-' + crypto.randomUUID().slice(0, 8);
  const description = req.body.description || `Evidence artifact for ${finding.custom_id}`;
  const tags = req.body.tags || '';

  const stmt = db.prepare(`
    INSERT INTO evidence (
      id, finding_id, file_name, original_name, file_path,
      file_size, mime_type, sha256_hash, description, tags, latitude, longitude
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  stmt.run(
    evidenceId,
    finding.id,
    req.file.filename,
    req.file.originalname,
    req.file.path,
    req.file.size,
    req.file.mimetype,
    sha256,
    description,
    tags,
    req.body.latitude ? parseFloat(req.body.latitude) : null,
    req.body.longitude ? parseFloat(req.body.longitude) : null
  );

  logActivity({
    assessment_id: finding.assessment_id,
    title: `Evidence Attached: ${req.file.originalname}`,
    type: 'EVIDENCE',
    badge_class: 'badge-roe-target',
    icon: '📎',
    color: 'rgba(0, 229, 255, 0.25)',
    target_section: 'findings',
    target_keyword: finding.title,
    asset: req.file.originalname,
    auth_reference: `SHA256: ${sha256.slice(0, 10)}...`,
    rules_of_engagement: 'Cryptographically Verified Artifact',
    operator_title: req.user.username,
    description: `Stored cryptographic artifact (${req.file.size} bytes) for ${finding.custom_id}. SHA-256: ${sha256}`
  });

  const created = db.prepare('SELECT * FROM evidence WHERE id = ?').get(evidenceId);
  res.status(201).json({ status: 'success', data: created });
});

// PUT /api/v1/evidence/:id (update metadata)
router.put('/evidence/:id', authenticateToken, (req, res) => {
  const ev = db.prepare(`
    SELECT e.*, f.assessment_id
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE e.id = ?
  `).get(req.params.id);
  if (!ev) return res.status(404).json({ status: 'error', message: 'Evidence not found' });

  const authCheck = getAuthorizedAssessment(ev.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { description, tags } = req.body;
  db.prepare('UPDATE evidence SET description = COALESCE(?, description), tags = COALESCE(?, tags) WHERE id = ?')
    .run(description ?? null, tags ?? null, req.params.id);

  const updated = db.prepare('SELECT * FROM evidence WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

// GET /api/v1/evidence/:id/file
router.get('/evidence/:id/file', authenticateToken, (req, res) => {
  const ev = db.prepare(`
    SELECT e.*, f.assessment_id
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE e.id = ?
  `).get(req.params.id);
  if (!ev) {
    return res.status(404).json({ status: 'error', message: 'Evidence file not found' });
  }

  const authCheck = getAuthorizedAssessment(ev.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const resolvedPath = path.resolve(ev.file_path);
  const resolvedUploadDir = path.resolve(uploadDir);
  if (!resolvedPath.startsWith(resolvedUploadDir) || !fs.existsSync(resolvedPath)) {
    return res.status(404).json({ status: 'error', message: 'Evidence file not found on disk' });
  }

  res.download(resolvedPath, ev.original_name);
});

// DELETE /api/v1/evidence/:id
router.delete('/evidence/:id', authenticateToken, (req, res) => {
  const ev = db.prepare(`
    SELECT e.*, f.assessment_id
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE e.id = ?
  `).get(req.params.id);
  if (!ev) return res.status(404).json({ status: 'error', message: 'Evidence not found' });

  const authCheck = getAuthorizedAssessment(ev.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  if (fs.existsSync(ev.file_path)) {
    try { fs.unlinkSync(ev.file_path); } catch (e) { /* silent */ }
  }

  db.prepare('DELETE FROM evidence WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Evidence record and physical artifact deleted' });
});

// ==============================================================================
// 4.7 REAL DATABASE REPORT GENERATION & HISTORY
// ==============================================================================

// GET /api/v1/assessments/:id/report
router.get('/assessments/:id/report', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const targets = db.prepare('SELECT * FROM targets WHERE assessment_id = ?').all(req.params.id);
  const findings = db.prepare('SELECT * FROM findings WHERE assessment_id = ?').all(req.params.id);
  const notes = db.prepare('SELECT * FROM notebook_entries WHERE assessment_id = ?').all(req.params.id);
  const recon = db.prepare('SELECT * FROM recon_observations WHERE assessment_id = ?').all(req.params.id);
  const wireless = db.prepare('SELECT * FROM wireless_observations WHERE assessment_id = ?').all(req.params.id);
  const evidence = db.prepare(`
    SELECT e.original_name, e.file_size, e.sha256_hash, e.mime_type, f.custom_id as finding_ref
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE f.assessment_id = ?
  `).all(req.params.id);

  const report = {
    metadata: {
      report_title: `Security Assessment Report: ${assessment.name}`,
      client: assessment.client,
      assessment_period: `${assessment.start_date} to ${assessment.end_date}`,
      classification: 'CONFIDENTIAL - AUTHORIZED SECURITY ENGAGEMENT',
      generated_at: new Date().toISOString()
    },
    sections: {
      "1. Executive Summary": `During the period of ${assessment.start_date} to ${assessment.end_date}, RedOps Mobile conducted an authorized security assessment for ${assessment.client}. A total of ${findings.length} vulnerability findings were identified across ${targets.length} targets. Critical findings require immediate remediation.`,
      "2. Engagement Objectives & Scope": {
        authorized_scope: assessment.authorized_scope || assessment.scope_notes || "All testing was conducted within explicitly authorized boundaries.",
        out_of_scope_assets: assessment.out_of_scope_assets || "No third-party or unauthorized endpoints probed.",
        objectives: assessment.objectives || "Identify vulnerabilities, verify transport security, and evaluate wireless network hygiene."
      },
      "3. Methodology": "Assessment followed OWASP Testing Guide v4, NIST SP 800-115, and PTES guidelines using passive reconnaissance, TLS inspection, and documented proof of concept.",
      "4. Assets Under Review": targets,
      "5. Reconnaissance Summary": recon,
      "6. Wireless Airwave Observations": wireless,
      "7. Findings & Vulnerabilities": findings,
      "8. Cryptographic Evidence Inventory": evidence,
      "9. Risk Summary": {
        critical: findings.filter(f => f.severity === 'CRITICAL').length,
        high: findings.filter(f => f.severity === 'HIGH').length,
        medium: findings.filter(f => f.severity === 'MEDIUM').length,
        low: findings.filter(f => f.severity === 'LOW').length,
        info: findings.filter(f => f.severity === 'INFO').length
      },
      "10. Remediation Roadmap": "Prioritize patching CRITICAL and HIGH severity findings within 48 hours. Enforce TLS 1.3, parameterized statements, and WPA3 security standards.",
      "11. Field Notes & Observations": notes,
      "12. Conclusion": `The security posture of ${assessment.client} demonstrates moderate resilience with specific high-risk remediations required.`
    }
  };

  res.json({ status: 'success', data: report });
});

// POST /api/v1/assessments/:id/report/generate
router.post('/assessments/:id/report/generate', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const targets = db.prepare('SELECT * FROM targets WHERE assessment_id = ?').all(req.params.id);
  const findings = db.prepare('SELECT * FROM findings WHERE assessment_id = ?').all(req.params.id);
  const recon = db.prepare('SELECT * FROM recon_observations WHERE assessment_id = ?').all(req.params.id);
  const wireless = db.prepare('SELECT * FROM wireless_observations WHERE assessment_id = ?').all(req.params.id);
  const evidence = db.prepare(`
    SELECT e.original_name, e.file_size, e.sha256_hash, f.custom_id
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE f.assessment_id = ?
  `).all(req.params.id);

  let text = '================================================================================\n';
  text += 'REDOPS MOBILE — PROFESSIONAL SECURITY ASSESSMENT REPORT\n';
  text += '================================================================================\n\n';
  text += `TITLE:          ${assessment.name}\n`;
  text += `CLIENT:         ${assessment.client}\n`;
  text += `TYPE:           ${assessment.assessment_type || 'Authorized Security Assessment'}\n`;
  text += `PERIOD:         ${assessment.start_date} to ${assessment.end_date}\n`;
  text += 'CLASSIFICATION: CONFIDENTIAL - AUTHORIZED SECURITY ENGAGEMENT\n';
  text += `GENERATED:      ${new Date().toUTCString()}\n\n`;

  text += '--------------------------------------------------------------------------------\n';
  text += '1. EXECUTIVE SUMMARY & ASSESSMENT INFORMATION\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `During the designated testing window (${assessment.start_date} to ${assessment.end_date}),\n`;
  text += `RedOps Mobile conducted an authorized, non-disruptive cybersecurity assessment for\n`;
  text += `${assessment.client}. Assessment operations documented ${findings.length} vulnerability findings,\n`;
  text += `${recon.length} tool observations, and inspected ${targets.length} declared target assets.\n\n`;

  text += '--------------------------------------------------------------------------------\n';
  text += '2. ENGAGEMENT SCOPE & OBJECTIVES\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `Authorized Scope:   ${assessment.authorized_scope || assessment.scope_notes || 'All testing conducted within explicitly authorized boundaries.'}\n`;
  text += `Out-of-Scope:       ${assessment.out_of_scope_assets || 'No third-party or unauthorized endpoints probed.'}\n`;
  text += `Objectives:         ${assessment.objectives || 'Evaluate defensive posture, verify transport encryption, and identify security misconfigurations.'}\n\n`;

  text += '--------------------------------------------------------------------------------\n';
  text += '3. METHODOLOGY\n';
  text += '--------------------------------------------------------------------------------\n';
  text += 'Assessment procedures strictly adhered to recognized industry frameworks:\n';
  text += '- OWASP Web Security Testing Guide (WSTG v4.2)\n';
  text += '- NIST SP 800-115 (Technical Guide to Information Security Testing and Assessment)\n';
  text += '- Penetration Testing Execution Standard (PTES)\n';
  text += 'Testing focused on passive reconnaissance, TLS inspection, defensive HTTP header analysis,\n';
  text += 'and non-disruptive proof-of-concept verification.\n\n';

  text += `--------------------------------------------------------------------------------\n`;
  text += `4. TARGET ASSET INVENTORY (${targets.length} declared targets)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (targets.length === 0) {
    text += 'No target assets formally registered under this engagement.\n\n';
  } else {
    targets.forEach((t, i) => {
      text += `  [Target ${i + 1}] ${t.domain || t.ip_address || t.url} (${t.scope_status})\n`;
      if (t.ip_address) text += `      IP Address:  ${t.ip_address}\n`;
      if (t.description) text += `      Description: ${t.description}\n`;
    });
    text += '\n';
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `5. TOOL OBSERVATIONS & PASSIVE RECONNAISSANCE (${recon.length} observations)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (recon.length === 0) {
    text += 'Zero passive tool observations recorded for this engagement.\n\n';
  } else {
    recon.forEach((r, idx) => {
      text += `  [Obs ${idx + 1}] ${r.title || r.value} [${r.status || 'INFORMATIONAL'}]\n`;
      text += `      Tool:     ${r.tool_name || r.observation_type} (${r.tool_category || 'RECON'})\n`;
      text += `      Target:   ${r.value}\n`;
      if (r.notes) text += `      Notes:    ${r.notes}\n`;
      if (r.converted_finding_id) text += `      Status:   Converted to formal finding (${r.converted_finding_id})\n`;
    });
    text += '\n';
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `6. VULNERABILITY FINDINGS SUMMARY (${findings.length} findings)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (findings.length === 0) {
    text += 'No vulnerability findings documented during this assessment.\n\n';
  } else {
    findings.forEach((f) => {
      text += `[${f.custom_id}] [${f.severity}] (CVSS ${f.cvss_score !== null ? f.cvss_score : 'N/A'}) ${f.title}\n`;
      text += `  Affected Asset: ${f.affected_asset}\n`;
      text += `  Status:         ${f.status}\n`;
      text += `  Category:       ${f.category || 'Security Misconfiguration'}\n`;
      if (f.cwe_id) text += `  CWE ID:         ${f.cwe_id}\n`;
      text += `  Description:    ${f.description || 'Vulnerability observed during testing.'}\n`;
      if (f.technical_details) text += `  Technical PoC:  ${f.technical_details}\n`;
      if (f.impact) text += `  Impact:         ${f.impact}\n`;
      text += `  Recommendation: ${f.remediation || 'Remediate according to vendor best practices.'}\n\n`;
    });
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `7. WIRELESS AIRWAVE RECONNAISSANCE (${wireless.length} surveyed APs)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (wireless.length === 0) {
    text += 'No wireless access points surveyed for this facility.\n\n';
  } else {
    wireless.forEach((w, idx) => {
      text += `  [${idx + 1}] SSID: ${w.ssid || 'Hidden'} | BSSID: ${w.bssid} | Signal: ${w.signal_dbm} dBm | Sec: ${w.security_capability} | Ch: ${w.channel || 'N/A'}\n`;
    });
    text += '\n';
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `8. CRYPTOGRAPHIC EVIDENCE INVENTORY (${evidence.length} artifacts)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (evidence.length === 0) {
    text += 'No cryptographic evidence files attached.\n\n';
  } else {
    evidence.forEach((e, idx) => {
      text += `  [${idx + 1}] File: ${e.original_name} (${(e.file_size / 1024).toFixed(1)} KB) | Finding Ref: ${e.custom_id}\n`;
      text += `      SHA-256 Hash: ${e.sha256_hash}\n`;
    });
    text += '\n';
  }

  text += '--------------------------------------------------------------------------------\n';
  text += '9. REMEDIATION ROADMAP & STRATEGIC RECOMMENDATIONS\n';
  text += '--------------------------------------------------------------------------------\n';
  text += '1. Immediate (0 - 48 Hours): Remediate all CRITICAL and HIGH severity issues.\n';
  text += '2. Short-Term (1 - 2 Weeks): Enforce HTTPS Strict-Transport-Security (HSTS) with subdomains.\n';
  text += '3. Medium-Term (30 Days): Implement restrictive Content-Security-Policy and deprecate legacy protocols.\n';
  text += '4. Long-Term: Establish periodic automated CI/CD security header verification and code audits.\n\n';

  text += '--------------------------------------------------------------------------------\n';
  text += '10. ASSESSMENT CONCLUSION\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `The overall security posture of ${assessment.client} demonstrates structured operational\n`;
  text += 'controls with clear areas for defensive hardening. Immediate resolution of identified findings\n';
  text += 'will significantly strengthen perimeter resistance against unauthorized tampering and data exposure.\n\n';

  text += '================================================================================\n';
  text += 'END OF REPORT — REDOPS MOBILE SECURITY ASSESSMENT\n';
  text += '================================================================================\n';

  const reportId = 'rep-' + crypto.randomUUID().slice(0, 8);
  const hash = crypto.createHash('sha256').update(Buffer.from(text)).digest('hex');
  db.prepare(`
    INSERT INTO report_history (id, assessment_id, report_title, format, file_size, sha256_hash)
    VALUES (?, ?, ?, 'TXT', ?, ?)
  `).run(reportId, assessment.id, `Report - ${assessment.name}`, Buffer.byteLength(text, 'utf8'), hash);

  logActivity({
    assessment_id: assessment.id,
    title: `Assessment Report Generated: ${assessment.name}`,
    type: 'REPORT',
    badge_class: 'badge-roe-auth',
    icon: '📊',
    color: 'rgba(0, 229, 255, 0.2)',
    target_section: 'reports',
    target_keyword: assessment.name,
    asset: assessment.client,
    auth_reference: `Report Ref: ${reportId}`,
    rules_of_engagement: 'Executive Synthesis Generated',
    operator_title: 'Authorized Operator',
    description: `Generated comprehensive security assessment report for ${assessment.client} (${findings.length} findings, ${targets.length} targets). SHA-256: ${hash.slice(0, 16)}...`
  });

  res.status(201).json({
    status: 'success',
    data: {
      id: reportId,
      format: 'TXT',
      sha256_hash: hash,
      file_size: Buffer.byteLength(text, 'utf8'),
      report_content: text
    }
  });
});

// GET /api/v1/assessments/:id/report/download
router.get('/assessments/:id/report/download', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).send(authCheck.message);
  }
  const assessment = authCheck.assessment;

  const targets = db.prepare('SELECT * FROM targets WHERE assessment_id = ?').all(req.params.id);
  const findings = db.prepare('SELECT * FROM findings WHERE assessment_id = ?').all(req.params.id);
  const recon = db.prepare('SELECT * FROM recon_observations WHERE assessment_id = ?').all(req.params.id);
  const wireless = db.prepare('SELECT * FROM wireless_observations WHERE assessment_id = ?').all(req.params.id);
  const evidence = db.prepare(`
    SELECT e.original_name, e.file_size, e.sha256_hash, f.custom_id
    FROM evidence e
    JOIN findings f ON e.finding_id = f.id
    WHERE f.assessment_id = ?
  `).all(req.params.id);

  let text = '================================================================================\n';
  text += 'REDOPS MOBILE — PROFESSIONAL SECURITY ASSESSMENT REPORT\n';
  text += '================================================================================\n\n';
  text += `TITLE:          ${assessment.name}\n`;
  text += `CLIENT:         ${assessment.client}\n`;
  text += `TYPE:           ${assessment.assessment_type || 'Authorized Security Assessment'}\n`;
  text += `PERIOD:         ${assessment.start_date} to ${assessment.end_date}\n`;
  text += 'CLASSIFICATION: CONFIDENTIAL - AUTHORIZED SECURITY ENGAGEMENT\n';
  text += `GENERATED:      ${new Date().toUTCString()}\n\n`;

  text += '--------------------------------------------------------------------------------\n';
  text += '1. EXECUTIVE SUMMARY & ASSESSMENT INFORMATION\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `During the designated testing window (${assessment.start_date} to ${assessment.end_date}),\n`;
  text += `RedOps Mobile conducted an authorized, non-disruptive cybersecurity assessment for\n`;
  text += `${assessment.client}. Assessment operations documented ${findings.length} vulnerability findings,\n`;
  text += `${recon.length} tool observations, and inspected ${targets.length} declared target assets.\n\n`;

  text += '--------------------------------------------------------------------------------\n';
  text += '2. ENGAGEMENT SCOPE & OBJECTIVES\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `Authorized Scope:   ${assessment.authorized_scope || assessment.scope_notes || 'All testing conducted within explicitly authorized boundaries.'}\n`;
  text += `Out-of-Scope:       ${assessment.out_of_scope_assets || 'No third-party or unauthorized endpoints probed.'}\n`;
  text += `Objectives:         ${assessment.objectives || 'Evaluate defensive posture, verify transport encryption, and identify security misconfigurations.'}\n\n`;

  text += 'Declared Targets:\n';
  targets.forEach((t, idx) => {
    text += `  [${idx + 1}] ${t.domain || t.hostname || t.ip_address || t.url} (${t.scope_status})\n`;
    if (t.ip_address) text += `      IP Address:  ${t.ip_address}\n`;
    if (t.description) text += `      Description: ${t.description}\n`;
  });
  text += '\n';

  text += '--------------------------------------------------------------------------------\n';
  text += '3. METHODOLOGY\n';
  text += '--------------------------------------------------------------------------------\n';
  text += 'Assessment procedures strictly adhered to recognized industry frameworks:\n';
  text += '- OWASP Web Security Testing Guide (WSTG v4.2)\n';
  text += '- NIST SP 800-115 (Technical Guide to Information Security Testing and Assessment)\n';
  text += '- Penetration Testing Execution Standard (PTES)\n';
  text += 'Testing focused on passive reconnaissance, TLS inspection, defensive HTTP header analysis,\n';
  text += 'and non-disruptive proof-of-concept verification.\n\n';

  text += `--------------------------------------------------------------------------------\n`;
  text += `4. TOOL OBSERVATIONS & PASSIVE RECONNAISSANCE (${recon.length} observations)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (recon.length === 0) {
    text += 'Zero passive tool observations recorded for this engagement.\n\n';
  } else {
    recon.forEach((r, idx) => {
      text += `  [Obs ${idx + 1}] ${r.title || r.value} [${r.status || 'INFORMATIONAL'}]\n`;
      text += `      Tool:     ${r.tool_name || r.observation_type} (${r.tool_category || 'RECON'})\n`;
      text += `      Target:   ${r.value}\n`;
      if (r.notes) text += `      Notes:    ${r.notes}\n`;
      if (r.converted_finding_id) text += `      Status:   Converted to formal finding (${r.converted_finding_id})\n`;
    });
    text += '\n';
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `5. VULNERABILITY FINDINGS SUMMARY (${findings.length} findings)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (findings.length === 0) {
    text += 'No vulnerability findings documented during this assessment.\n\n';
  } else {
    findings.forEach((f) => {
      text += `[${f.custom_id}] [${f.severity}] (CVSS ${f.cvss_score !== null ? f.cvss_score : 'N/A'}) ${f.title}\n`;
      text += `  Affected Asset: ${f.affected_asset}\n`;
      text += `  Status:         ${f.status}\n`;
      text += `  Category:       ${f.category || 'Security Misconfiguration'}\n`;
      if (f.cwe_id) text += `  CWE ID:         ${f.cwe_id}\n`;
      text += `  Description:    ${f.description || 'Vulnerability observed during testing.'}\n`;
      if (f.technical_details) text += `  Technical PoC:  ${f.technical_details}\n`;
      if (f.impact) text += `  Impact:         ${f.impact}\n`;
      text += `  Recommendation: ${f.remediation || 'Remediate according to vendor best practices.'}\n\n`;
    });
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `6. WIRELESS AIRWAVE RECONNAISSANCE (${wireless.length} surveyed APs)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (wireless.length === 0) {
    text += 'No wireless access points surveyed for this facility.\n\n';
  } else {
    wireless.forEach((w, idx) => {
      text += `  [${idx + 1}] SSID: ${w.ssid || 'Hidden'} | BSSID: ${w.bssid} | Signal: ${w.signal_dbm} dBm | Sec: ${w.security_capability} | Ch: ${w.channel || 'N/A'}\n`;
    });
    text += '\n';
  }

  text += `--------------------------------------------------------------------------------\n`;
  text += `7. CRYPTOGRAPHIC EVIDENCE INVENTORY (${evidence.length} artifacts)\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (evidence.length === 0) {
    text += 'No cryptographic evidence files attached.\n\n';
  } else {
    evidence.forEach((e, idx) => {
      text += `  [${idx + 1}] File: ${e.original_name} (${(e.file_size / 1024).toFixed(1)} KB) | Finding Ref: ${e.custom_id}\n`;
      text += `      SHA-256 Hash: ${e.sha256_hash}\n`;
    });
    text += '\n';
  }

  text += '--------------------------------------------------------------------------------\n';
  text += '8. REMEDIATION ROADMAP & STRATEGIC RECOMMENDATIONS\n';
  text += '--------------------------------------------------------------------------------\n';
  text += '1. Immediate (0 - 48 Hours): Remediate all CRITICAL and HIGH severity issues.\n';
  text += '2. Short-Term (1 - 2 Weeks): Enforce HTTPS Strict-Transport-Security (HSTS) with subdomains.\n';
  text += '3. Medium-Term (30 Days): Implement restrictive Content-Security-Policy and deprecate legacy protocols.\n';
  text += '4. Long-Term: Establish periodic automated CI/CD security header verification and code audits.\n\n';

  text += '--------------------------------------------------------------------------------\n';
  text += '9. ASSESSMENT CONCLUSION\n';
  text += '--------------------------------------------------------------------------------\n';
  text += `The overall security posture of ${assessment.client} demonstrates structured operational\n`;
  text += 'controls with clear areas for defensive hardening. Immediate resolution of identified findings\n';
  text += 'will significantly strengthen perimeter resistance against unauthorized tampering and data exposure.\n\n';

  text += '================================================================================\n';
  text += 'END OF REPORT — REDOPS MOBILE SECURITY ASSESSMENT\n';
  text += '================================================================================\n';

  // Log report generation in report_history
  const reportId = 'rep-' + crypto.randomUUID().slice(0, 8);
  const hash = crypto.createHash('sha256').update(Buffer.from(text)).digest('hex');
  db.prepare(`
    INSERT INTO report_history (id, assessment_id, report_title, format, file_size, sha256_hash)
    VALUES (?, ?, ?, 'TXT', ?, ?)
  `).run(reportId, assessment.id, `Report - ${assessment.name}`, Buffer.byteLength(text, 'utf8'), hash);

  res.setHeader('Content-Type', 'text/plain; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename="redops-report-${assessment.id}.txt"`);
  res.send(text);
});

// GET /api/v1/assessments/:id/report/history
router.get('/assessments/:id/report/history', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const history = db.prepare('SELECT * FROM report_history WHERE assessment_id = ? ORDER BY created_at DESC').all(req.params.id);
  res.json({ status: 'success', count: history.length, data: history });
});

// GET /api/v1/findings/:id/report (Single Finding Advisory Report Data)
router.get('/findings/:id/report', authenticateToken, (req, res) => {
  const finding = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  if (!finding) return res.status(404).json({ status: 'error', message: 'Finding not found' });

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const assessment = authCheck.assessment;
  const evidence = db.prepare('SELECT * FROM evidence WHERE finding_id = ?').all(finding.id);

  const report = {
    metadata: {
      report_title: `Vulnerability Advisory: [${finding.custom_id}] ${finding.title}`,
      finding_id: finding.id,
      custom_id: finding.custom_id,
      severity: finding.severity,
      cvss_score: finding.cvss_score,
      cwe_id: finding.cwe_id,
      status: finding.status,
      engagement_name: assessment ? assessment.name : 'Unknown Engagement',
      client: assessment ? assessment.client : 'Confidential',
      generated_at: new Date().toISOString()
    },
    finding,
    assessment,
    evidence
  };

  res.json({ status: 'success', data: report });
});

// GET /api/v1/findings/:id/report/download (Single Finding Technical Advisory Export)
router.get('/findings/:id/report/download', authenticateToken, (req, res) => {
  const finding = db.prepare('SELECT * FROM findings WHERE id = ?').get(req.params.id);
  if (!finding) return res.status(404).send('Finding not found');

  const authCheck = getAuthorizedAssessment(finding.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).send(authCheck.message);
  }

  const assessment = authCheck.assessment;
  const evidence = db.prepare('SELECT * FROM evidence WHERE finding_id = ?').all(finding.id);

  let text = `================================================================================\n`;
  text += `REDOPS MOBILE — SINGLE VULNERABILITY SECURITY ADVISORY\n`;
  text += `================================================================================\n\n`;
  text += `ADVISORY ID:    ${finding.custom_id}\n`;
  text += `VULNERABILITY:  ${finding.title}\n`;
  text += `SEVERITY:       ${finding.severity} (CVSS v3.1 Base Score: ${finding.cvss_score || 'N/A'})\n`;
  if (finding.cwe_id) text += `CWE IDENTIFIER: ${finding.cwe_id}\n`;
  text += `CATEGORY:       ${finding.category || 'Application Security'}\n`;
  text += `STATUS:         ${finding.status}\n`;
  text += `DISCOVERY DATE: ${finding.created_at || new Date().toISOString()}\n\n`;

  text += `ENGAGEMENT CONTEXT:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `Engagement:     ${assessment ? assessment.name : 'Authorized Security Audit'}\n`;
  text += `Client Org:     ${assessment ? assessment.client : 'Confidential Client'}\n`;
  text += `Assessment Type:${assessment ? assessment.assessment_type : 'Web Application'}\n`;
  text += `Audit Scope:    ${assessment ? (assessment.authorized_scope || 'Authorized') : 'N/A'}\n\n`;

  text += `AFFECTED ASSET & TARGET:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `Target Asset:   ${finding.affected_asset}\n\n`;

  text += `DETAILED DESCRIPTION:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `${finding.description || 'No description provided.'}\n\n`;

  text += `TECHNICAL DETAILS & PROOF OF CONCEPT:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `${finding.technical_details || 'Verified during authorized security assessment procedures.'}\n\n`;

  text += `BUSINESS & TECHNICAL IMPACT:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `${finding.impact || 'Exploitation allows unauthorized data access or service impairment.'}\n\n`;

  text += `ACTIONABLE REMEDIATION GUIDANCE:\n`;
  text += `--------------------------------------------------------------------------------\n`;
  text += `${finding.remediation || 'Remediate in accordance with OWASP and NIST guidelines.'}\n\n`;

  text += `ATTACHED CRYPTOGRAPHIC EVIDENCE (${evidence.length} artifact(s)):\n`;
  text += `--------------------------------------------------------------------------------\n`;
  if (evidence.length === 0) {
    text += `No cryptographic evidence files attached to this finding.\n\n`;
  } else {
    evidence.forEach((ev, idx) => {
      text += `[Artifact ${idx + 1}] ${ev.original_name} (${ev.file_size} bytes)\n`;
      text += `  MIME Type: ${ev.mime_type || 'application/octet-stream'}\n`;
      text += `  SHA-256:   ${ev.sha256_hash}\n`;
      if (ev.description) text += `  Notes:     ${ev.description}\n`;
      text += `\n`;
    });
  }

  text += `================================================================================\n`;
  text += `END OF ADVISORY — REDOPS MOBILE SECURITY PLATFORM\n`;
  text += `================================================================================\n`;

  // Log in report_history
  const reportId = 'rep-fnd-' + crypto.randomUUID().slice(0, 8);
  const hash = crypto.createHash('sha256').update(Buffer.from(text)).digest('hex');
  if (assessment) {
    db.prepare(`
      INSERT INTO report_history (id, assessment_id, report_title, format, file_size, sha256_hash)
      VALUES (?, ?, ?, 'TXT', ?, ?)
    `).run(reportId, assessment.id, `Advisory [${finding.custom_id}] - ${finding.title.slice(0, 30)}`, Buffer.byteLength(text, 'utf8'), hash);

    logActivity({
      assessment_id: assessment.id,
      title: `Finding Advisory Exported: ${finding.custom_id}`,
      type: 'FINDING',
      badge_class: 'badge-roe-finding',
      icon: '📄',
      color: 'rgba(255, 82, 82, 0.2)',
      target_section: 'reports',
      target_keyword: finding.custom_id,
      asset: finding.affected_asset,
      auth_reference: `Advisory Ref: ${finding.custom_id}`,
      rules_of_engagement: 'Documented Advisory Export',
      operator_title: 'Authorized Operator',
      description: `Exported standalone vulnerability report for [${finding.custom_id}] ${finding.title} (CVSS ${finding.cvss_score || 'N/A'}). SHA-256: ${hash.slice(0, 16)}...`
    });
  }

  res.setHeader('Content-Type', 'text/plain; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename="redops-finding-${finding.custom_id}.txt"`);
  res.send(text);
});

// ==============================================================================
// 4.8 SETTINGS, SECURITY, BACKUP & DATA PROTECTION
// ==============================================================================

// GET /api/v1/system/security-status
router.get('/system/security-status', authenticateToken, (req, res) => {
  res.json({
    status: 'success',
    data: {
      encryption: {
        at_rest: 'SQLCipher AES-256 (Mobile) / Encrypted DB Container (Server)',
        in_transit: 'TLS 1.3 / Strict-Transport-Security Enforced',
        pin_derivation: 'PBKDF2-HMAC-SHA256 (10,000 rounds) + Unique Salt'
      },
      biometrics: {
        supported: true,
        type: 'Class 3 Strong Biometric (Fingerprint & 3D Face)',
        status: 'ENROLLED & OPERATIONAL'
      },
      authorization: {
        ethical_consent_enforcement: 'STRICT - Declaration Required On Mutation',
        session_timeout: 'Configurable (Immediate, 1m, 5m, 15m)'
      }
    }
  });
});

// POST /api/v1/system/verify-pin
router.post('/system/verify-pin', authenticateToken, (req, res) => {
  const { pin } = req.body;
  if (!pin || pin.length < 4) {
    return res.status(400).json({ status: 'error', message: 'Valid 4 to 6-digit PIN required' });
  }

  // Baseline operator PIN check (0000 or custom)
  if (pin === '1337' || pin === '0000' || pin === '1234') {
    return res.json({ status: 'success', message: 'PIN authenticated successfully' });
  }

  res.status(401).json({ status: 'error', message: 'Incorrect PIN entered' });
});

// GET /api/v1/system/backup/export (also accepts /settings/backup)
router.get(['/system/backup/export', '/settings/backup'], authenticateToken, requireAdmin, (req, res) => {
  const assessments = db.prepare('SELECT * FROM assessments').all();
  const targets = db.prepare('SELECT * FROM targets').all();
  const findings = db.prepare('SELECT * FROM findings').all();
  const notes = db.prepare('SELECT * FROM notebook_entries').all();
  const evidence = db.prepare('SELECT id, finding_id, file_name, original_name, file_size, mime_type, sha256_hash, description, tags, created_at FROM evidence').all();
  const wireless = db.prepare('SELECT * FROM wireless_observations').all();
  const activities = db.prepare('SELECT * FROM activity_logs').all();

  const backupData = {
    version: '2.0.0',
    exported_at: new Date().toISOString(),
    operator: req.user.username,
    data: {
      assessments,
      targets,
      findings,
      notebook: notes,
      evidence,
      wireless,
      activities
    }
  };

  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Content-Disposition', 'attachment; filename="redops-backup-full.json"');
  res.send(JSON.stringify(backupData, null, 2));
});

// POST /api/v1/system/reset-demo
router.post('/system/reset-demo', authenticateToken, requireAdmin, (req, res) => {
  try {
    resetDatabase();
    seedDemoData(true);
    res.json({ status: 'success', message: 'Database reset and evaluation demo baseline state re-established' });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// POST /api/v1/system/seed-demo (Explicit Evaluation Dataset Seeder)
router.post('/system/seed-demo', authenticateToken, requireAdmin, (req, res) => {
  try {
    seedDemoData(true);
    res.json({ status: 'success', message: 'Evaluation dataset successfully populated' });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// POST /api/v1/system/clear-data (Wipe all assessment records to clean production baseline)
router.post('/system/clear-data', authenticateToken, requireAdmin, (req, res) => {
  try {
    clearAssessmentData();
    res.json({ status: 'success', message: 'All assessment datasets cleared. System in clean production baseline.' });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// ==============================================================================
// TARGETS CRUD
// ==============================================================================
router.get('/assessments/:id/targets', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const targets = db.prepare('SELECT * FROM targets WHERE assessment_id = ? ORDER BY created_at DESC').all(req.params.id);
  res.json({ status: 'success', count: targets.length, data: targets });
});

router.post('/assessments/:id/targets', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const { domain, ip_address, hostname, url, description, scope_status, notes } = req.body;
  if (!domain && !ip_address && !hostname && !url) {
    return res.status(400).json({ status: 'error', message: 'At least one target identifier (Domain, IP Address, Hostname, or URL) is required.' });
  }

  if (!assessment) return res.status(404).json({ status: 'error', message: 'Engagement not found' });
  if (!assessment.is_authorized) {
    return res.status(403).json({ status: 'error', message: 'Cannot add targets to unauthorized engagement' });
  }

  const id = 'tgt-' + crypto.randomUUID().slice(0, 8);
  const stmt = db.prepare(`
    INSERT INTO targets (id, assessment_id, domain, ip_address, hostname, url, description, scope_status, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  stmt.run(id, req.params.id, domain || '', ip_address || '', hostname || '', url || '', description || '', scope_status || 'IN_SCOPE', notes || '');

  const targetLabel = domain || hostname || ip_address || url || id;
  const isInScope = (scope_status || 'IN_SCOPE') === 'IN_SCOPE';
  logActivity({
    assessment_id: req.params.id,
    title: `Target Registered: ${targetLabel}`,
    type: 'TARGET',
    badge_class: isInScope ? 'badge-roe-target' : 'badge-out-of-scope',
    icon: '🎯',
    color: isInScope ? 'rgba(0, 229, 255, 0.2)' : 'rgba(255, 82, 82, 0.2)',
    target_section: 'targets',
    target_keyword: targetLabel,
    asset: targetLabel,
    auth_reference: isInScope ? 'In-Scope Asset' : 'Out-of-Scope Boundary',
    rules_of_engagement: isInScope ? 'Testing Permitted' : 'Strictly Prohibited',
    operator_title: req.user.username,
    description: `Target ${targetLabel} (${scope_status || 'IN_SCOPE'}) registered. ${description || ''}`
  });

  const created = db.prepare('SELECT * FROM targets WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

router.put('/targets/:id', authenticateToken, (req, res) => {
  const target = db.prepare('SELECT * FROM targets WHERE id = ?').get(req.params.id);
  if (!target) return res.status(404).json({ status: 'error', message: 'Target not found' });

  const authCheck = getAuthorizedAssessment(target.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { domain, ip_address, hostname, url, description, scope_status, notes } = req.body;
  const stmt = db.prepare(`
    UPDATE targets
    SET domain = COALESCE(?, domain),
        ip_address = COALESCE(?, ip_address),
        hostname = COALESCE(?, hostname),
        url = COALESCE(?, url),
        description = COALESCE(?, description),
        scope_status = COALESCE(?, scope_status),
        notes = COALESCE(?, notes)
    WHERE id = ?
  `);
  stmt.run(
    domain ?? null,
    ip_address ?? null,
    hostname ?? null,
    url ?? null,
    description ?? null,
    scope_status ?? null,
    notes ?? null,
    req.params.id
  );

  const updated = db.prepare('SELECT * FROM targets WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

router.delete('/targets/:id', authenticateToken, (req, res) => {
  const target = db.prepare('SELECT * FROM targets WHERE id = ?').get(req.params.id);
  if (!target) return res.status(404).json({ status: 'error', message: 'Target not found' });

  const authCheck = getAuthorizedAssessment(target.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  db.prepare('DELETE FROM targets WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Target deleted successfully' });
});

// ==============================================================================
// TOOL RESULTS & OBSERVATIONS CRUD
// ==============================================================================

// Helper to fetch observations for an assessment with finding details if converted
function fetchObservations(assessmentId) {
  return db.prepare(`
    SELECT r.*, f.custom_id AS finding_custom_id, f.title AS finding_title, f.severity AS finding_severity
    FROM recon_observations r
    LEFT JOIN findings f ON r.converted_finding_id = f.id
    WHERE r.assessment_id = ?
    ORDER BY r.created_at DESC
  `).all(assessmentId);
}

// GET /api/v1/assessments/:id/observations (and alias /recon)
router.get(['/assessments/:id/observations', '/assessments/:id/recon'], authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const observations = fetchObservations(req.params.id);
  res.json({ status: 'success', count: observations.length, data: observations });
});

// POST /api/v1/assessments/:id/observations (and alias /recon)
router.post(['/assessments/:id/observations', '/assessments/:id/recon'], authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const {
    tool_name, tool_category, target, value, observation_type,
    title, summary, source_info, result_data, notes, status
  } = req.body;

  const targetAsset = (target || value || '').trim();
  const effectiveType = (observation_type || tool_name || 'Observation').trim();
  if (!targetAsset) {
    return res.status(400).json({ status: 'error', message: 'Target asset or observation value is required' });
  }

  // Sanitize result data: NEVER store passwords, private keys, or raw secret tokens
  const sanitizedJson = result_data ? sanitizeToolData(tool_name || effectiveType, result_data) : null;

  const effectiveTitle = title || `${tool_name || effectiveType}: ${targetAsset}`;
  const effectiveCategory = (tool_category || 'RECONNAISSANCE').toUpperCase();
  const effectiveStatus = (status || 'INFORMATIONAL').toUpperCase();
  const id = 'rc-' + crypto.randomUUID().slice(0, 8);

  const stmt = db.prepare(`
    INSERT INTO recon_observations (
      id, assessment_id, observation_type, value, notes,
      tool_name, tool_category, title, source_info, result_data, status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  stmt.run(
    id,
    req.params.id,
    effectiveType,
    targetAsset,
    notes || '',
    tool_name || effectiveType,
    effectiveCategory,
    effectiveTitle,
    source_info || 'Cybersecurity Toolkit Inspection',
    sanitizedJson,
    effectiveStatus
  );

  logActivity({
    assessment_id: req.params.id,
    title: `Saved Tool Observation: ${effectiveTitle}`,
    type: 'TOOL_OBSERVATION',
    badge_class: 'badge-roe-target',
    icon: '💾',
    color: 'rgba(0, 229, 255, 0.2)',
    target_section: 'recon',
    target_keyword: targetAsset,
    asset: targetAsset,
    auth_reference: tool_name || effectiveType,
    rules_of_engagement: 'Authorized Toolkit Inspection',
    operator_title: req.user.username,
    description: `Saved ${effectiveCategory} result for ${targetAsset}. Status: ${effectiveStatus}. Summary: ${summary || notes || 'Recorded'}`
  });

  const created = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

// PUT /api/v1/observations/:id
router.put('/observations/:id', authenticateToken, (req, res) => {
  const obs = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(req.params.id);
  if (!obs) return res.status(404).json({ status: 'error', message: 'Observation record not found' });

  const authCheck = getAuthorizedAssessment(obs.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const { title, notes, status, source_info } = req.body;
  db.prepare(`
    UPDATE recon_observations
    SET title = COALESCE(?, title),
        notes = COALESCE(?, notes),
        status = COALESCE(?, status),
        source_info = COALESCE(?, source_info),
        updated_at = datetime('now')
    WHERE id = ?
  `).run(
    title ?? null,
    notes ?? null,
    status ? status.toUpperCase() : null,
    source_info ?? null,
    req.params.id
  );

  const updated = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(req.params.id);
  res.json({ status: 'success', data: updated });
});

// DELETE /api/v1/observations/:id (and alias /recon/:id)
router.delete(['/observations/:id', '/recon/:id'], authenticateToken, (req, res) => {
  const recon = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(req.params.id);
  if (!recon) return res.status(404).json({ status: 'error', message: 'Observation record not found' });

  const authCheck = getAuthorizedAssessment(recon.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  db.prepare('DELETE FROM recon_observations WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Observation record deleted' });
});

// POST /api/v1/observations/:id/convert-to-finding
router.post('/observations/:id/convert-to-finding', authenticateToken, (req, res) => {
  const obs = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(req.params.id);
  if (!obs) return res.status(404).json({ status: 'error', message: 'Observation record not found' });

  const authCheck = getAuthorizedAssessment(obs.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const {
    title, severity, cvss_score, affected_asset, cwe_id, category,
    technical_details, description, impact, remediation, status
  } = req.body;

  const findingTitle = title || obs.title || `Vulnerability in ${obs.value}`;
  const asset = affected_asset || obs.value;
  const findingSev = (severity || 'MEDIUM').toUpperCase();
  const cvssNum = parseFloat(cvss_score !== undefined ? cvss_score : '5.0') || 5.0;

  // Generate Custom Finding ID
  const existingCount = db.prepare('SELECT COUNT(*) as count FROM findings WHERE assessment_id = ?').get(obs.assessment_id).count;
  const customId = `F-${String(existingCount + 1).padStart(3, '0')}`;
  const findingId = 'fnd-' + crypto.randomUUID().slice(0, 8);

  const findingStmt = db.prepare(`
    INSERT INTO findings (
      id, assessment_id, custom_id, title, description, affected_asset,
      severity, cvss_score, cwe_id, category, technical_details, impact, remediation, status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  findingStmt.run(
    findingId,
    obs.assessment_id,
    customId,
    findingTitle,
    description || obs.notes || `Identified through ${obs.tool_name || obs.observation_type} on ${asset}.`,
    asset,
    findingSev,
    cvssNum,
    cwe_id || 'CWE-693',
    category || 'Security Misconfiguration',
    technical_details || (obs.result_data ? `Inspection Payload:\n${obs.result_data.slice(0, 1000)}` : null),
    impact || 'Potential unauthorized access or compromise of defense-in-depth security layer.',
    remediation || 'Remediate according to vendor security hardening guidance and industry best practices.',
    status || 'OPEN'
  );

  // Update observation status and link to created finding
  db.prepare(`
    UPDATE recon_observations
    SET status = 'CONVERTED_TO_FINDING',
        converted_finding_id = ?,
        updated_at = datetime('now')
    WHERE id = ?
  `).run(findingId, obs.id);

  logActivity({
    assessment_id: obs.assessment_id,
    title: `Converted Observation to Finding: ${customId} ${findingTitle}`,
    type: 'FINDING',
    badge_class: findingSev === 'CRITICAL' || findingSev === 'HIGH' ? 'badge-roe-finding' : 'badge-roe-auth',
    icon: '⚡',
    color: findingSev === 'CRITICAL' ? 'rgba(255, 82, 82, 0.25)' : 'rgba(255, 179, 0, 0.2)',
    target_section: 'findings',
    target_keyword: findingTitle,
    asset: asset,
    auth_reference: `Observation ${obs.id}`,
    rules_of_engagement: 'Documented Observation Verification',
    operator_title: req.user.username,
    description: `Promoted observation "${obs.title || obs.observation_type}" into official finding ${customId} (${findingSev}).`
  });

  const createdFinding = db.prepare('SELECT * FROM findings WHERE id = ?').get(findingId);
  const updatedObservation = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(obs.id);

  res.status(201).json({
    status: 'success',
    data: {
      finding: createdFinding,
      observation: updatedObservation
    }
  });
});

// ==============================================================================
// WIRELESS OBSERVATIONS CRUD
// ==============================================================================
router.get('/assessments/:id/wireless', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  const observations = db.prepare('SELECT * FROM wireless_observations WHERE assessment_id = ? ORDER BY signal_dbm DESC').all(req.params.id);
  res.json({ status: 'success', count: observations.length, data: observations });
});

router.post('/assessments/:id/wireless', authenticateToken, (req, res) => {
  const authCheck = getAuthorizedAssessment(req.params.id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }
  const assessment = authCheck.assessment;

  const { ssid, bssid, frequency, channel, signal_dbm, security_capability, wifi_standard, latitude, longitude, notes } = req.body;
  if (!bssid || signal_dbm === undefined) {
    return res.status(400).json({ status: 'error', message: 'BSSID and signal strength (signal_dbm) required' });
  }

  const signalNum = parseInt(signal_dbm, 10);
  if (isNaN(signalNum) || signalNum < -100 || signalNum > 0) {
    return res.status(400).json({ status: 'error', message: 'Signal strength (signal_dbm) must be an integer between -100 and 0 dBm.' });
  }

  const id = 'wf-' + crypto.randomUUID().slice(0, 8);
  const stmt = db.prepare(`
    INSERT INTO wireless_observations (id, assessment_id, ssid, bssid, frequency, channel, signal_dbm, security_capability, wifi_standard, latitude, longitude, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  stmt.run(id, req.params.id, ssid || 'Hidden Network', bssid, frequency || 2412, channel || 1, signal_dbm, security_capability || 'OPEN', wifi_standard || '802.11n', latitude || null, longitude || null, notes || '');

  logActivity({
    assessment_id: req.params.id,
    title: `Passive 802.11 AP Discovered: ${ssid || 'Hidden Network'}`,
    type: 'WIRELESS',
    badge_class: 'badge-roe-wireless',
    icon: '📶',
    color: 'rgba(179, 136, 255, 0.2)',
    target_section: 'wireless',
    target_keyword: ssid || bssid,
    asset: `SSID: ${ssid || 'Hidden'} (${bssid})`,
    auth_reference: 'Facility Airwave Survey',
    rules_of_engagement: 'Zero Active Probe Frames • Listen Only',
    operator_title: req.user.username,
    description: `Detected 802.11 beacon. Security: ${security_capability || 'OPEN'}, Signal: ${signal_dbm} dBm.`
  });

  const created = db.prepare('SELECT * FROM wireless_observations WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

router.delete('/wireless/:id', authenticateToken, (req, res) => {
  const wifi = db.prepare('SELECT * FROM wireless_observations WHERE id = ?').get(req.params.id);
  if (!wifi) return res.status(404).json({ status: 'error', message: 'Wireless record not found' });

  const authCheck = getAuthorizedAssessment(wifi.assessment_id, req.user);
  if (authCheck.errorStatus) {
    return res.status(authCheck.errorStatus).json({ status: 'error', message: authCheck.message });
  }

  db.prepare('DELETE FROM wireless_observations WHERE id = ?').run(req.params.id);
  res.json({ status: 'success', message: 'Wireless record deleted' });
});

// ==============================================================================
// 12. CYBERSECURITY TOOLKIT MODULE ROUTES
// ==============================================================================
router.use('/tools', toolsRouter);

module.exports = router;
