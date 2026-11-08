/**
 * REDOPS MOBILE — TARGET INSPECTION SECURITY SAFEGUARDS
 * Authorization gate, Rate Limiting, and SSRF / Cloud Metadata defenses.
 */

const dns = require('node:dns').promises;
const { URL } = require('node:url');

// In-memory sliding window rate limiter
const requestCounts = new Map();
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const MAX_REQUESTS_PER_WINDOW = 30; // 30 requests per minute

// Cleanup stale rate limit entries every 2 minutes
setInterval(() => {
  const now = Date.now();
  for (const [key, record] of requestCounts.entries()) {
    if (now - record.startTime > RATE_LIMIT_WINDOW_MS * 2) {
      requestCounts.delete(key);
    }
  }
}, 120000).unref();

/**
 * Middleware: Enforces explicit operator authorization confirmation
 */
function requireTargetAuthorization(req, res, next) {
  const bodyAuth = req.body && (
    req.body.authorized === true ||
    req.body.is_authorized === true ||
    req.body.authorized_consent === true ||
    req.body.authorized === 'true' ||
    req.body.is_authorized === 'true'
  );
  const queryAuth = req.query && (
    req.query.authorized === 'true' ||
    req.query.is_authorized === 'true'
  );
  const headerAuth = req.headers['x-target-authorized'] === 'true' || req.headers['x-target-authorized'] === '1';

  if (!bodyAuth && !queryAuth && !headerAuth) {
    return res.status(403).json({
      status: 'error',
      error_type: 'AUTHORIZATION_REQUIRED',
      message: 'Target-based inspection requires explicit authorization confirmation. Please check the authorization consent box confirming you have written authorization to assess this target.'
    });
  }
  next();
}

/**
 * Middleware: Rate limiter for outbound reconnaissance & inspection queries
 */
function targetRateLimiter(req, res, next) {
  const clientKey = req.user?.id || req.ip || 'anonymous';
  const now = Date.now();

  let clientRecord = requestCounts.get(clientKey);
  if (!clientRecord || (now - clientRecord.startTime > RATE_LIMIT_WINDOW_MS)) {
    clientRecord = {
      startTime: now,
      count: 1
    };
    requestCounts.set(clientKey, clientRecord);
  } else {
    clientRecord.count++;
  }

  res.setHeader('X-RateLimit-Limit', MAX_REQUESTS_PER_WINDOW);
  res.setHeader('X-RateLimit-Remaining', Math.max(0, MAX_REQUESTS_PER_WINDOW - clientRecord.count));

  if (clientRecord.count > MAX_REQUESTS_PER_WINDOW) {
    const retryAfterSeconds = Math.ceil((RATE_LIMIT_WINDOW_MS - (now - clientRecord.startTime)) / 1000);
    res.setHeader('Retry-After', retryAfterSeconds);
    return res.status(429).json({
      status: 'error',
      error_type: 'RATE_LIMIT_EXCEEDED',
      message: `Rate limit exceeded. To protect target resources and registry infrastructure, requests are limited to ${MAX_REQUESTS_PER_WINDOW} per minute. Please wait ${retryAfterSeconds} seconds before retrying.`
    });
  }

  next();
}

/**
 * Validates if an IP address is a blocked loopback, private, link-local, IPv6 ULA, or cloud metadata address
 */
