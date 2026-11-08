const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');

const { resolveDns, reverseDns, lookupRdap } = require('../tools/dnsTool');
const { getIpInformation } = require('../tools/ipInfoTool');
const { analyzeHttpHeaders, inspectSslCertificate, traceRedirects } = require('../tools/webSecurityTool');
const { convertFormat, generateHashes, identifyHash } = require('../tools/cryptoTool');
const { calculateSubnet } = require('../tools/subnetTool');
const { calculateCvss31, parseCvssVector } = require('../tools/cvssTool');
const { analyzeUrl } = require('../tools/urlAnalyzerTool');
const { decodeJwt } = require('../tools/jwtDecoderTool');
const { estimatePasswordStrength } = require('../tools/passwordStrengthTool');
const { formatJson } = require('../tools/jsonFormatterTool');
const { db, logActivity } = require('../database/db');
const { authenticateToken } = require('../middleware/auth');
const { requireTargetAuthorization, targetRateLimiter } = require('../middleware/targetSecurity');

// Enforce JWT authentication on all toolkit endpoints
router.use(authenticateToken);

function logToolAction(req, action, details) {
  if (req.user && req.user.id) {
    try {
      logActivity(req.user.id, action, details, null, req.ip);
    } catch (e) {
      console.warn('[ToolLogWarning]', e.message);
    }
  }
}

// ==============================================================================
// 1. PASSIVE RECONNAISSANCE ENDPOINTS
// ==============================================================================

/**
 * POST /api/v1/tools/dns/resolve
 * Body: { domain: string, type: 'A'|'AAAA'|'MX'|'TXT'|'NS'|'CNAME'|'SOA'|'ALL', authorized: boolean }
 */
router.post('/dns/resolve', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const { domain, type } = req.body;
    if (!domain) {
      return res.status(400).json({ status: 'error', message: 'Domain is required' });
    }

    const result = await resolveDns(domain, type || 'ALL');
    logToolAction(req, 'DNS_RESOLUTION', `Resolved ${type || 'ALL'} records for ${domain}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'DNS resolution failed'
    });
  }
});

/**
 * POST /api/v1/tools/dns/reverse
 * Body: { ip: string, authorized: boolean }
 */
router.post('/dns/reverse', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const { ip } = req.body;
    if (!ip) {
      return res.status(400).json({ status: 'error', message: 'IP address is required' });
    }

    const result = await reverseDns(ip);
    logToolAction(req, 'REVERSE_DNS', `Reverse DNS PTR lookup for ${ip}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Reverse DNS lookup failed'
    });
  }
});

/**
 * POST /api/v1/tools/dns/rdap
 * Body: { query: string, authorized: boolean } (domain or IP)
 */
router.post('/dns/rdap', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const { query } = req.body;
    if (!query) {
      return res.status(400).json({ status: 'error', message: 'Query target (domain or IP) is required' });
    }

    const result = await lookupRdap(query);
    logToolAction(req, 'RDAP_LOOKUP', `RDAP registration query for ${query}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'RDAP lookup failed'
    });
  }
});

/**
 * POST /api/v1/tools/dns/ip-info
 * Body: { ip: string, authorized: boolean }
 */
router.post('/dns/ip-info', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const { ip } = req.body;
    if (!ip) {
      return res.status(400).json({ status: 'error', message: 'IP address is required' });
    }

    const result = await getIpInformation(ip);
    logToolAction(req, 'IP_INFORMATION', `Retrieved IP information and registration for ${ip}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'IP information query failed'
    });
  }
});

/**
 * POST /api/v1/tools/network/subnet
 * Body: { cidr: string } (e.g. "192.168.1.0/24")
 */
