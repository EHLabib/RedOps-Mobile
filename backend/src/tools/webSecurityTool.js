/**
 * REDOPS CYBERSECURITY TOOLKIT — WEB SECURITY INSPECTION ENGINE
 * Real, read-only HTTP Security Headers, SSL/TLS Certificates, and Redirect Tracing.
 * Strictly non-intrusive, SSRF-protected, and rate-limited.
 */

const tls = require('node:tls');
const https = require('node:https');
const http = require('node:http');
const { URL } = require('node:url');
const { validateSsrfSafety } = require('../middleware/targetSecurity');

/**
 * Standardize and validate target URL
 */
function normalizeUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') throw new Error('Target URL is required.');
  let cleaned = rawUrl.trim();
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//i.test(cleaned)) {
    const scheme = cleaned.split('://')[0].toLowerCase() + ':';
    if (!['http:', 'https:'].includes(scheme)) {
      throw new Error(`Unsupported protocol: "${scheme}". Only HTTP and HTTPS are permitted.`);
    }
  } else {
    cleaned = 'https://' + cleaned;
  }
  try {
    const parsed = new URL(cleaned);
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      throw new Error(`Unsupported protocol: "${parsed.protocol}". Only HTTP and HTTPS are permitted.`);
    }
    return parsed.href;
  } catch (err) {
    if (err.message.includes('Unsupported protocol')) throw err;
    throw new Error(`Invalid URL format: "${rawUrl}". Please provide a valid URL (e.g. https://example.com).`);
  }
}

/**
 * RFC 6125 compliant hostname matching (exact and wildcard)
 */
function matchesHostname(certName, targetHost) {
  if (!certName || !targetHost) return false;
  const cn = certName.toLowerCase().trim();
  const host = targetHost.toLowerCase().trim();

  if (cn === host) return true;

  // Wildcard matching: *.domain.tld matches sub.domain.tld, but not domain.tld or nested.sub.domain.tld
  if (cn.startsWith('*.')) {
    const wildcardDomain = cn.slice(2);
    const hostParts = host.split('.');
    const wildcardParts = wildcardDomain.split('.');
    if (hostParts.length === wildcardParts.length + 1) {
      const hostSuffix = hostParts.slice(1).join('.');
      if (hostSuffix === wildcardDomain) return true;
    }
  }

  return false;
}

/**
 * Explains network connection errors accurately
 */
function explainConnectionError(err, host, port) {
  const code = err.code || '';
  const msg = err.message || '';

  if (code === 'ECONNREFUSED' || msg.includes('ECONNREFUSED')) {
    return `Connection refused by ${host}:${port}. The target service is not accepting connections on this port.`;
  }
  if (code === 'ETIMEDOUT' || code === 'ESOCKETTIMEDOUT' || msg.includes('timed out')) {
    return `Connection to ${host}:${port} timed out after request timeout limit. Target did not respond.`;
  }
  if (code === 'ENOTFOUND' || msg.includes('ENOTFOUND') || msg.includes('getaddrinfo')) {
    return `Domain name resolution failed for "${host}". Host does not exist or DNS lookup timed out.`;
  }
  if (code === 'EHOSTUNREACH' || msg.includes('EHOSTUNREACH')) {
    return `Target host ${host} is unreachable via current routing table.`;
  }
  if (code === 'ECONNRESET' || msg.includes('ECONNRESET')) {
    return `Connection was unexpectedly reset by peer (${host}:${port}).`;
  }
  if (code === 'CERT_HAS_EXPIRED' || msg.includes('expired')) {
    return `TLS handshake rejected: Certificate presented by ${host} has expired.`;
  }
  if (code === 'DEPTH_ZERO_SELF_SIGNED_CERT' || msg.includes('self-signed')) {
    return `TLS certificate is self-signed and not signed by a recognized Certificate Authority.`;
  }
  return `Network connection failed: ${msg}`;
}

/**
 * Audit definition for HTTP security headers
 */
