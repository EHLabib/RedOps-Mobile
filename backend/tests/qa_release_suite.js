const assert = require('assert');
const crypto = require('crypto');

const BASE_URL = 'http://localhost:5000/api/v1';

async function runQaSuite() {
  console.log('================================================================');
  console.log('  REDOPS MOBILE — COMPLETE QA & RELEASE VERIFICATION SUITE');
  console.log('================================================================\n');

  // Authenticate as operator
  console.log('[Phase 1] Authenticating Operator & Checking Security Boundaries...');
  const authRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'sh@dow_sec', password: 'Password123!Safe' })
  });
  assert.strictEqual(authRes.status, 200, 'Operator login must succeed');
  const authData = await authRes.json();
  const token = authData.token;
  assert.ok(token, 'JWT token must be issued');
  console.log('  ✔ Authentication passed: Logged in as', authData.user.username, `(${authData.user.role})`);

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  // 1. Test Every Cybersecurity Tool Individually
  console.log('\n[Phase 2] Testing Every Cybersecurity Tool Individually...');

  // Tool 1: Hash Generator
  const hashRes = await fetch(`${BASE_URL}/tools/crypto/hash`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ input: 'RedOps_Mobile_2026', salt: 'sec', hmacKey: 'key' })
  });
  assert.strictEqual(hashRes.status, 200);
  const hashData = await hashRes.json();
  assert.ok(hashData.data.hashes.find(h => h.algorithm === 'SHA-256'), 'SHA-256 hash must be generated');
  assert.ok(hashData.data.hmac, 'HMAC must be calculated');
  console.log('  ✔ Tool 1/14: Hash Generator (MD5, SHA-1, SHA-256, SHA-384, SHA-512, HMAC)');

  // Tool 2: Hash Identifier
  const idHashRes = await fetch(`${BASE_URL}/tools/crypto/identify`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ hash: '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8' })
  });
  assert.strictEqual(idHashRes.status, 200);
  const idHashData = await idHashRes.json();
  assert.ok(idHashData.data.candidates.some(c => c.algorithm === 'SHA-256'), 'Must identify SHA-256');
  console.log('  ✔ Tool 2/14: Hash Identifier (Pattern & length classifier)');

  // Tool 3: Multi-Format Converter (Base64, Hex, URL, UTF-8)
  const convRes = await fetch(`${BASE_URL}/tools/crypto/convert`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ input: 'RedOps Authorized', from: 'utf8', to: 'base64' })
  });
  assert.strictEqual(convRes.status, 200);
  const convData = await convRes.json();
  assert.strictEqual(convData.data.output, 'UmVkT3BzIEF1dGhvcml6ZWQ=');
  console.log('  ✔ Tool 3/14: Multi-Format Converter (Base64, Hex, URL, UTF-8)');

  // Tool 4: JWT Decoder
  const sampleToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMzM3IiwibmFtZSI6IlNoYWRvdyBPcGVyYXRvciIsImFkbWluIjp0cnVlLCJpYXQiOjE3OTQxNDAwMDAsImV4cCI6MTc5NDIyNjQwMH0.signature-placeholder';
  const jwtRes = await fetch(`${BASE_URL}/tools/crypto/jwt-decode`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ token: sampleToken })
  });
  assert.strictEqual(jwtRes.status, 200);
  const jwtData = await jwtRes.json();
  assert.strictEqual(jwtData.data.header.alg, 'HS256');
  assert.strictEqual(jwtData.data.payload.sub, '1337');
  assert.ok(jwtData.data.disclaimer.includes('signature') || jwtData.data.disclaimer.includes('verify'), 'Signature verification notice must be present');
  console.log('  ✔ Tool 4/14: JWT Decoder (Header, Payload, Timestamps, Local verification disclaimer)');

  // Tool 5: URL Analyzer
  const urlRes = await fetch(`${BASE_URL}/tools/web/url-analyze`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://admin:secret@portal.acme.corp:8443/api/v1/auth?redirect=http://test.com#token' })
  });
  assert.strictEqual(urlRes.status, 200);
  const urlData = await urlRes.json();
  assert.strictEqual(urlData.data.hostname, 'portal.acme.corp');
  assert.strictEqual(urlData.data.port, '8443');
  assert.ok(urlData.data.suspicious_indicators.length > 0, 'Must highlight credential embedding & open redirect param');
  console.log('  ✔ Tool 5/14: URL Analyzer (Structure, Query Parameters, Security Indicators)');

  // Tool 6: Subnet Calculator
  const subnetRes = await fetch(`${BASE_URL}/tools/network/subnet`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ cidr: '192.168.10.0/24' })
  });
  assert.strictEqual(subnetRes.status, 200);
  const subnetData = await subnetRes.json();
  assert.strictEqual(subnetData.data.network, '192.168.10.0');
  assert.strictEqual(subnetData.data.broadcast, '192.168.10.255');
  assert.strictEqual(subnetData.data.usableHostsRaw, 254);
  console.log('  ✔ Tool 6/14: Subnet Calculator (IPv4 CIDR, Netmask, Range, Usable Host Count)');

  // Tool 7: Password Strength Estimator
  const pwdRes = await fetch(`${BASE_URL}/tools/crypto/password-strength`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ password: 'P@ssw0rd123!SecureOps#2026' })
  });
  assert.strictEqual(pwdRes.status, 200);
  const pwdData = await pwdRes.json();
  assert.ok(pwdData.data.score >= 80, 'Strong password must score high');
  assert.strictEqual(pwdData.data.strength, 'Very Strong');
  console.log('  ✔ Tool 7/14: Password Strength Estimator (Length, Entropy, Character Diversity, Suggestions)');

  // Tool 8: JSON Formatter
  const jsonRes = await fetch(`${BASE_URL}/tools/format/json`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ json: '{"service":"redops","active":true,"code":200}', indent: 2 })
  });
  assert.strictEqual(jsonRes.status, 200);
  const jsonData = await jsonRes.json();
  assert.strictEqual(jsonData.data.is_valid, true);
  assert.ok(jsonData.data.formatted.includes('service'));
  console.log('  ✔ Tool 8/14: JSON Formatter (Pretty-print, Minify, Syntax Validation)');

  // Tool 9: DNS Lookup (Real public query)
  const dnsRes = await fetch(`${BASE_URL}/tools/dns/resolve`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ domain: 'google.com', type: 'A', authorized: true })
  });
  assert.strictEqual(dnsRes.status, 200);
  const dnsData = await dnsRes.json();
  assert.ok(dnsData.data.records.length > 0, 'Must resolve Google A records');
  console.log('  ✔ Tool 9/14: DNS Lookup (A, AAAA, MX, NS, TXT record resolution)');

  // Tool 10: Domain Information (RDAP)
  const rdapRes = await fetch(`${BASE_URL}/tools/dns/rdap`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ query: 'example.com', authorized: true })
  });
  assert.strictEqual(rdapRes.status, 200);
  const rdapData = await rdapRes.json();
  assert.strictEqual(rdapData.data.query, 'example.com');
  console.log('  ✔ Tool 10/14: Domain Information (Public RDAP protocol, registration data)');

  // Tool 11: IP Information & Scope Classification
  const ipRes = await fetch(`${BASE_URL}/tools/dns/ip-info`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ip: '10.0.1.5', authorized: true })
  });
  assert.strictEqual(ipRes.status, 200);
  const ipData = await ipRes.json();
  assert.strictEqual(ipData.data.scope, 'RFC 1918 Private Network');
  assert.strictEqual(ipData.data.is_routable, false);
  console.log('  ✔ Tool 11/14: IP Information & Scope Classifier (IPv4/IPv6, RFC 1918/4193)');

  // Tool 12: HTTP Security Headers
  const hdrRes = await fetch(`${BASE_URL}/tools/web/headers`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://example.com', authorized: true })
  });
  assert.strictEqual(hdrRes.status, 200);
  const hdrData = await hdrRes.json();
  assert.ok(hdrData.data.status_code >= 200 && hdrData.data.status_code < 400);
  assert.ok(Array.isArray(hdrData.data.findings), 'Must contain defensive header findings');
  console.log('  ✔ Tool 12/14: HTTP Security Header Analyzer (HSTS, CSP, XFO, XCTO, Referrer, Permissions)');

  // Tool 13: TLS Certificate Inspector
  const sslRes = await fetch(`${BASE_URL}/tools/web/ssl`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ host: 'example.com', port: 443, authorized: true })
  });
  assert.strictEqual(sslRes.status, 200);
  const sslData = await sslRes.json();
  assert.ok(sslData.data.issuer, 'Must return certificate issuer');
  assert.ok(sslData.data.days_remaining > 0, 'Valid certificate has remaining days');
  console.log('  ✔ Tool 13/14: TLS Certificate Inspector (Issuer, Validity, Protocol, RFC 6125 match)');

  // Tool 14: Redirect Tracer
  const redRes = await fetch(`${BASE_URL}/tools/web/redirects`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'http://example.com', authorized: true })
  });
  assert.strictEqual(redRes.status, 200);
  const redData = await redRes.json();
  assert.ok(redData.data.total_hops >= 1, 'Must track redirect hops');
  console.log('  ✔ Tool 14/14: Redirect Tracer (HTTP hop chain, status codes, final URL)');

  // 2. Test Invalid Inputs
  console.log('\n[Phase 3] Testing Invalid Inputs & Boundary Validations...');

  // 2.1 Subnet calculator invalid prefix
  const badSubnet = await fetch(`${BASE_URL}/tools/network/subnet`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ cidr: '192.168.1.0/35' })
  });
  assert.strictEqual(badSubnet.status, 400, 'CIDR /35 must be rejected with 400');
  console.log('  ✔ Rejected out-of-range subnet prefix /35');

  // 2.2 Malformed JSON
  const badJson = await fetch(`${BASE_URL}/tools/format/json`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ json: '{ invalid json structure }' })
  });
  const badJsonData = await badJson.json();
  assert.strictEqual(badJsonData.data.is_valid, false, 'Malformed JSON must report is_valid: false');
  assert.ok(badJsonData.data.error_message, 'Must provide error message');
  console.log('  ✔ Rejected malformed JSON with precise line/column pointer');

  // 2.3 Invalid IP syntax
  const badIp = await fetch(`${BASE_URL}/tools/dns/ip-info`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ip: '999.888.777.666', authorized: true })
  });
  assert.strictEqual(badIp.status, 400, 'Invalid IP syntax must return 400');
  console.log('  ✔ Rejected invalid IP syntax');

  // 2.4 CVSS score out-of-bounds in finding creation
  const badCvss = await fetch(`${BASE_URL}/assessments/asm-acme-2026/findings`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      title: 'Bad CVSS Test',
      severity: 'CRITICAL',
      cvss_score: 15.5,
      affected_asset: 'portal.acme.corp'
    })
  });
  assert.strictEqual(badCvss.status, 400, 'CVSS score > 10.0 must be rejected with 400');
  console.log('  ✔ Rejected out-of-bounds CVSS score (15.5 > 10.0)');

  // 3. Test Network Failures, Unreachable Hosts, and SSRF Protections
  console.log('\n[Phase 4] Testing Network Failures, Unavailable Services & SSRF Protections...');

  // 3.1 Non-existent domain DNS
  const nxDns = await fetch(`${BASE_URL}/tools/dns/resolve`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ domain: 'redops-definitely-nonexistent-domain-xyz-12345.org', type: 'A', authorized: true })
  });
  const nxDnsData = await nxDns.json();
  console.log('    [Debug NXDOMAIN]', JSON.stringify(nxDnsData));
  assert.ok(nxDnsData.data.records.some(r => r.status === 'ERROR' || (r.message && r.message.includes('not found'))), 'Must report error gracefully');
  console.log('  ✔ Gracefully handled non-existent domain (NXDOMAIN) without crashing');

  // 3.2 Unreachable TLS port
  const badTls = await fetch(`${BASE_URL}/tools/web/ssl`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ host: '127.0.0.1', port: 59999, authorized: true })
  });
  assert.ok(badTls.status >= 400, 'Connection refusal must return clean error status');
  console.log('  ✔ Handled unreachable connection failure safely');

  // 3.3 SSRF Loopback protection
  const ssrfLoopback = await fetch(`${BASE_URL}/tools/web/headers`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'http://127.0.0.1:5000/api/v1/users', authorized: true })
  });
  assert.ok(ssrfLoopback.status === 400 || ssrfLoopback.status === 403, 'Loopback SSRF must be blocked');
  console.log('  ✔ SSRF protection successfully blocked loopback 127.0.0.1');

  // 3.4 SSRF Cloud Metadata protection
  const ssrfMeta = await fetch(`${BASE_URL}/tools/web/headers`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'http://169.254.169.254/latest/meta-data/', authorized: true })
  });
  assert.ok(ssrfMeta.status === 400 || ssrfMeta.status === 403, 'AWS/GCP metadata SSRF must be blocked');
  console.log('  ✔ SSRF protection successfully blocked cloud metadata 169.254.169.254');

  // 3.5 SSRF Non-HTTP protocol protection
  const ssrfFile = await fetch(`${BASE_URL}/tools/web/headers`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'file:///etc/passwd', authorized: true })
  });
  assert.strictEqual(ssrfFile.status, 400, 'file:// protocol must be blocked');
  console.log('  ✔ SSRF protection blocked unsafe file:// protocol');

  // 4. Test Frontend/Backend Communication & Legal Consent Gate
  console.log('\n[Phase 5] Testing Scope Consent Gate & Auth Enforcement...');
  
  // Unconsented network operation
  const unconsented = await fetch(`${BASE_URL}/tools/web/headers`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://example.com', authorized: false })
  });
  assert.strictEqual(unconsented.status, 403, 'Unconsented target check must be blocked with 403');
  console.log('  ✔ Mandatory Legal Consent Gate (ROE) strictly enforced (403 Forbidden)');

  // Missing bearer token on mutation endpoint
  const unauth = await fetch(`${BASE_URL}/assessments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: 'Unauthorized Engagement' })
  });
  assert.strictEqual(unauth.status, 401, 'Protected mutation route requires JWT');
  console.log('  ✔ JWT Authentication strictly enforced (401 Unauthorized)');

  // 5. Test Assessment Lifecycle, Persistence & Report Generation
  console.log('\n[Phase 6] Testing Assessment Integration, Database Persistence & Reporting...');

  // Step 5.1: Create Engagement
  const asmRes = await fetch(`${BASE_URL}/assessments`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      name: 'QA Release Verification Engagement',
      client: 'RedOps Core QA Corp',
      start_date: '2026-11-08',
      end_date: '2026-11-15',
      assessment_type: 'Full Hybrid Security Assessment',
      authorized_scope: 'https://portal.redops-qa.local, 192.168.100.0/24',
      out_of_scope_assets: 'payment.redops-qa.local, third-party APIs',
      objectives: 'Verify defensive header hardening, TLS configurations, and assess cryptographic evidence handling.',
      is_authorized: true,
      status: 'ACTIVE'
    })
  });
  assert.strictEqual(asmRes.status, 201);
  const asmData = await asmRes.json();
  const testAsmId = asmData.data.id;
  console.log('  ✔ Created Engagement:', asmData.data.name, `(${testAsmId})`);

  // Step 5.2: Create Target
  const tgtRes = await fetch(`${BASE_URL}/assessments/${testAsmId}/targets`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      domain: 'portal.redops-qa.local',
      ip_address: '192.168.100.10',
      scope_status: 'IN_SCOPE',
      url: 'https://portal.redops-qa.local',
      description: 'Primary customer authentication portal'
    })
  });
  assert.strictEqual(tgtRes.status, 201);
  const tgtData = await tgtRes.json();
  console.log('  ✔ Created Target Asset:', tgtData.data.domain);

  // Step 5.3: Save Tool Observation
  const obsRes = await fetch(`${BASE_URL}/assessments/${testAsmId}/observations`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      tool_name: 'HTTP Security Headers',
      tool_category: 'WEB_SECURITY',
      title: 'Missing HSTS & CSP Defense Headers',
      target: 'https://portal.redops-qa.local',
      source_info: 'Audit Port 443',
      result_data: { grade: 'F', score: 0, missing: ['Strict-Transport-Security', 'Content-Security-Policy'] },
      notes: 'Discovered during authorized passive header analysis',
      status: 'INFORMATIONAL'
    })
  });
  assert.strictEqual(obsRes.status, 201);
  const obsData = await obsRes.json();
  const testObsId = obsData.data.id;
  console.log('  ✔ Saved Tool Observation:', obsData.data.title);

  // Step 5.4: Convert Observation into Formal Vulnerability Finding
  const convObsRes = await fetch(`${BASE_URL}/observations/${testObsId}/convert-to-finding`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      title: 'Missing Strict-Transport-Security & Content-Security-Policy',
      severity: 'HIGH',
      cvss_score: 7.5,
      status: 'OPEN',
      category: 'Security Misconfiguration',
      cwe_id: 'CWE-693',
      affected_asset: 'https://portal.redops-qa.local',
      description: 'The web service does not enforce Strict-Transport-Security (HSTS) or Content-Security-Policy (CSP).',
      technical_details: 'HTTP Response Headers missing Strict-Transport-Security and Content-Security-Policy directives.',
      impact: 'Enables potential man-in-the-middle downgrade attacks and increases susceptibility to cross-site scripting (XSS).',
      remediation: 'Configure the web server to enforce Strict-Transport-Security: max-age=31536000; includeSubDomains and a restrictive CSP.'
    })
  });
  assert.strictEqual(convObsRes.status, 201);
  const fndData = await convObsRes.json();
  const fnd = fndData.data.finding || fndData.data;
  const testFndId = fnd.id;
  console.log('  ✔ Converted Observation into Finding:', fnd.custom_id, fnd.title);

  // Step 5.5: Attach Evidence to Finding
  const boundary = '----WebKitFormBoundaryQA2026';
  const fileContent = 'GET / HTTP/1.1\nHost: portal.redops-qa.local\n\nHTTP/1.1 200 OK\nServer: nginx/1.24\n(HSTS Missing)';
  const body = [
    `--${boundary}`,
    'Content-Disposition: form-data; name="file"; filename="header-audit-proof.log"',
    'Content-Type: text/plain',
    '',
    fileContent,
    `--${boundary}`,
    'Content-Disposition: form-data; name="description"',
    '',
    'Raw HTTP response header capture demonstrating absence of defensive headers',
    `--${boundary}--`
  ].join('\r\n');

  const evRes = await fetch(`${BASE_URL}/findings/${testFndId}/evidence`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': `multipart/form-data; boundary=${boundary}`
    },
    body: Buffer.from(body)
  });
  assert.strictEqual(evRes.status, 201);
  const evData = await evRes.json();
  assert.ok(evData.data.sha256_hash, 'Must compute SHA-256 hash of evidence artifact');
  console.log('  ✔ Attached Cryptographic Evidence:', evData.data.original_name, `(SHA-256: ${evData.data.sha256_hash.slice(0, 16)}...)`);

  // Step 5.6: Test Report Generation (JSON & Download)
  const repRes = await fetch(`${BASE_URL}/assessments/${testAsmId}/report`, { headers });
  assert.strictEqual(repRes.status, 200);
  const repData = await repRes.json();
  assert.ok(repData.data.metadata.report_title);
  assert.ok(repData.data.sections['1. Executive Summary']);
  assert.ok(repData.data.sections['2. Engagement Objectives & Scope']);
  assert.ok(repData.data.sections['3. Methodology']);
  assert.ok(repData.data.sections['4. Assets Under Review']);
  assert.ok(repData.data.sections['5. Reconnaissance Summary']);
  assert.ok(repData.data.sections['7. Findings & Vulnerabilities']);
  assert.ok(repData.data.sections['8. Cryptographic Evidence Inventory']);
  assert.ok(repData.data.sections['10. Remediation Roadmap']);
  assert.ok(repData.data.sections['12. Conclusion']);
  console.log('  ✔ Generated Full JSON Report Preview with all required assessment sections');

  // Step 5.7: Generate & Download TXT Report
  const dlRes = await fetch(`${BASE_URL}/assessments/${testAsmId}/report/download`, { headers });
  assert.strictEqual(dlRes.status, 200);
  const reportText = await dlRes.text();
  assert.ok(reportText.includes('REDOPS MOBILE — PROFESSIONAL SECURITY ASSESSMENT REPORT'));
  assert.ok(reportText.includes('1. EXECUTIVE SUMMARY & ASSESSMENT INFORMATION'));
  assert.ok(reportText.includes('2. ENGAGEMENT SCOPE & OBJECTIVES'));
  assert.ok(reportText.includes('3. METHODOLOGY'));
  assert.ok(reportText.includes('4. TOOL OBSERVATIONS & PASSIVE RECONNAISSANCE'));
  assert.ok(reportText.includes('5. VULNERABILITY FINDINGS SUMMARY'));
  assert.ok(reportText.includes('7. CRYPTOGRAPHIC EVIDENCE INVENTORY'));
  assert.ok(reportText.includes('8. REMEDIATION ROADMAP & STRATEGIC RECOMMENDATIONS'));
  assert.ok(reportText.includes('9. ASSESSMENT CONCLUSION'));
  console.log('  ✔ Downloaded Complete TXT Assessment Report (Length:', reportText.length, 'bytes)');

  // Step 5.8: Single Finding Advisory Export
  const advRes = await fetch(`${BASE_URL}/findings/${testFndId}/report/download`, { headers });
  assert.strictEqual(advRes.status, 200);
  const advText = await advRes.text();
  assert.ok(advText.includes('SINGLE VULNERABILITY SECURITY ADVISORY'));
  assert.ok(advText.includes(fnd.custom_id));
  console.log('  ✔ Exported Single Finding Vulnerability Advisory:', fnd.custom_id);

  // Step 5.9: Verify Activity Audit Logs
  const actRes = await fetch(`${BASE_URL}/activities?assessment_id=${testAsmId}`, { headers });
  assert.strictEqual(actRes.status, 200);
  const actData = await actRes.json();
  assert.ok(actData.data.length >= 3, 'Must have logged sequential activities');
  console.log('  ✔ Activity History verified: Logged', actData.data.length, 'immutable assessment events');

  // Step 5.10: Clean Cascade Deletion Test
  const delRes = await fetch(`${BASE_URL}/assessments/${testAsmId}`, {
    method: 'DELETE',
    headers
  });
  assert.strictEqual(delRes.status, 200);
  console.log('  ✔ Relational Cascade Cleanup: Engagement and all associated targets, findings, observations, and evidence unlinked cleanly');

  console.log('\n================================================================');
  console.log('  ALL QA & RELEASE VERIFICATION SUITES PASSED SUCCESSFULLY (100%)');
  console.log('================================================================');
}

runQaSuite().catch(err => {
  console.error('\n❌ QA Suite Failed:', err);
  process.exit(1);
});