router.post('/network/subnet', (req, res) => {
  try {
    const { cidr } = req.body;
    if (!cidr) {
      return res.status(400).json({ status: 'error', message: 'CIDR notation or IP/mask is required (e.g. 192.168.1.0/24)' });
    }

    const result = calculateSubnet(cidr);
    logToolAction(req, 'SUBNET_CALCULATION', `Calculated IPv4 subnet parameters for ${cidr}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Subnet calculation failed'
    });
  }
});

// ==============================================================================
// 2. WEB SECURITY INSPECTION ENDPOINTS
// ==============================================================================

/**
 * POST /api/v1/tools/web/headers
 * Body: { url: string, followRedirects: boolean, authorized: boolean }
 */
router.post('/web/headers', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const url = req.body.url || req.body.target;
    const { followRedirects } = req.body;
    if (!url) {
      return res.status(400).json({ status: 'error', message: 'Target URL is required' });
    }

    const result = await analyzeHttpHeaders(url, { followRedirects: followRedirects !== false });
    logToolAction(req, 'HTTP_HEADER_INSPECTION', `Audited HTTP security headers for ${url}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    const isDenied = err.message && (
      err.message.includes('Access Denied') ||
      err.message.includes('blocked') ||
      err.message.includes('prohibited') ||
      err.message.includes('Loopback') ||
      err.message.includes('Private') ||
      err.message.includes('Link-Local')
    );
    res.status(isDenied ? 403 : 400).json({
      status: 'error',
      message: err.message || 'HTTP Header inspection failed'
    });
  }
});

/**
 * POST /api/v1/tools/web/ssl
 * Body: { host: string, port: number, authorized: boolean }
 */
router.post('/web/ssl', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const host = req.body.host || req.body.target || req.body.url;
    const { port } = req.body;
    if (!host) {
      return res.status(400).json({ status: 'error', message: 'Target hostname is required' });
    }

    const result = await inspectSslCertificate(host, port || 443);
    logToolAction(req, 'SSL_CERT_INSPECTION', `Inspected TLS certificate for ${host}:${port || 443}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    const isDenied = err.message && (
      err.message.includes('Access Denied') ||
      err.message.includes('blocked') ||
      err.message.includes('prohibited')
    );
    res.status(isDenied ? 403 : 400).json({
      status: 'error',
      message: err.message || 'SSL Certificate inspection failed'
    });
  }
});

/**
 * POST /api/v1/tools/web/redirects
 * Body: { url: string, authorized: boolean }
 */
router.post('/web/redirects', requireTargetAuthorization, targetRateLimiter, async (req, res) => {
  try {
    const url = req.body.url || req.body.target;
    if (!url) {
      return res.status(400).json({ status: 'error', message: 'Target URL is required' });
    }

    const result = await traceRedirects(url);
    logToolAction(req, 'REDIRECT_TRACER', `Traced HTTP redirect chain for ${url}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    const isDenied = err.message && (
      err.message.includes('Access Denied') ||
      err.message.includes('blocked') ||
      err.message.includes('prohibited')
    );
    res.status(isDenied ? 403 : 400).json({
      status: 'error',
      message: err.message || 'Redirect trace failed'
    });
  }
});

// ==============================================================================
// 3. SECURITY UTILITIES ENDPOINTS
// ==============================================================================

/**
 * POST /api/v1/tools/crypto/convert
 * Body: { input: string, from: string, to: string }
 */
router.post('/crypto/convert', (req, res) => {
  try {
    const { input, from, to } = req.body;
    if (input === undefined || input === null) {
      return res.status(400).json({ status: 'error', message: 'Input text is required' });
    }

    const result = convertFormat(input, from || 'plain', to || 'base64');
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Encoding/decoding conversion failed'
    });
  }
});

/**
 * POST /api/v1/tools/crypto/hash
 * Body: { input: string, salt: string, hmacKey: string }
 */
router.post('/crypto/hash', (req, res) => {
  try {
    const { input, salt, hmacKey } = req.body;
    if (input === undefined || input === null) {
      return res.status(400).json({ status: 'error', message: 'Input text is required' });
    }

    const result = generateHashes(input, { salt, hmacKey });
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Cryptographic hash generation failed'
    });
  }
});

/**
 * POST /api/v1/tools/crypto/identify
 * Body: { hash: string }
 */
router.post('/crypto/identify', (req, res) => {
  try {
    const { hash } = req.body;
    if (!hash) {
      return res.status(400).json({ status: 'error', message: 'Hash string is required' });
    }

    const result = identifyHash(hash);
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Hash identification failed'
    });
  }
});

/**
 * POST /api/v1/tools/cvss/calculate
 * Body: { metrics: object } OR { vector: string }
 */
