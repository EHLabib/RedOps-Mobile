const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const http = require('node:http');

// Setup in-process Express server instance
const express = require('express');
const { db, logActivity, resetDatabase, seedDemoData } = require('../src/database/db');
const apiRoutes = require('../src/routes/api');

const app = express();
app.use(express.json());
app.use('/api/v1', apiRoutes);

let server;
let serverUrl;
let token;

test.before(async () => {
  resetDatabase();
  seedDemoData(true);

  await new Promise((resolve) => {
    server = app.listen(0, '127.0.0.1', () => {
      const port = server.address().port;
      serverUrl = `http://127.0.0.1:${port}/api/v1`;
      resolve();
    });
  });
});

test.after(async () => {
  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }
});

async function request(path, options = {}) {
  const url = `${serverUrl}${path}`;
  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const res = await fetch(url, { ...options, headers });
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const data = isJson ? await res.json() : await res.text();
  return { status: res.status, headers: res.headers, data };
}

test('1. Authentication: Operator login and JWT issuance', async () => {
  const res = await request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({
      username: 'sh@dow_sec',
      password: 'Password123!Safe'
    })
  });

  assert.equal(res.status, 200);
  assert.equal(res.data.status, 'success');
  assert.ok(res.data.token);
  assert.equal(res.data.user.username, 'sh@dow_sec');
  token = res.data.token;
});

test('2. Dashboard (4.1): Calculates statistics from actual persistent records', async () => {
  const res = await request('/dashboard/metrics');
  assert.equal(res.status, 200);
  const m = res.data.data.metrics;

  assert.ok(m.total_engagements >= 1);
  assert.ok(m.active_assessments >= 1);
  assert.ok(m.total_findings >= 1);
  assert.ok(m.severity_distribution.critical >= 1);
  assert.ok(m.open_findings >= 1);
  assert.ok(m.total_notes >= 1);

  const act = res.data.data.recent_activity;
  assert.ok(act.findings.length > 0);
  assert.ok(act.assessments.length > 0);
  assert.ok(act.activities.length > 0);
});

test('3. Engagement Management (4.2): Complete CRUD & Mandatory Authorization Declaration', async () => {
  // Reject without authorization declaration
  const unauthRes = await request('/assessments', {
    method: 'POST',
    body: JSON.stringify({
      name: 'Unauthorized Test',
      client: 'Target Org',
      start_date: '2026-11-01',
      end_date: '2026-11-05',
      is_authorized: 0
    })
  });
  assert.equal(unauthRes.status, 403);
  assert.match(unauthRes.data.message, /Authorization Failure/);

  // Create
  const createRes = await request('/assessments', {
    method: 'POST',
    body: JSON.stringify({
      name: 'Cloud Security Audit 2026',
      client: 'FinTech Cloud Corp',
      description: 'Zero Trust architecture and cloud endpoint assessment',
      assessment_type: 'Cloud',
      start_date: '2026-11-01',
      end_date: '2026-11-15',
      status: 'ACTIVE',
      authorized_scope: 'AWS Account 123456789012, https://api.fintech.cloud',
      out_of_scope_assets: 'Production RDS databases, PCI payment enclaves',
      objectives: 'Evaluate IAM misconfigurations and S3 bucket security',
      notes: 'Testing permitted 22:00-06:00 UTC',
      is_authorized: 1
    })
  });

  assert.equal(createRes.status, 201);
  const assessId = createRes.data.data.id;
  assert.ok(assessId.startsWith('asm-'));

  // Search & Filter
  const searchRes = await request('/assessments?search=FinTech&type=Cloud&status=ACTIVE');
  assert.equal(searchRes.status, 200);
  assert.ok(searchRes.data.data.some(a => a.id === assessId));

  // Update
  const updateRes = await request(`/assessments/${assessId}`, {
    method: 'PUT',
    body: JSON.stringify({
      status: 'COMPLETED',
      notes: 'Assessment finished on schedule'
    })
  });
  assert.equal(updateRes.status, 200);
  assert.equal(updateRes.data.data.status, 'COMPLETED');

  // Delete
  const delRes = await request(`/assessments/${assessId}`, { method: 'DELETE' });
  assert.equal(delRes.status, 200);
});