const SECURITY_HEADER_CHECKS = [
  {
    header: 'strict-transport-security',
    name: 'Strict-Transport-Security (HSTS)',
    category: 'Transport Security',
    evaluate: (val) => {
      if (!val) return { ok: false, issue: 'Missing HSTS header. Browsers may connect over unencrypted HTTP.', severity: 'MEDIUM' };
      const maxAgeMatch = val.match(/max-age=(\d+)/i);
      const maxAge = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) : 0;
      if (maxAge < 15552000) {
        return { ok: true, warning: true, details: `HSTS configured with short max-age (${maxAge}s). Recommended: at least 31536000s (1 year).`, severity: 'LOW' };
      }
      const includesSubdomains = /includesubdomains/i.test(val);
      const preload = /preload/i.test(val);
      return {
        ok: true,
        details: `Enforced for ${Math.round(maxAge / 86400)} days${includesSubdomains ? ', includes subdomains' : ''}${preload ? ', preload designated' : ''}.`
      };
    },
    remediation: 'Add "Strict-Transport-Security: max-age=31536000; includeSubDomains; preload" to ensure HTTPS enforcement.'
  },
  {
    header: 'content-security-policy',
    name: 'Content-Security-Policy (CSP)',
    category: 'Injection Defense',
    evaluate: (val) => {
      if (!val) return { ok: false, issue: 'Missing Content-Security-Policy. Browser will not restrict execution of injected scripts.', severity: 'HIGH' };
      const hasUnsafeInline = /'unsafe-inline'/i.test(val);
      const hasUnsafeEval = /'unsafe-eval'/i.test(val);
      if (hasUnsafeInline || hasUnsafeEval) {
        return {
          ok: true,
          warning: true,
          details: `Policy active but permits ${hasUnsafeInline ? "'unsafe-inline' " : ''}${hasUnsafeEval ? "'unsafe-eval'" : ''}, which weakens XSS mitigation.`,
          severity: 'LOW'
        };
      }
      return { ok: true, details: 'Strict Content-Security-Policy directive set.' };
    },
    remediation: 'Implement a Content-Security-Policy directive restricting script-src, object-src, and default-src.'
  },
  {
    header: 'x-content-type-options',
    name: 'X-Content-Type-Options',
    category: 'MIME Protection',
    evaluate: (val) => {
      if (!val) return { ok: false, issue: 'Missing X-Content-Type-Options. Vulnerable to MIME-type sniffing.', severity: 'LOW' };
      if (val.toLowerCase().includes('nosniff')) {
        return { ok: true, details: 'Correctly set to "nosniff".' };
      }
      return { ok: true, warning: true, details: `Value "${val}" does not specify "nosniff".`, severity: 'LOW' };
    },
    remediation: 'Add "X-Content-Type-Options: nosniff" to all HTTP responses.'
  },
  {
    header: 'x-frame-options',
    name: 'X-Frame-Options',
    category: 'Clickjacking Protection',
    evaluate: (val, allHeaders) => {
      const csp = allHeaders['content-security-policy'] || '';
      const hasFrameAncestors = /frame-ancestors/i.test(csp);
      if (!val && !hasFrameAncestors) {
        return { ok: false, issue: 'Missing X-Frame-Options and CSP frame-ancestors. Site may be framed in clickjacking attacks.', severity: 'MEDIUM' };
      }
      if (hasFrameAncestors) {
        return { ok: true, details: 'Clickjacking mitigated via CSP frame-ancestors directive.' };
      }
      const v = val.toUpperCase();
      if (v === 'DENY' || v === 'SAMEORIGIN') {
        return { ok: true, details: `Configured with "${v}". Framing restricted.` };
      }
      return { ok: true, warning: true, details: `Value "${val}" may not be supported by modern browsers. Prefer DENY or SAMEORIGIN.`, severity: 'LOW' };
    },
    remediation: 'Configure "X-Frame-Options: DENY" or "X-Frame-Options: SAMEORIGIN", or use CSP frame-ancestors.'
  },
  {
    header: 'referrer-policy',
    name: 'Referrer-Policy',
    category: 'Privacy & Information Disclosure',
    evaluate: (val) => {
      if (!val) return { ok: false, issue: 'Missing Referrer-Policy. Browser default may leak URL parameters to third-party referrers.', severity: 'LOW' };
      const strictValues = ['strict-origin-when-cross-origin', 'no-referrer', 'same-origin', 'strict-origin'];
      if (strictValues.some(sv => val.toLowerCase().includes(sv))) {
        return { ok: true, details: `Configured to privacy-preserving policy: "${val}".` };
      }
      return { ok: true, warning: true, details: `Policy "${val}" may leak complete URL path and query string cross-origin.`, severity: 'LOW' };
    },
    remediation: 'Set "Referrer-Policy: strict-origin-when-cross-origin" or "no-referrer".'
  },
  {
    header: 'permissions-policy',
    name: 'Permissions-Policy',
    category: 'Device API Hardening',
    evaluate: (val) => {
      if (!val) return { ok: false, issue: 'Missing Permissions-Policy. Browser APIs (camera, geolocation, mic) not explicitly restricted.', severity: 'INFO' };
      return { ok: true, details: 'Permissions-Policy configured to restrict device capabilities.' };
    },
    remediation: 'Configure Permissions-Policy (e.g. "geolocation=(), microphone=(), camera=()").'
  }
];