router.post('/cvss/calculate', (req, res) => {
  try {
    const { metrics, vector } = req.body;
    let result;

    if (vector) {
      result = parseCvssVector(vector);
    } else if (metrics) {
      result = calculateCvss31(metrics);
    } else {
      return res.status(400).json({
        status: 'error',
        message: 'Either metrics object or CVSS vector string must be provided'
      });
    }

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'CVSS calculation failed'
    });
  }
});

/**
 * POST /api/v1/tools/web/url-analyze
 * Body: { url: string }
 */
router.post('/web/url-analyze', (req, res) => {
  try {
    const { url } = req.body;
    if (!url) {
      return res.status(400).json({ status: 'error', message: 'URL string is required' });
    }

    const result = analyzeUrl(url);
    logToolAction(req, 'URL_ANALYSIS', `Analyzed URL structure and heuristics for ${url}`);

    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'URL analysis failed'
    });
  }
});

/**
 * POST /api/v1/tools/crypto/jwt-decode
 * Body: { token: string }
 */
router.post('/crypto/jwt-decode', (req, res) => {
  try {
    const { token } = req.body;
    if (!token) {
      return res.status(400).json({ status: 'error', message: 'JWT token string is required' });
    }

    const result = decodeJwt(token);
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'JWT decoding failed'
    });
  }
});

/**
 * POST /api/v1/tools/crypto/password-strength
 * Body: { password: string }
 */
router.post('/crypto/password-strength', (req, res) => {
  try {
    const { password } = req.body;
    const result = estimatePasswordStrength(password ?? '');
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'Password strength estimation failed'
    });
  }
});

/**
 * POST /api/v1/tools/format/json
 * Body: { json: string, indent: number }
 */
router.post('/format/json', (req, res) => {
  try {
    const { json, indent } = req.body;
    const result = formatJson(json, indent);
    res.json({
      status: 'success',
      data: result
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      message: err.message || 'JSON formatting failed'
    });
  }
});

/**
 * POST /api/v1/tools/save-result
 * Body: { assessment_id, tool_name, tool_category, target, title, summary, source_info, result_data, notes, status }
 */
router.post('/save-result', (req, res) => {
  const {
    assessment_id, tool_name, tool_category, target, title,
    summary, source_info, result_data, notes, status
  } = req.body;

  if (!assessment_id || !target) {
    return res.status(400).json({ status: 'error', message: 'assessment_id and target are required' });
  }

  const assessment = db.prepare('SELECT id, name FROM assessments WHERE id = ?').get(assessment_id);
  if (!assessment) {
    return res.status(404).json({ status: 'error', message: 'Assessment not found' });
  }

  const { sanitizeToolData } = require('../database/db');
  const sanitizedJson = result_data ? sanitizeToolData(tool_name, result_data) : null;
  const effectiveTitle = title || `${tool_name || 'Tool Result'}: ${target}`;
  const effectiveCategory = (tool_category || 'SECURITY_TOOLKIT').toUpperCase();
  const effectiveStatus = (status || 'INFORMATIONAL').toUpperCase();
  const id = 'rc-' + require('node:crypto').randomUUID().slice(0, 8);

  db.prepare(`
    INSERT INTO recon_observations (
      id, assessment_id, observation_type, value, notes,
      tool_name, tool_category, title, source_info, result_data, status
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    id,
    assessment_id,
    tool_name || 'Observation',
    target,
    notes || '',
    tool_name || 'Tool Result',
    effectiveCategory,
    effectiveTitle,
    source_info || 'Cybersecurity Toolkit Result',
    sanitizedJson,
    effectiveStatus
  );

  const username = req.user ? req.user.username : 'Operator';
  logActivity({
    assessment_id,
    title: `Saved Tool Result: ${effectiveTitle}`,
    type: 'TOOL_OBSERVATION',
    badge_class: 'badge-roe-target',
    icon: '💾',
    color: 'rgba(0, 229, 255, 0.2)',
    target_section: 'recon',
    target_keyword: target,
    asset: target,
    auth_reference: tool_name,
    rules_of_engagement: 'Authorized Assessment Observation',
    operator_title: username,
    description: `Persisted ${tool_name} observation for ${target}. Status: ${effectiveStatus}. Summary: ${summary || notes || 'Documented'}`
  });

  const created = db.prepare('SELECT * FROM recon_observations WHERE id = ?').get(id);
  res.status(201).json({ status: 'success', data: created });
});

module.exports = router;