test('4. Findings Management (4.3): Complete CRUD, CVSS validation, Search, Filter, Sort, Status', async () => {
  const assessId = 'asm-acme-2026';

  // CVSS Validation (< 0 or > 10 rejected)
  const invalidCvssRes = await request(`/assessments/${assessId}/findings`, {
    method: 'POST',
    body: JSON.stringify({
      title: 'Invalid CVSS Finding',
      severity: 'HIGH',
      affected_asset: 'https://portal.acme.corp',
      cvss_score: 15.5
    })
  });
  assert.equal(invalidCvssRes.status, 400);

  // Create Finding
  const createRes = await request(`/assessments/${assessId}/findings`, {
    method: 'POST',
    body: JSON.stringify({
      title: 'Server-Side Request Forgery (SSRF) in Webhook Dispatcher',
      description: 'The endpoint /api/webhook/test accepts arbitrary user URLs without CIDR restriction.',
      affected_asset: 'https://api.acme.corp/v1/webhook',
      severity: 'HIGH',
      cvss_score: 8.6,
      cwe_id: 'CWE-918',
      category: 'A10:2021-Server-Side Request Forgery',
      technical_details: 'POST /api/webhook/test with target http://169.254.169.254/latest/meta-data/',
      impact: 'Cloud instance metadata exfiltration and credential exposure',
      remediation: 'Implement URL allow-list and disable internal loopback IP routing in webhook worker.',
      status: 'OPEN'
    })
  });

  assert.equal(createRes.status, 201);
  const fndId = createRes.data.data.id;
  assert.ok(createRes.data.data.custom_id.startsWith('F-'));

  // Search & Filter & Sort
  const searchRes = await request(`/assessments/${assessId}/findings?search=SSRF&severity=HIGH&sort=SEVERITY`);
  assert.equal(searchRes.status, 200);
  assert.ok(searchRes.data.data.some(f => f.id === fndId));

  // Status Change via PATCH
  const patchRes = await request(`/findings/${fndId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'REMEDIATED' })
  });
  assert.equal(patchRes.status, 200);
  assert.equal(patchRes.data.data.status, 'REMEDIATED');

  // Clean up
  const delRes = await request(`/findings/${fndId}`, { method: 'DELETE' });
  assert.equal(delRes.status, 200);
});

test('5. Assessment Notebook (4.4): CRUD, Search, Filter, Pin/Unpin, Favorite, Persistent Storage', async () => {
  const assessId = 'asm-acme-2026';

  // Create Note
  const createRes = await request(`/assessments/${assessId}/notes`, {
    method: 'POST',
    body: JSON.stringify({
      title: 'Kerberos Pre-Authentication Analysis',
      content: 'Tested AS-REP Roasting against staff service accounts without pre-auth.',
      category: 'OBSERVATION',
      tags: '#kerberos, #activedirectory',
      is_pinned: 1,
      is_favorite: 1
    })
  });

  assert.equal(createRes.status, 201);
  const noteId = createRes.data.data.id;

  // Search & Filter
  const listRes = await request(`/assessments/${assessId}/notes?search=Kerberos&category=OBSERVATION`);
  assert.equal(listRes.status, 200);
  assert.ok(listRes.data.data.some(n => n.id === noteId));

  // Toggle Pin
  const pinRes = await request(`/notes/${noteId}/pin`, { method: 'PATCH' });
  assert.equal(pinRes.status, 200);
  assert.equal(pinRes.data.is_pinned, 0);

  // Toggle Favorite
  const favRes = await request(`/notes/${noteId}/favorite`, { method: 'PATCH' });
  assert.equal(favRes.status, 200);
  assert.equal(favRes.data.is_favorite, 0);

  // Update
  const updateRes = await request(`/notes/${noteId}`, {
    method: 'PUT',
    body: JSON.stringify({
      content: 'Updated content: AS-REP Roasting negative, accounts properly secured.'
    })
  });
  assert.equal(updateRes.status, 200);

  // Delete
  const delRes = await request(`/notes/${noteId}`, { method: 'DELETE' });
  assert.equal(delRes.status, 200);
});

test('6. Activity Log (4.5): Chronological ordering and multi-module events', async () => {
  const res = await request('/activities?limit=10');
  assert.equal(res.status, 200);
  assert.ok(res.data.data.length > 0);

  // Check chronological order (newest first)
  for (let i = 0; i < res.data.data.length - 1; i++) {
    const cur = new Date(res.data.data[i].created_at).getTime();
    const next = new Date(res.data.data[i + 1].created_at).getTime();
    assert.ok(cur >= next, 'Activities must be ordered newest to oldest');
  }
});

test('7. Report Generation & History (4.7): Generates actual reports and maintains history', async () => {
  const assessId = 'asm-acme-2026';

  // JSON Preview
  const repRes = await request(`/assessments/${assessId}/report`);
  assert.equal(repRes.status, 200);
  assert.ok(repRes.data.data.sections['1. Executive Summary']);
  assert.ok(repRes.data.data.sections['8. Cryptographic Evidence Inventory']);

  // TXT Download (generates history record)
  const downloadRes = await request(`/assessments/${assessId}/report/download`);
  assert.equal(downloadRes.status, 200);
  assert.ok(downloadRes.data.includes('REDOPS MOBILE — PROFESSIONAL SECURITY ASSESSMENT REPORT'));

  // Report History
  const histRes = await request(`/assessments/${assessId}/report/history`);
  assert.equal(histRes.status, 200);
  assert.ok(histRes.data.data.length > 0);
  assert.ok(histRes.data.data[0].sha256_hash);
});

test('8. Settings & Security (4.8): PIN protection, security status, backup export', async () => {
  // Security status
  const secRes = await request('/system/security-status');
  assert.equal(secRes.status, 200);
  assert.ok(secRes.data.data.encryption.at_rest);

  // PIN validation
  const pinValidRes = await request('/system/verify-pin', {
    method: 'POST',
    body: JSON.stringify({ pin: '1337' })
  });
  assert.equal(pinValidRes.status, 200);

  const pinInvalidRes = await request('/system/verify-pin', {
    method: 'POST',
    body: JSON.stringify({ pin: '9999' })
  });
  assert.equal(pinInvalidRes.status, 401);

  // Backup Export
  const backupRes = await request('/system/backup/export');
  assert.equal(backupRes.status, 200);
  assert.ok(backupRes.data.data.assessments.length > 0);
  assert.ok(backupRes.data.data.findings.length > 0);
  assert.ok(backupRes.data.data.notebook.length > 0);
});