/**
 * Comprehensive HTTP Security Headers Inspection
 * Validates SSRF safety, follows max 5 redirects, evaluates defense-in-depth headers honestly.
 */
async function analyzeHttpHeaders(rawUrl, followRedirects = true) {
  const targetUrl = normalizeUrl(rawUrl);

  // Validate initial target SSRF safety
  await validateSsrfSafety(targetUrl);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);
  const startTime = Date.now();

  let res;
  let finalUrl = targetUrl;
  let redirectCount = 0;
  const maxRedirects = 5;

  try {
    if (followRedirects) {
      // Manual redirect loop to validate SSRF safety at EVERY hop
      let currentUrl = targetUrl;
      while (redirectCount <= maxRedirects) {
        await validateSsrfSafety(currentUrl);

        res = await fetch(currentUrl, {
          method: 'GET',
          redirect: 'manual',
          signal: controller.signal,
          headers: {
            'User-Agent': 'RedOps-Mobile-Security-Inspector/1.0 (Authorized Audit; +https://redops-platform.local)',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
          }
        });

        finalUrl = currentUrl;
        const location = res.headers.get('location');
        if ([301, 302, 303, 307, 308].includes(res.status) && location) {
          redirectCount++;
          if (redirectCount > maxRedirects) {
            throw new Error(`Exceeded maximum redirect limit (${maxRedirects} hops). Potential redirect loop.`);
          }
          currentUrl = new URL(location, currentUrl).href;
        } else {
          break; // Final destination reached
        }
      }
    } else {
      res = await fetch(targetUrl, {
        method: 'GET',
        redirect: 'manual',
        signal: controller.signal,
        headers: {
          'User-Agent': 'RedOps-Mobile-Security-Inspector/1.0 (Authorized Audit; +https://redops-platform.local)',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
        }
      });
    }
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error(`Request timed out after 8 seconds connecting to ${targetUrl}`);
    }
    throw new Error(`HTTP inspection failed: ${err.message}`);
  } finally {
    clearTimeout(timeoutId);
  }

  const responseTimeMs = Date.now() - startTime;
  const rawHeaders = {};
  for (const [key, value] of res.headers.entries()) {
    rawHeaders[key.toLowerCase()] = value;
  }

  const auditResults = [];
  let totalScore = 0;
  const maxPossibleScore = SECURITY_HEADER_CHECKS.length * 10;

  SECURITY_HEADER_CHECKS.forEach(check => {
    const headerValue = rawHeaders[check.header];
    const outcome = check.evaluate(headerValue, rawHeaders);

    if (outcome.ok) {
      totalScore += outcome.warning ? 6 : 10;
      auditResults.push({
        header: check.header,
        name: check.name,
        category: check.category,
        status: outcome.warning ? 'WARNING' : 'PASS',
        severity: outcome.severity || 'INFO',
        details: outcome.details,
        value: outcome.value || headerValue,
        remediation: outcome.warning ? check.remediation : null
      });
    } else {
      auditResults.push({
        header: check.header,
        name: check.name,
        category: check.category,
        status: 'FAIL',
        severity: outcome.severity || 'MEDIUM',
        details: outcome.issue,
        value: null,
        remediation: check.remediation
      });
    }
  });

  const percentageScore = Math.round((totalScore / maxPossibleScore) * 100);
  let grade = 'F';
  if (percentageScore >= 95) grade = 'A+';
  else if (percentageScore >= 85) grade = 'A';
  else if (percentageScore >= 70) grade = 'B';
  else if (percentageScore >= 50) grade = 'C';
  else if (percentageScore >= 35) grade = 'D';

  return {
    target_url: targetUrl,
    final_url: finalUrl,
    redirect_count: redirectCount,
    status_code: res.status,
    status_text: res.statusText,
    response_time_ms: responseTimeMs,
    security_score: percentageScore,
    grade,
    assessment_notice: 'Security headers provide defense-in-depth hardening. The absence of a recommended header does not automatically prove an exploitable vulnerability.',
    summary: {
      passed: auditResults.filter(r => r.status === 'PASS').length,
      warnings: auditResults.filter(r => r.status === 'WARNING').length,
      failed: auditResults.filter(r => r.status === 'FAIL').length
    },
    findings: auditResults,
    server: rawHeaders['server'] || 'Not Disclosed',
    content_type: rawHeaders['content-type'] || 'Unknown',
    all_headers: rawHeaders
  };
}