function isBlockedIp(ipStr) {
  if (!ipStr) return { blocked: false };
  const clean = ipStr.trim().replace(/^\[|\]$/g, '').toLowerCase();

  // Cloud metadata endpoints
  if (clean === '169.254.169.254' || clean === '169.254.170.2' || clean === '100.100.100.200' || clean === 'fd00:ec2::254') {
    return { blocked: true, reason: 'Prohibited cloud metadata service (IMDS)' };
  }

  // Link-Local addresses (IPv4: 169.254.0.0/16, IPv6: fe80::/10)
  if (clean.startsWith('169.254.')) {
    return { blocked: true, reason: 'Prohibited link-local address space (169.254.0.0/16)' };
  }
  if (/^fe[89ab][0-9a-f]:/i.test(clean)) {
    return { blocked: true, reason: 'Prohibited IPv6 link-local address space (fe80::/10)' };
  }

  // Loopback addresses (127.0.0.0/8, ::1, 0.0.0.0/8)
  if (clean === '127.0.0.1' || clean === '::1' || clean === '0.0.0.0' || clean === '::' || clean.startsWith('127.')) {
    return { blocked: true, reason: 'Prohibited loopback address space (127.0.0.0/8, ::1)' };
  }

  // RFC 1918 Private network addresses
  if (/^10\./.test(clean) || /^192\.168\./.test(clean) || /^172\.(1[6-9]|2[0-9]|3[01])\./.test(clean)) {
    return { blocked: true, reason: 'Prohibited RFC 1918 private internal network space' };
  }

  // IPv6 Unique Local Address (ULA: fc00::/7, fd00::/8)
  if (/^f[cd][0-9a-f]{0,2}:/i.test(clean) || clean.startsWith('fc') || clean.startsWith('fd')) {
    return { blocked: true, reason: 'Prohibited IPv6 Unique Local Address (ULA: fc00::/7, fd00::/8)' };
  }

  // Carrier-Grade NAT (RFC 6598: 100.64.0.0/10)
  if (/^100\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\./.test(clean)) {
    return { blocked: true, reason: 'Prohibited shared Carrier-Grade NAT space (100.64.0.0/10)' };
  }

  // Multicast and Broadcast addresses
  if (/^(22[4-9]|23[0-9])\./.test(clean) || clean === '255.255.255.255' || /^ff[0-9a-f]{2}:/i.test(clean)) {
    return { blocked: true, reason: 'Prohibited multicast or broadcast address' };
  }

  return { blocked: false };
}

function isCloudMetadataOrLinkLocal(ipStr) {
  const check = isBlockedIp(ipStr);
  return check.blocked;
}

/**
 * Checks if target hostname or URL is safe against SSRF attacks
 */
async function validateSsrfSafety(targetUrlOrHost, options = {}) {
  const allowLocal = options.allowLocalhost === true || process.env.ALLOW_LOCAL_TARGETS === 'true';

  let hostname = '';
  let protocol = 'https:';

  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(targetUrlOrHost)) {
    try {
      const parsed = new URL(targetUrlOrHost);
      protocol = parsed.protocol.toLowerCase();
      hostname = parsed.hostname.toLowerCase();
    } catch {
      throw new Error(`Malformed URL syntax: "${targetUrlOrHost}"`);
    }
  } else {
    hostname = targetUrlOrHost.split('/')[0].split(':')[0].toLowerCase();
  }

  // Protocol whitelist
  if (protocol !== 'http:' && protocol !== 'https:') {
    throw new Error(`Unsupported URL scheme "${protocol}". Only http: and https: protocols are permitted.`);
  }

  // Cloud metadata domain blocks
  const cloudMetadataHostnames = [
    'metadata.google.internal',
    'metadata',
    'instance-data',
    '169.254.169.254',
    '169.254.170.2',
    '100.100.100.200'
  ];
  if (cloudMetadataHostnames.includes(hostname)) {
    throw new Error('Access Denied: Connections to cloud metadata services (IMDS) are strictly prohibited.');
  }

  // Direct IP check on hostname
  const directCheck = isBlockedIp(hostname);
  if (directCheck.blocked && !allowLocal) {
    throw new Error(`Access Denied: ${directCheck.reason} (${hostname}).`);
  }

  // Resolve hostname to IP to protect against DNS rebinding & private IP access
  try {
    const lookup = await dns.lookup(hostname, { all: true });
    for (const entry of lookup) {
      const resolvedIp = entry.address;
      const ipCheck = isBlockedIp(resolvedIp);
      if (ipCheck.blocked && !allowLocal) {
        throw new Error(`Access Denied: Resolved target address points to ${ipCheck.reason} (${resolvedIp}).`);
      }
    }
  } catch (err) {
    if (err.message && err.message.startsWith('Access Denied')) {
      throw err;
    }
    // DNS resolution failure will be handled by the calling tool
  }

  return true;
}

module.exports = {
  requireTargetAuthorization,
  targetRateLimiter,
  validateSsrfSafety,
  isCloudMetadataOrLinkLocal
};

