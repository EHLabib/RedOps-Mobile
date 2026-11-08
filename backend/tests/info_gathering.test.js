const { test, describe } = require('node:test');
const assert = require('node:assert/strict');

const { resolveDns, reverseDns, lookupRdap } = require('../src/tools/dnsTool');
const { isValidIpv4, isValidIpv6, evaluateIpScope, getIpInformation } = require('../src/tools/ipInfoTool');
const { matchesHostname, explainConnectionError, normalizeUrl } = require('../src/tools/webSecurityTool');
const { validateSsrfSafety, isCloudMetadataOrLinkLocal } = require('../src/middleware/targetSecurity');

describe('Passive Reconnaissance & Information Gathering Suite', () => {

  describe('1. IP Address Information & Scope Verification', () => {
    test('Validates IPv4 and IPv6 syntax accurately', () => {
      assert.strictEqual(isValidIpv4('192.168.1.1'), true);
      assert.strictEqual(isValidIpv4('8.8.8.8'), true);
      assert.strictEqual(isValidIpv4('256.0.0.1'), false);
      assert.strictEqual(isValidIpv4('192.168.1'), false);

      assert.strictEqual(isValidIpv6('2001:4860:4860::8888'), true);
      assert.strictEqual(isValidIpv6('::1'), true);
      assert.strictEqual(isValidIpv6('127.0.0.1'), false);
      assert.strictEqual(isValidIpv6('invalid:ipv6::xyz'), false);
    });

    test('Classifies private and reserved IP scopes without querying public registries', async () => {
      // Loopback
      const loopbackInfo = await getIpInformation('127.0.0.1');
      assert.ok(loopbackInfo.scope.includes('Loopback'));
      assert.strictEqual(loopbackInfo.is_routable, false);
      assert.strictEqual(loopbackInfo.organization, 'Local Network / Host Authority');

      // RFC 1918 Private
      const privateInfo = await getIpInformation('10.50.1.1');
      assert.ok(privateInfo.scope.includes('Private'));
      assert.strictEqual(privateInfo.is_routable, false);

      // APIPA Link-Local
      const apipaInfo = await getIpInformation('169.254.10.20');
      assert.ok(apipaInfo.scope.includes('Link-Local'));
      assert.strictEqual(apipaInfo.is_routable, false);
    });

    test('Extracts attribution and states limitations on public IP queries', async () => {
      const publicInfo = await getIpInformation('8.8.8.8');
      assert.strictEqual(publicInfo.ip, '8.8.8.8');
      assert.strictEqual(publicInfo.ip_version, 'IPv4');
      assert.strictEqual(publicInfo.is_routable, true);
      assert.ok(publicInfo.data_provider.includes('Regional Internet Registry') || publicInfo.data_provider.includes('RFC'));
      assert.ok(publicInfo.limitations.toLowerCase().includes('administrative allocation'));
    });
  });

  describe('2. TLS Hostname Matching (RFC 6125)', () => {
    test('Matches exact hostnames case-insensitively', () => {
      assert.strictEqual(matchesHostname('example.com', 'example.com'), true);
      assert.strictEqual(matchesHostname('EXAMPLE.COM', 'example.com'), true);
      assert.strictEqual(matchesHostname('portal.acme.corp', 'portal.acme.corp'), true);
      assert.strictEqual(matchesHostname('portal.acme.corp', 'other.acme.corp'), false);
    });

    test('Matches wildcard hostnames strictly according to RFC 6125', () => {
      // *.example.com matches sub.example.com
      assert.strictEqual(matchesHostname('*.example.com', 'sub.example.com'), true);
      assert.strictEqual(matchesHostname('*.example.com', 'api.example.com'), true);

      // *.example.com must NOT match example.com itself
      assert.strictEqual(matchesHostname('*.example.com', 'example.com'), false);

      // *.example.com must NOT match nested subdomains (deep.sub.example.com)
      assert.strictEqual(matchesHostname('*.example.com', 'deep.sub.example.com'), false);
    });

    test('Explains connection failures accurately', () => {
      const refusedErr = { code: 'ECONNREFUSED', message: 'connect ECONNREFUSED 127.0.0.1:443' };
      const exp1 = explainConnectionError(refusedErr, '127.0.0.1', 443);
      assert.ok(exp1.includes('Connection refused'));

      const timeoutErr = { code: 'ETIMEDOUT', message: 'connect ETIMEDOUT' };
      const exp2 = explainConnectionError(timeoutErr, 'target.local', 443);
      assert.ok(exp2.includes('timed out'));

      const dnsErr = { code: 'ENOTFOUND', message: 'getaddrinfo ENOTFOUND fake.test.domain' };
      const exp3 = explainConnectionError(dnsErr, 'fake.test.domain', 443);
      assert.ok(exp3.includes('resolution failed'));
    });
  });

  describe('3. SSRF & Cloud Metadata Protections', () => {
    test('Detects link-local and cloud metadata IP addresses', () => {
      assert.strictEqual(isCloudMetadataOrLinkLocal('169.254.169.254'), true);
      assert.strictEqual(isCloudMetadataOrLinkLocal('169.254.170.2'), true);
      assert.strictEqual(isCloudMetadataOrLinkLocal('100.100.100.200'), true);
      assert.strictEqual(isCloudMetadataOrLinkLocal('8.8.8.8'), false);
      assert.strictEqual(isCloudMetadataOrLinkLocal('1.1.1.1'), false);
    });

    test('Rejects cloud metadata endpoints in validateSsrfSafety', async () => {
      await assert.rejects(
        () => validateSsrfSafety('http://169.254.169.254/latest/meta-data/'),
        /cloud metadata service/i
      );

      await assert.rejects(
        () => validateSsrfSafety('http://metadata.google.internal/computeMetadata/v1/'),
        /cloud metadata service/i
      );
    });

    test('Rejects unsafe URL protocols (e.g. file:, ftp:, gopher:)', async () => {
      assert.throws(
        () => normalizeUrl('file:///etc/passwd'),
        /Unsupported protocol/
      );

      assert.throws(
        () => normalizeUrl('gopher://evil.corp'),
        /Unsupported protocol/
      );
    });
  });

  describe('4. DNS Resolution Tool', () => {
    test('Resolves A records for public domains', async () => {
      const res = await resolveDns('google.com', 'A');
      assert.strictEqual(res.domain, 'google.com');
      assert.ok(Array.isArray(res.records));
      assert.ok(res.records.length > 0);
      assert.ok(res.records.some(r => r.type === 'A' && r.status === 'SUCCESS'));
    });

    test('Resolves MX records for domains with mail exchangers', async () => {
      const res = await resolveDns('google.com', 'MX');
      const mxRecord = res.records.find(r => r.type === 'MX');
      assert.ok(mxRecord && mxRecord.status === 'SUCCESS');
      assert.ok(Array.isArray(mxRecord.data));
    });

    test('Handles non-existent domain gracefully without crashing', async () => {
      const res = await resolveDns('thisdomaindefinitelydoesnotexist-redops-test-999.xyz', 'A');
      assert.ok(Array.isArray(res.records));
      assert.ok(res.records.every(r => r.status === 'ERROR' || r.status === 'EMPTY'));
    });
  });

  describe('5. Domain Information (RDAP Protocol)', () => {
    test('Queries legitimate public RDAP endpoints and handles privacy gracefully', async () => {
      const res = await lookupRdap('example.com');
      assert.strictEqual(res.query, 'example.com');
      assert.strictEqual(res.type, 'DOMAIN');
      assert.strictEqual(res.status, 'FOUND');
      assert.ok(res.registrar !== undefined);
      assert.ok(Array.isArray(res.nameservers));
    });
  });

});