/**
 * Inspect live TLS/SSL Certificate details
 * Verifies issuer, validity dates, RFC 6125 hostname matching, and reports verification status honestly.
 */
function inspectSslCertificate(rawHost, port = 443) {
  return new Promise(async (resolve, reject) => {
    try {
      const cleanHost = (rawHost || '').trim().replace(/^https?:\/\//i, '').split('/')[0].split(':')[0];
      if (!cleanHost) return reject(new Error('Hostname is required for SSL inspection.'));

      // Validate SSRF & Cloud Metadata protection
      await validateSsrfSafety(`https://${cleanHost}`);

      const targetPort = parseInt(port, 10) || 443;
      let isResolved = false;

      const socket = tls.connect({
        host: cleanHost,
        port: targetPort,
        servername: cleanHost,
        rejectUnauthorized: false // Inspect peer cert even if untrusted/expired to extract diagnostics honestly
      }, () => {
        try {
          const cert = socket.getPeerCertificate(true);
          const protocol = socket.getProtocol();
          const cipher = socket.getCipher();
          const isAuthorized = socket.authorized === true;
          const authError = socket.authorizationError || null;

          if (!cert || Object.keys(cert).length === 0) {
            socket.destroy();
            isResolved = true;
            return resolve({
              host: cleanHost,
              port: targetPort,
              status: 'NO_CERTIFICATE',
              is_trusted: false,
              verification_status: 'NO_CERTIFICATE_PRESENTED',
              message: 'No peer TLS certificate was presented by target server.'
            });
          }

          const validFrom = new Date(cert.valid_from);
          const validTo = new Date(cert.valid_to);
          const now = new Date();
          const daysRemaining = Math.round((validTo.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
          const isExpired = daysRemaining < 0;

          // Subject Alternative Names
          const sans = cert.subjectaltname
            ? cert.subjectaltname.split(', ').map(s => s.replace(/^DNS:/, ''))
            : [];

          // Hostname match verification RFC 6125
          const cn = cert.subject?.CN || '';
          const candidateNames = sans.length > 0 ? sans : (cn ? [cn] : []);
          const hostnameMatched = candidateNames.some(name => matchesHostname(name, cleanHost));

          // Compute overall verification status honestly
          let verificationStatus = 'VERIFIED_TRUSTED';
          if (!isAuthorized) {
            verificationStatus = authError || 'UNTRUSTED_ROOT';
          } else if (isExpired) {
            verificationStatus = 'CERTIFICATE_EXPIRED';
          } else if (!hostnameMatched) {
            verificationStatus = 'HOSTNAME_MISMATCH';
          }

          const result = {
            host: cleanHost,
            port: targetPort,
            status: isExpired ? 'EXPIRED' : (daysRemaining <= 14 ? 'EXPIRING_SOON' : 'VALID'),
            is_trusted: isAuthorized && !isExpired && hostnameMatched,
            verification_status: verificationStatus,
            authorization_error: authError,
            hostname_matches: hostnameMatched,
            subject: {
              common_name: cert.subject?.CN || 'Unknown',
              organization: cert.subject?.O || 'None',
              organizational_unit: cert.subject?.OU || 'None'
            },
            issuer: {
              common_name: cert.issuer?.CN || 'Unknown',
              organization: cert.issuer?.O || 'Unknown',
              country: cert.issuer?.C || 'Unknown'
            },
            valid_from: cert.valid_from,
            valid_to: cert.valid_to,
            days_remaining: daysRemaining,
            is_expired: isExpired,
            san: sans,
            protocol_version: protocol,
            cipher_suite: cipher?.name,
            cipher_standard: cipher?.standardName,
            key_bits: cert.bits,
            serial_number: cert.serialNumber,
            fingerprint_sha256: cert.fingerprint256,
            fingerprint_sha1: cert.fingerprint
          };

          socket.destroy();
          isResolved = true;
          resolve(result);
        } catch (err) {
          socket.destroy();
          isResolved = true;
          reject(new Error(`Failed to parse TLS certificate: ${err.message}`));
        }
      });

      socket.setTimeout(7000, () => {
        if (!isResolved) {
          socket.destroy();
          isResolved = true;
          reject(new Error(`TLS connection to ${cleanHost}:${targetPort} timed out after 7 seconds.`));
        }
      });

      socket.on('error', (err) => {
        if (!isResolved) {
          socket.destroy();
          isResolved = true;
          const explained = explainConnectionError(err, cleanHost, targetPort);
          reject(new Error(explained));
        }
      });
    } catch (outerErr) {
      reject(outerErr);
    }
  });
}

/**
 * Traces HTTP Redirect Hops
 * Maximum 5 hops, SSRF-validated at each step.
 */
async function traceRedirects(rawUrl) {
  const initialUrl = normalizeUrl(rawUrl);
  let currentUrl = initialUrl;
  const hops = [];
  const maxHops = 5;

  for (let i = 0; i < maxHops; i++) {
    // Validate SSRF safety on hop
    await validateSsrfSafety(currentUrl);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);
    const hopStart = Date.now();

    try {
      const res = await fetch(currentUrl, {
        method: 'HEAD',
        redirect: 'manual',
        signal: controller.signal,
        headers: { 'User-Agent': 'RedOps-Mobile-Hop-Tracer/1.0 (Authorized Audit)' }
      });
      clearTimeout(timeoutId);

      const hopDuration = Date.now() - hopStart;
      const location = res.headers.get('location');

      hops.push({
        hop: i + 1,
        url: currentUrl,
        status: res.status,
        status_text: res.statusText,
        duration_ms: hopDuration,
        redirect_to: location || null
      });

      if ([301, 302, 303, 307, 308].includes(res.status) && location) {
        currentUrl = new URL(location, currentUrl).href;
      } else {
        break; // Final destination reached
      }
    } catch (err) {
      clearTimeout(timeoutId);
      hops.push({
        hop: i + 1,
        url: currentUrl,
        error: err.message
      });
      break;
    }
  }

  return {
    initial_url: initialUrl,
    final_url: currentUrl,
    total_hops: hops.length,
    hops
  };
}

module.exports = {
  normalizeUrl,
  matchesHostname,
  explainConnectionError,
  analyzeHttpHeaders,
  inspectSslCertificate,
  traceRedirects
};
