const assert = require('node:assert');
const { calculateSubnet } = require('../src/tools/subnetTool');
const { calculateCvss31, parseCvssVector } = require('../src/tools/cvssTool');
const { convertFormat, generateHashes, identifyHash } = require('../src/tools/cryptoTool');
const { isValidDomain, isValidIp } = require('../src/tools/dnsTool');
const { normalizeUrl } = require('../src/tools/webSecurityTool');

console.log('Testing RedOps Cybersecurity Toolkit Modules...');

// 1. Subnet Calculator
console.log('1. Subnet Calculator:');
const sub = calculateSubnet('192.168.1.10/24');
assert.strictEqual(sub.network, '192.168.1.0');
assert.strictEqual(sub.broadcast, '192.168.1.255');
assert.strictEqual(sub.firstUsable, '192.168.1.1');
assert.strictEqual(sub.lastUsable, '192.168.1.254');
assert.strictEqual(sub.netmask, '255.255.255.0');
assert.strictEqual(sub.scope, 'Private');
console.log('  ✓ 192.168.1.10/24 subnet calculations passed');

// 2. CVSS v3.1 Calculator
console.log('2. CVSS v3.1 Calculator:');
// Log4Shell CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H -> 10.0 Critical
const log4j = parseCvssVector('CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H');
assert.strictEqual(log4j.baseScore, 10.0);
assert.strictEqual(log4j.severity, 'Critical');

// Medium CVE CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:C/C:L/I:L/A:N -> 5.4 Medium
const xss = calculateCvss31({
  AV: 'N', AC: 'L', PR: 'L', UI: 'R', S: 'C', C: 'L', I: 'L', A: 'N'
});
assert.strictEqual(xss.baseScore, 5.4);
assert.strictEqual(xss.severity, 'Medium');
console.log('  ✓ CVSS v3.1 calculation and vector parsing passed');

// 3. Crypto & Encoding Tool
console.log('3. Crypto & Encoding Tool:');
const enc = convertFormat('RedOps Security', 'plain', 'base64');
assert.strictEqual(enc.output, 'UmVkT3BzIFNlY3VyaXR5');
const dec = convertFormat('UmVkT3BzIFNlY3VyaXR5', 'base64', 'plain');
assert.strictEqual(dec.output, 'RedOps Security');

const rot = convertFormat('hello', 'plain', 'rot13');
assert.strictEqual(rot.output, 'uryyb');

const hashes = generateHashes('admin');
const md5Hash = hashes.hashes.find(h => h.algorithm === 'MD5').hex;
const sha256Hash = hashes.hashes.find(h => h.algorithm === 'SHA-256').hex;
assert.strictEqual(md5Hash, '21232f297a57a5a743894a0e4a801fc3');
assert.strictEqual(sha256Hash, '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918');

const idResult = identifyHash('21232f297a57a5a743894a0e4a801fc3');
assert(idResult.matched);
assert(idResult.candidates.some(c => c.algorithm === 'MD5'));
console.log('  ✓ Encodings, hash generation, and hash identification passed');

// 4. DNS & Web helpers
console.log('4. DNS & URL Validation:');
assert(isValidDomain('redops.local'));
assert(isValidDomain('sub.example.com'));
assert(!isValidDomain('invalid..domain'));
assert(isValidIp('10.0.0.1'));
assert(isValidIp('::1'));
assert.strictEqual(normalizeUrl('example.com'), 'https://example.com/');
console.log('  ✓ Domain, IP, and URL helpers passed');

console.log('ALL TOOLKIT UNIT CHECKS PASSED SUCCESSFULLY!');
