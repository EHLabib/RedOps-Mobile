const assert = require('node:assert');

async function testToolkitEndpoints() {
  console.log('Testing RedOps Mobile Toolkit API Endpoints on http://localhost:5000/api/v1/tools ...');

  // Authenticate session for toolkit access
  const loginRes = await fetch('http://localhost:5000/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'sh@dow_sec', password: 'Password123!Safe' })
  });
  const loginData = await loginRes.json();
  const token = loginData.token;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // 1. Subnet Calculator
  console.log('\n--- 1. Subnet Calculator ---');
  const subRes = await fetch('http://localhost:5000/api/v1/tools/network/subnet', {
    method: 'POST',
    headers,
    body: JSON.stringify({ cidr: '10.0.0.0/16' })
  });
  const subData = await subRes.json();
  assert.strictEqual(subData.status, 'success');
  assert.strictEqual(subData.data.network, '10.0.0.0');
  assert.strictEqual(subData.data.netmask, '255.255.0.0');
  assert.strictEqual(subData.data.scope, 'Private');
  console.log('  ✓ POST /tools/network/subnet ->', subData.data.cidrNotation, 'Usable Hosts:', subData.data.usableHosts);

  // 2. CVSS v3.1 Calculator
  console.log('\n--- 2. CVSS v3.1 Calculator ---');
  const cvssRes = await fetch('http://localhost:5000/api/v1/tools/cvss/calculate', {
    method: 'POST',
    headers,
    body: JSON.stringify({
      metrics: { AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H', A: 'H' }
    })
  });
  const cvssData = await cvssRes.json();
  assert.strictEqual(cvssData.status, 'success');
  assert.strictEqual(cvssData.data.baseScore, 9.8);
  assert.strictEqual(cvssData.data.severity, 'Critical');
  console.log('  ✓ POST /tools/cvss/calculate -> Score:', cvssData.data.baseScore, 'Severity:', cvssData.data.severity, 'Vector:', cvssData.data.vector);

  // 3. Multi-Format Converter
  console.log('\n--- 3. Multi-Format Converter ---');
  const encRes = await fetch('http://localhost:5000/api/v1/tools/crypto/convert', {
    method: 'POST',
    headers,
    body: JSON.stringify({ input: 'RedOps Authorized', from: 'plaintext', to: 'base64' })
  });
  const encData = await encRes.json();
  assert.strictEqual(encData.status, 'success');
  assert.strictEqual(encData.data.output, 'UmVkT3BzIEF1dGhvcml6ZWQ=');
  console.log('  ✓ POST /tools/crypto/convert -> Output:', encData.data.output);

  // 4. Crypto Hasher
  console.log('\n--- 4. Crypto Hasher ---');
  const hashRes = await fetch('http://localhost:5000/api/v1/tools/crypto/hash', {
    method: 'POST',
    headers,
    body: JSON.stringify({ input: 'password123', salt: 's@lt', hmacKey: 'secret' })
  });
  const hashData = await hashRes.json();
  assert.strictEqual(hashData.status, 'success');
  assert(hashData.data.hashes.length >= 6);
  assert(hashData.data.hmac);
  console.log('  ✓ POST /tools/crypto/hash -> Hashes count:', hashData.data.hashes.length, 'HMAC:', hashData.data.hmac.hex.slice(0, 16) + '...');

  // 5. Hash Identifier
  console.log('\n--- 5. Hash Identifier ---');
  const idRes = await fetch('http://localhost:5000/api/v1/tools/crypto/identify', {
    method: 'POST',
    headers,
    body: JSON.stringify({ hash: '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918' })
  });
  const idData = await idRes.json();
  assert.strictEqual(idData.status, 'success');
  assert(idData.data.matched);
  assert(idData.data.candidates.some(c => c.algorithm === 'SHA-256'));
  console.log('  ✓ POST /tools/crypto/identify -> Matched:', idData.data.candidates.map(c => c.algorithm).join(', '));

  // 6. Web Security Headers
  console.log('\n--- 6. Web Security Headers ---');
  // 6a. Blocked without authorization consent
  const unauthHeadRes = await fetch('http://localhost:5000/api/v1/tools/web/headers', {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://example.com' })
  });
  assert.strictEqual(unauthHeadRes.status, 403);
  console.log('  ✓ POST /tools/web/headers (unauthorized) -> Blocked with HTTP 403 AUTHORIZATION_REQUIRED');

  // 6b. Blocked by SSRF when targeting loopback
  const ssrfHeadRes = await fetch('http://localhost:5000/api/v1/tools/web/headers', {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'http://127.0.0.1:5000', authorized: true })
  });
  const ssrfData = await ssrfHeadRes.json();
  assert.strictEqual(ssrfData.status, 'error');
  assert(ssrfData.message.includes('loopback') || ssrfData.message.includes('Access Denied'));
  console.log('  ✓ POST /tools/web/headers (SSRF loopback protection) -> Correctly blocked internal access');

  // 6c. Successful audit with authorization
  const headRes = await fetch('http://localhost:5000/api/v1/tools/web/headers', {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://example.com', authorized: true })
  });
  const headData = await headRes.json();
  assert.strictEqual(headData.status, 'success');
  const secScore = headData.data.security_score !== undefined ? headData.data.security_score : headData.data.score;
  console.log('  ✓ POST /tools/web/headers (authorized) -> Score:', secScore + '%', 'Grade:', headData.data.grade, 'Status:', headData.data.status_code);

  // 7. Web Redirect Tracer
  console.log('\n--- 7. Web Redirect Tracer ---');
  const redRes = await fetch('http://localhost:5000/api/v1/tools/web/redirects', {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'http://example.com', authorized: true })
  });
  const redData = await redRes.json();
  assert.strictEqual(redData.status, 'success');
  console.log('  ✓ POST /tools/web/redirects -> Hops:', redData.data.total_hops, 'Final:', redData.data.final_url);

  // 8. URL Analyzer
  console.log('\n--- 8. URL Analyzer ---');
  const urlRes = await fetch('http://localhost:5000/api/v1/tools/web/url-analyze', {
    method: 'POST',
    headers,
    body: JSON.stringify({ url: 'https://admin:pass@example.com:8443/auth?dest=https://evil.corp#token' })
  });
  const urlData = await urlRes.json();
  assert.strictEqual(urlData.status, 'success');
  assert.strictEqual(urlData.data.hostname, 'example.com');
  assert.strictEqual(urlData.data.port, '8443');
  assert(urlData.data.suspicious_indicators.length > 0);
  console.log('  ✓ POST /tools/web/url-analyze -> Host:', urlData.data.hostname, 'Suspicious indicators:', urlData.data.suspicious_indicators_count);

  // 9. JWT Decoder
  console.log('\n--- 9. JWT Decoder ---');
  const headerB64 = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const payloadB64 = Buffer.from(JSON.stringify({ sub: 'user123', exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url');
  const jwtRes = await fetch('http://localhost:5000/api/v1/tools/crypto/jwt-decode', {
    method: 'POST',
    headers,
    body: JSON.stringify({ token: `${headerB64}.${payloadB64}.fakeSignature` })
  });
  const jwtData = await jwtRes.json();
  assert.strictEqual(jwtData.status, 'success');
  assert.strictEqual(jwtData.data.payload.sub, 'user123');
  assert.strictEqual(jwtData.data.is_expired, false);
  console.log('  ✓ POST /tools/crypto/jwt-decode -> Subject:', jwtData.data.payload.sub, 'Algorithm:', jwtData.data.algorithm);

  // 10. Password Strength Estimator
  console.log('\n--- 10. Password Strength Estimator ---');
  const pwdRes = await fetch('http://localhost:5000/api/v1/tools/crypto/password-strength', {
    method: 'POST',
    headers,
    body: JSON.stringify({ password: 'vK#9$mX!pL2@qR8^wZ5~' })
  });
  const pwdData = await pwdRes.json();
  assert.strictEqual(pwdData.status, 'success');
  assert.strictEqual(pwdData.data.strength, 'Very Strong');
  console.log('  ✓ POST /tools/crypto/password-strength -> Score:', pwdData.data.score + '%', 'Strength:', pwdData.data.strength, 'Entropy:', pwdData.data.entropy_bits, 'bits');

  // 11. JSON Formatter
  console.log('\n--- 11. JSON Formatter ---');
  const jsonRes = await fetch('http://localhost:5000/api/v1/tools/format/json', {
    method: 'POST',
    headers,
    body: JSON.stringify({ json: '{"tool":"json-formatter","status":"active"}', indent: 2 })
  });
  const jsonData = await jsonRes.json();
  assert.strictEqual(jsonData.status, 'success');
  assert.strictEqual(jsonData.data.is_valid, true);
  console.log('  ✓ POST /tools/format/json -> Parsed type:', jsonData.data.parsed_type, 'Items:', jsonData.data.item_count);

  // 12. IP Information & Scope
  console.log('\n--- 12. IP Information & Scope ---');
  const ipRes = await fetch('http://localhost:5000/api/v1/tools/dns/ip-info', {
    method: 'POST',
    headers,
    body: JSON.stringify({ ip: '192.168.1.1', authorized: true })
  });
  const ipData = await ipRes.json();
  assert.strictEqual(ipData.status, 'success');
  assert.strictEqual(ipData.data.is_routable, false);
  console.log('  ✓ POST /tools/dns/ip-info -> IP:', ipData.data.ip, 'Scope:', ipData.data.scope, 'Routable:', ipData.data.is_routable);

  console.log('\n======================================================');
  console.log('ALL TOOLKIT REST ENDPOINTS VERIFIED & FUNCTIONING 100%');
  console.log('======================================================');
}

testToolkitEndpoints().catch(err => {
  console.error('Toolkit API test failed:', err);
  process.exit(1);
});
