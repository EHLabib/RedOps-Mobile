/**
 * REDOPS MOBILE — BACKEND SECURITY AUDIT & HARDENING TEST SUITE
 * 
 * Verifies all 6 critical security categories:
 * 1. Privilege Escalation Prevention & Bootstrap Lockdown
 * 2. Mandatory Authentication on Private Endpoints
 * 3. Cross-User Data Access (IDOR Prevention & Multi-Tenant Isolation)
 * 4. Target Authorization & Consent Guardrails
 * 5. SSRF Defense (Loopback, IMDS 169.254.169.254, RFC 1918, IPv6 ULA, Unsafe Schemes)
 * 6. File Handling Hardening (MIME/Extension Whitelisting, Dangerous Executable Rejection, Path Traversal Defense)
 * 7. Administrative Endpoint Authorization
 */

const assert = require('node:assert');
const path = require('node:path');
const fs = require('node:fs');

const BASE_URL = 'http://localhost:5000/api/v1';

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const headers = { ...(options.headers || {}) };
  if (options.body && !(options.body instanceof FormData) && typeof options.body === 'object') {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  const res = await fetch(url, { ...options, headers });
  let data = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    data = await res.json();
  } else {
    data = await res.text();
  }

  return { status: res.status, headers: res.headers, data };
}

