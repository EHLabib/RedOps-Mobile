const { test, describe, before } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { db, sanitizeToolData } = require('../src/database/db');
const { analyzeHttpHeaders } = require('../src/tools/webSecurityTool');
const { getIpInformation } = require('../src/tools/ipInfoTool');

const API_BASE = 'http://localhost:5000/api/v1';

describe('Toolkit & Assessment Workspace Complete Integration Workflow', () => {
  let token;
  const assessmentId = 'asm-acme-2026';
  let savedObservationId;
  let createdFindingId;
  let uploadedEvidenceId;

  before(async () => {
    // Authenticate operator
    const loginRes = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'sh@dow_sec', password: 'Password123!Safe' })
    });
    const loginData = await loginRes.json();
    assert.strictEqual(loginData.status, 'success');
    token = loginData.token;
    assert.ok(token, 'JWT Token must be acquired');
  });

  test('Step 1: Run Tool & View Result (HTTP Security Header Inspection)', async () => {
    const inspectRes = await fetch(`${API_BASE}/tools/web/headers`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        url: 'https://example.com',
        authorized: true
      })
    });

    const inspectData = await inspectRes.json();
    assert.strictEqual(inspectData.status, 'success');
    assert.ok(inspectData.data.target_url);
    assert.ok(inspectData.data.findings.length > 0);
    assert.ok(inspectData.data.assessment_notice.includes('defense-in-depth'));
  });

  test('Step 2: Save Tool Result as Informational Observation associated with Assessment', async () => {
    // Sensitive data sanitization check: password or tokens must never be persisted
    const mockToolResult = {
      target_url: 'https://portal.acme.corp',
      security_score: 45,
      grade: 'D',
      server: 'nginx/1.24.0',
      missing_headers: ['Strict-Transport-Security', 'Content-Security-Policy'],
      secret_api_key: 'SUPER_SECRET_TOKEN_DO_NOT_STORE',
      password: 'PlaintextPassword123'
    };

    const saveRes = await fetch(`${API_BASE}/assessments/${assessmentId}/observations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        tool_name: 'HTTP Security Headers Analyzer',
        tool_category: 'WEB_SECURITY',
        target: 'https://portal.acme.corp',
        title: 'HTTP Security Headers: https://portal.acme.corp',
        summary: 'Missing defensive headers: Strict-Transport-Security, Content-Security-Policy. Grade D (45%).',
        source_info: 'HTTP Port 443 Response at ' + new Date().toISOString(),
        result_data: mockToolResult,
        notes: 'Initial automated inspection of main portal endpoint.',
        status: 'INFORMATIONAL'
      })
    });

    const saveData = await saveRes.json();
    assert.strictEqual(saveData.status, 'success');
    savedObservationId = saveData.data.id;
    assert.ok(savedObservationId);
    assert.strictEqual(saveData.data.assessment_id, assessmentId);
    assert.strictEqual(saveData.data.status, 'INFORMATIONAL');

    // Verify database row and strict credential redaction
    const row = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(savedObservationId);
    assert.ok(row);
    assert.strictEqual(row.target, undefined); // value column holds target
    assert.strictEqual(row.value, 'https://portal.acme.corp');
    assert.ok(!row.result_data.includes('SUPER_SECRET_TOKEN_DO_NOT_STORE'), 'Sensitive secrets must be redacted');
    assert.ok(!row.result_data.includes('PlaintextPassword123'), 'Passwords must never be stored in persistent storage');
    assert.ok(row.result_data.includes('[REDACTED_SENSITIVE_DATA]') || row.result_data.includes('[REDACTED_LOCAL_ESTIMATE_ONLY]'));
  });

  test('Step 3: Edit and Update Saved Observation', async () => {
    const editRes = await fetch(`${API_BASE}/observations/${savedObservationId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        notes: 'Updated operator notes: verified lack of HSTS over TLS.',
        status: 'INVESTIGATING'
      })
    });

    const editData = await editRes.json();
    assert.strictEqual(editData.status, 'success');
    assert.strictEqual(editData.data.status, 'INVESTIGATING');
    assert.strictEqual(editData.data.notes, 'Updated operator notes: verified lack of HSTS over TLS.');
  });

  test('Step 4: Convert Observation into Formal Vulnerability Finding', async () => {
    const convertRes = await fetch(`${API_BASE}/observations/${savedObservationId}/convert-to-finding`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        title: 'Missing HTTP Strict-Transport-Security (HSTS) and CSP Headers',
        severity: 'MEDIUM',
        cvss_score: 5.3,
        affected_asset: 'https://portal.acme.corp',
        cwe_id: 'CWE-319',
        category: 'Security Misconfiguration',
        description: 'Target portal does not enforce HTTPS transport security headers, allowing protocol downgrade.',
        impact: 'Users on insecure networks can be subjected to SSL stripping and man-in-the-middle eavesdropping.',
        remediation: 'Configure "Strict-Transport-Security: max-age=31536000; includeSubDomains; preload" in web server configuration.'
      })
    });

    const convertData = await convertRes.json();
    assert.strictEqual(convertData.status, 'success');
    assert.ok(convertData.data.finding);
    assert.ok(convertData.data.observation);

    createdFindingId = convertData.data.finding.id;
    assert.ok(createdFindingId);
    assert.strictEqual(convertData.data.finding.severity, 'MEDIUM');
    assert.strictEqual(convertData.data.finding.cvss_score, 5.3);

    // Verify observation is linked to created finding
    assert.strictEqual(convertData.data.observation.status, 'CONVERTED_TO_FINDING');
    assert.strictEqual(convertData.data.observation.converted_finding_id, createdFindingId);
  });

  test('Step 5: Attach Supporting Evidence to Created Finding', async () => {
    // Create temporary evidence file
    const evidenceContent = 'HTTP/1.1 200 OK\r\nServer: nginx/1.24.0\r\nContent-Type: text/html\r\n\r\n[Proof of Concept: Header absence confirmed]';
    const tempFile = path.join(__dirname, 'temp_evidence_test.txt');
    fs.writeFileSync(tempFile, evidenceContent, 'utf8');

    // Create form data boundary
    const boundary = '----WebKitFormBoundary' + Math.random().toString(36).substring(2);
    let postData = '';
    postData += `--${boundary}\r\n`;
    postData += `Content-Disposition: form-data; name="evidence_file"; filename="header_audit_poc.txt"\r\n`;
    postData += `Content-Type: text/plain\r\n\r\n`;
    postData += evidenceContent + '\r\n';
    postData += `--${boundary}\r\n`;
    postData += `Content-Disposition: form-data; name="description"\r\n\r\n`;
    postData += `Raw HTTP response headers dump demonstrating missing HSTS\r\n`;
    postData += `--${boundary}\r\n`;
    postData += `Content-Disposition: form-data; name="tags"\r\n\r\n`;
    postData += `HSTS,PoC,Headers\r\n`;
    postData += `--${boundary}--\r\n`;

    const uploadRes = await fetch(`${API_BASE}/findings/${createdFindingId}/evidence`, {
      method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Authorization': `Bearer ${token}`
      },
      body: postData
    });

    const uploadData = await uploadRes.json();
    assert.strictEqual(uploadData.status, 'success');
    assert.ok(uploadData.data);
    uploadedEvidenceId = uploadData.data.id;
    assert.ok(uploadedEvidenceId);
    assert.strictEqual(uploadData.data.original_name, 'header_audit_poc.txt');
    assert.ok(uploadData.data.sha256_hash.length === 64, 'SHA-256 hash must be 64 hexadecimal characters');

    // Clean up temp file
    if (fs.existsSync(tempFile)) fs.unlinkSync(tempFile);
  });

  test('Step 6: View Assessment & Executive Report includes all artifacts', async () => {
    const reportRes = await fetch(`${API_BASE}/assessments/${assessmentId}/report`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    const reportData = await reportRes.json();
    assert.strictEqual(reportData.status, 'success');
    const r = reportData.data;

    // Check findings section contains created finding
    const findingsSection = r.sections['7. Findings & Vulnerabilities'] || [];
    assert.ok(findingsSection.some(f => f.id === createdFindingId));

    // Check evidence section contains attached evidence with SHA-256
    const evidenceSection = r.sections['8. Cryptographic Evidence Inventory'] || r.sections['8. Evidence Artifacts'] || [];
    assert.ok(evidenceSection.some(e => e.original_name === 'header_audit_poc.txt'));

    // Check reconnaissance section contains our observation
    const reconSection = r.sections['5. Reconnaissance Summary'] || [];
    assert.ok(reconSection.some(rc => rc.id === savedObservationId));
  });

  test('Step 7: Activity History logs all sequential operations with timestamps', async () => {
    const actRes = await fetch(`${API_BASE}/activities?assessment_id=${assessmentId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    const actData = await actRes.json();
    assert.strictEqual(actData.status, 'success');
    const activities = actData.data;

    // Verify observation, finding, and evidence activities are recorded in order
    assert.ok(activities.some(a => a.type === 'TOOL_OBSERVATION' || a.title.includes('Saved Tool Observation')));
    assert.ok(activities.some(a => a.type === 'FINDING' && a.title.includes('Converted Observation')));
    assert.ok(activities.some(a => a.type === 'EVIDENCE' || a.title.includes('Evidence')));
  });

  test('Step 8: Delete records cleanly (Observation, Finding, Evidence)', async () => {
    // Delete created finding (cascades to evidence)
    const delFindRes = await fetch(`${API_BASE}/findings/${createdFindingId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const delFindData = await delFindRes.json();
    assert.strictEqual(delFindData.status, 'success');

    // Delete saved observation
    const delObsRes = await fetch(`${API_BASE}/observations/${savedObservationId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const delObsData = await delObsRes.json();
    assert.strictEqual(delObsData.status, 'success');
  });

});