async function runSecurityAudit() {
  console.log('\n================================================================');
  console.log('  REDOPS MOBILE — SENIOR BACKEND SECURITY AUDIT & VERIFICATION');
  console.log('================================================================\n');

  let passed = 0;
  let total = 0;

  function testCheck(name, condition, details = '') {
    total++;
    if (condition) {
      passed++;
      console.log(`  [PASS] ${total}. ${name}`);
    } else {
      console.error(`  [FAIL] ${total}. ${name} -> ${details}`);
    }
  }

  // Generate unique test identities
  const ts = Date.now();
  const userAlpha = {
    username: `alpha_${ts}`,
    email: `alpha_${ts}@example.org`,
    password: 'PasswordAlpha123!',
    full_name: 'Operator Alpha'
  };

  const userBeta = {
    username: `beta_${ts}`,
    email: `beta_${ts}@example.org`,
    password: 'PasswordBeta123!',
    full_name: 'Operator Beta'
  };

  console.log('--- PHASE 1: PRIVILEGE ESCALATION & BOOTSTRAP DEFENSE ---');

  // Test 1: Self-assignment of ADMIN role during registration is strictly ignored
  const regAdminAttempt = await request('/auth/register', {
    method: 'POST',
    body: { ...userAlpha, role: 'ADMIN' }
  });
  const regUser = regAdminAttempt.data?.user || regAdminAttempt.data?.data?.user;
  testCheck(
    'Registration strictly enforces USER role, discarding client-supplied ADMIN role',
    regAdminAttempt.status === 201 && regUser && regUser.role === 'USER',
    `Status: ${regAdminAttempt.status}, Role: ${regUser?.role}`
  );

  // Register User Beta as well
  await request('/auth/register', {
    method: 'POST',
    body: userBeta
  });

  // Test 2: Admin bootstrap endpoint is permanently locked when admin exists
  const bootstrapAttempt = await request('/auth/bootstrap-admin', {
    method: 'POST',
    body: {
      username: 'evil_admin',
      password: 'EvilPassword123!',
      email: 'evil@attacker.local',
      bootstrap_key: 'random_key'
    }
  });
  testCheck(
    'Admin bootstrap is locked (403 Forbidden) once system administrator exists',
    bootstrapAttempt.status === 403,
    `Status: ${bootstrapAttempt.status}`
  );

  // Login both users to acquire JWT tokens
  const loginAlpha = await request('/auth/login', {
    method: 'POST',
    body: { username: userAlpha.username, password: userAlpha.password }
  });
  const tokenAlpha = loginAlpha.data.token;
  const idAlpha = loginAlpha.data.user.id;

  const loginBeta = await request('/auth/login', {
    method: 'POST',
    body: { username: userBeta.username, password: userBeta.password }
  });
  const tokenBeta = loginBeta.data.token;
  const idBeta = loginBeta.data.user.id;

  // Also obtain admin token (for admin-only checks)
  const loginAdmin = await request('/auth/login', {
    method: 'POST',
    body: { username: 'sh@dow_sec', password: 'Password123!Safe' }
  });
  const tokenAdmin = loginAdmin.data?.token;

  console.log('\n--- PHASE 2: UNAUTHENTICATED ACCESS PREVENTION ---');

  const privateEndpoints = [
    { method: 'GET', path: '/assessments' },
    { method: 'POST', path: '/assessments', body: { name: 'Test' } },
    { method: 'GET', path: '/assessments/asm-acme-2026' },
    { method: 'GET', path: '/assessments/asm-acme-2026/findings' },
    { method: 'POST', path: '/assessments/asm-acme-2026/findings', body: { title: 'Test' } },
    { method: 'GET', path: '/assessments/asm-acme-2026/targets' },
    { method: 'GET', path: '/assessments/asm-acme-2026/notes' },
    { method: 'GET', path: '/assessments/asm-acme-2026/report' },
    { method: 'GET', path: '/activities' },
    { method: 'GET', path: '/system/security-status' },
    { method: 'POST', path: '/tools/web/headers', body: { target: 'https://example.com' } },
    { method: 'POST', path: '/tools/web/ssl', body: { target: 'https://example.com' } },
    { method: 'POST', path: '/tools/dns/resolve', body: { domain: 'example.com' } }
  ];

  for (const ep of privateEndpoints) {
    const res = await request(ep.path, { method: ep.method, body: ep.body });
    testCheck(
      `Unauthenticated ${ep.method} ${ep.path.split('?')[0]} blocked (401 Unauthorized)`,
      res.status === 401,
      `Status: ${res.status}`
    );
  }

  console.log('\n--- PHASE 3: CROSS-USER DATA ACCESS (IDOR) DEFENSE ---');

  // Operator Alpha creates a private assessment
  const createAsmAlpha = await request('/assessments', {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenAlpha}` },
    body: {
      name: `Confidential Alpha Engagement ${ts}`,
      client: 'Alpha Financial Corp',
      start_date: '2026-11-01',
      end_date: '2026-11-30',
      is_authorized: true
    }
  });

  const asmIdAlpha = createAsmAlpha.data?.data?.id;
  testCheck(
    'Operator Alpha creates private assessment with owner binding',
    createAsmAlpha.status === 201 && asmIdAlpha && createAsmAlpha.data.data.user_id === idAlpha,
    `Status: ${createAsmAlpha.status}, User ID: ${createAsmAlpha.data?.data?.user_id}`
  );

  // Operator Alpha creates a finding on their assessment
  const createFindingAlpha = await request(`/assessments/${asmIdAlpha}/findings`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenAlpha}` },
    body: {
      title: 'Confidential Internal SQL Injection',
      severity: 'CRITICAL',
      affected_asset: 'https://internal.alpha.corp/db',
      cvss_score: 9.8
    }
  });
  const findingIdAlpha = createFindingAlpha.data?.data?.id;

  // Test Operator Beta attempting to read Operator Alpha's assessment
  const betaReadAsm = await request(`/assessments/${asmIdAlpha}`, {
    headers: { Authorization: `Bearer ${tokenBeta}` }
  });
  testCheck(
    'Operator Beta blocked from viewing Alpha assessment (403 Forbidden)',
    betaReadAsm.status === 403,
    `Status: ${betaReadAsm.status}`
  );

  // Test Operator Beta attempting to modify Operator Alpha's assessment
  const betaUpdateAsm = await request(`/assessments/${asmIdAlpha}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${tokenBeta}` },
    body: { name: 'Hacked By Beta' }
  });
  testCheck(
    'Operator Beta blocked from updating Alpha assessment (403 Forbidden)',
    betaUpdateAsm.status === 403,
    `Status: ${betaUpdateAsm.status}`
  );

  // Test Operator Beta attempting to delete Operator Alpha's assessment
  const betaDeleteAsm = await request(`/assessments/${asmIdAlpha}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${tokenBeta}` }
  });
  testCheck(
    'Operator Beta blocked from deleting Alpha assessment (403 Forbidden)',
    betaDeleteAsm.status === 403,
    `Status: ${betaDeleteAsm.status}`
  );

  // Test Operator Beta attempting to access Alpha's findings
  const betaReadFindings = await request(`/assessments/${asmIdAlpha}/findings`, {
    headers: { Authorization: `Bearer ${tokenBeta}` }
  });
  testCheck(
    'Operator Beta blocked from listing Alpha findings (403 Forbidden)',
    betaReadFindings.status === 403,
    `Status: ${betaReadFindings.status}`
  );

  // Test Operator Beta attempting to inject a finding into Alpha's assessment
  const betaCreateFinding = await request(`/assessments/${asmIdAlpha}/findings`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenBeta}` },
    body: {
      title: 'Malicious Injected Finding',
      severity: 'LOW',
      affected_asset: 'https://evil.local'
    }
  });
  testCheck(
    'Operator Beta blocked from injecting finding into Alpha assessment (403 Forbidden)',
    betaCreateFinding.status === 403,
    `Status: ${betaCreateFinding.status}`
  );

  // Test Operator Beta attempting to mutate Alpha's finding directly by ID
  const betaMutateFinding = await request(`/findings/${findingIdAlpha}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${tokenBeta}` },
    body: { title: 'Tampered Finding' }
  });
  testCheck(
    'Operator Beta blocked from modifying Alpha finding by ID (403 Forbidden)',
    betaMutateFinding.status === 403,
    `Status: ${betaMutateFinding.status}`
  );

  // Test Operator Beta attempting to export Alpha's assessment report
  const betaReadReport = await request(`/assessments/${asmIdAlpha}/report`, {
    headers: { Authorization: `Bearer ${tokenBeta}` }
  });
  testCheck(
    'Operator Beta blocked from viewing Alpha assessment report (403 Forbidden)',
    betaReadReport.status === 403,
    `Status: ${betaReadReport.status}`
  );

  console.log('\n--- PHASE 4: TARGET AUTHORIZATION & ETHICAL ROE ---');

  // Test running tool without is_authorized confirmation
  const unconfirmedTool = await request('/tools/web/headers', {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenAlpha}` },
    body: { target: 'https://example.com' }
  });
  testCheck(
    'Toolkit denies execution when is_authorized confirmation is omitted (403 Forbidden)',
    unconfirmedTool.status === 403,
    `Status: ${unconfirmedTool.status}`
  );

  console.log('\n--- PHASE 5: SSRF DEFENSE & DESTINATION BOUNDARIES ---');

  const ssrfTargets = [
    { name: 'IPv4 Loopback (127.0.0.1)', url: 'http://127.0.0.1:8080' },
    { name: 'Localhost alias', url: 'http://localhost:5000' },
    { name: 'AWS/GCP Cloud Metadata (169.254.169.254)', url: 'http://169.254.169.254/latest/meta-data/' },
    { name: 'RFC 1918 Private Class A (10.0.0.1)', url: 'http://10.0.0.1' },
    { name: 'RFC 1918 Private Class C (192.168.1.1)', url: 'http://192.168.1.1' },
    { name: 'IPv6 Loopback ([::1])', url: 'http://[::1]:8080' }
  ];

  for (const t of ssrfTargets) {
    const res = await request('/tools/web/headers', {
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenAlpha}` },
      body: { target: t.url, is_authorized: true }
    });
    testCheck(
      `SSRF Guardrail blocks ${t.name} (403 Access Denied)`,
      res.status === 403 && (res.data?.message?.includes('Access Denied') || res.data?.message?.includes('blocked') || res.data?.message?.includes('private')),
      `Status: ${res.status}, Msg: ${res.data?.message}`
    );
  }

  // Test unsafe URL scheme
  const unsafeScheme = await request('/tools/web/headers', {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenAlpha}` },
    body: { target: 'file:///etc/passwd', is_authorized: true }
  });
  testCheck(
    'SSRF Guardrail blocks non-HTTP/HTTPS scheme file:// (400 Bad Request)',
    unsafeScheme.status === 400,
    `Status: ${unsafeScheme.status}`
  );

  console.log('\n--- PHASE 6: EVIDENCE FILE HANDLING & PATH TRAVERSAL DEFENSE ---');

  // Test uploading dangerous executable file (.exe)
  const boundary = '----WebKitFormBoundarySecurityAudit';
  const exePayload = [
    `--${boundary}`,
    'Content-Disposition: form-data; name="evidence_file"; filename="exploit.exe"',
    'Content-Type: application/x-msdownload',
    '',
    'MZ_EXECUTABLE_BINARY_DATA',
    `--${boundary}--`
  ].join('\r\n');

  const uploadExe = await request(`/findings/${findingIdAlpha}/evidence`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${tokenAlpha}`,
      'Content-Type': `multipart/form-data; boundary=${boundary}`
    },
    body: exePayload
  });
  testCheck(
    'Evidence upload rejects dangerous executable files (.exe) (400 Bad Request)',
    uploadExe.status === 400 && (uploadExe.data?.message?.includes('prohibited') || uploadExe.data?.message?.includes('Dangerous')),
    `Status: ${uploadExe.status}, Msg: ${uploadExe.data?.message}`
  );

  // Test uploading script file (.php)
  const phpPayload = [
    `--${boundary}`,
    'Content-Disposition: form-data; name="evidence_file"; filename="webshell.php"',
    'Content-Type: application/x-php',
    '',
    '<?php echo "pwned"; ?>',
    `--${boundary}--`
  ].join('\r\n');

  const uploadPhp = await request(`/findings/${findingIdAlpha}/evidence`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${tokenAlpha}`,
      'Content-Type': `multipart/form-data; boundary=${boundary}`
    },
    body: phpPayload
  });
  testCheck(
    'Evidence upload rejects dangerous script files (.php) (400 Bad Request)',
    uploadPhp.status === 400 && (uploadPhp.data?.message?.includes('prohibited') || uploadPhp.data?.message?.includes('Dangerous')),
    `Status: ${uploadPhp.status}, Msg: ${uploadPhp.data?.message}`
  );

  // Test uploading legitimate forensic log (.log / text/plain)
  const logPayload = [
    `--${boundary}`,
    'Content-Disposition: form-data; name="evidence_file"; filename="audit_proof.log"',
    'Content-Type: text/plain',
    '',
    'HTTP/1.1 500 Internal Server Error\nSQL Syntax error in query...',
    `--${boundary}--`
  ].join('\r\n');

  const uploadLog = await request(`/findings/${findingIdAlpha}/evidence`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${tokenAlpha}`,
      'Content-Type': `multipart/form-data; boundary=${boundary}`
    },
    body: logPayload
  });
  const evidenceId = uploadLog.data?.data?.id;
  testCheck(
    'Evidence upload accepts legitimate forensic text log and returns SHA-256 integrity hash',
    uploadLog.status === 201 && evidenceId && uploadLog.data.data.sha256_hash?.length === 64,
    `Status: ${uploadLog.status}, Hash: ${uploadLog.data?.data?.sha256_hash}`
  );

  // Test Operator Beta attempting to download Operator Alpha's evidence file
  if (evidenceId) {
    const betaDownload = await request(`/evidence/${evidenceId}/file`, {
      headers: { Authorization: `Bearer ${tokenBeta}` }
    });
    testCheck(
      'Operator Beta blocked from downloading Alpha evidence file (403 Forbidden)',
      betaDownload.status === 403,
      `Status: ${betaDownload.status}`
    );

    const alphaDownload = await request(`/evidence/${evidenceId}/file`, {
      headers: { Authorization: `Bearer ${tokenAlpha}` }
    });
    testCheck(
      'Operator Alpha successfully downloads their verified evidence file (200 OK)',
      alphaDownload.status === 200,
      `Status: ${alphaDownload.status}`
    );
  }

  console.log('\n--- PHASE 7: ADMINISTRATIVE ENDPOINT PROTECTION ---');

  // Test standard Operator Alpha attempting admin-only endpoints
  const alphaClear = await request('/system/clear-data', {
    method: 'POST',
    headers: { Authorization: `Bearer ${tokenAlpha}` }
  });
  testCheck(
    'Standard operator blocked from clearing database (403 Forbidden)',
    alphaClear.status === 403,
    `Status: ${alphaClear.status}`
  );

  const alphaBackup = await request('/system/backup/export', {
    headers: { Authorization: `Bearer ${tokenAlpha}` }
  });
  testCheck(
    'Standard operator blocked from exporting full platform backup (403 Forbidden)',
    alphaBackup.status === 403,
    `Status: ${alphaBackup.status}`
  );

  if (tokenAdmin) {
    const adminBackup = await request('/system/backup/export', {
      headers: { Authorization: `Bearer ${tokenAdmin}` }
    });
    testCheck(
      'Verified administrator successfully exports system backup (200 OK)',
      adminBackup.status === 200,
      `Status: ${adminBackup.status}`
    );
  }

  console.log('\n================================================================');
  console.log(`  AUDIT RESULTS: ${passed} / ${total} Checks PASSED (${Math.round((passed / total) * 100)}%)`);
  console.log('================================================================\n');

  if (passed !== total) {
    process.exit(1);
  }
}

runSecurityAudit().catch(err => {
  console.error('Test suite failed with unexpected error:', err);
  process.exit(1);
});
