/**
 * RedOps Mobile - URL Analyzer Tool
 * Comprehensive URL parser, malformed detection, and suspicious indicator analysis.
 * Note: Suspicious indicators highlight structural anomalies without making definitive malicious verdicts.
 */

function analyzeUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') {
    throw new Error('Please enter a URL string to analyze.');
  }

  const trimmed = rawUrl.trim();
  let parsed;
  let isMalformed = false;
  let malformedReason = null;

  try {
    parsed = new URL(trimmed);
  } catch (err) {
    // Attempt auto-prefixing with https:// if scheme was omitted, but mark as notice
    try {
      if (!/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed)) {
        parsed = new URL(`https://${trimmed}`);
      } else {
        throw err;
      }
    } catch (secondErr) {
      isMalformed = true;
      malformedReason = err.message;
    }
  }

  if (isMalformed || !parsed) {
    return {
      raw_url: rawUrl,
      is_valid: false,
      is_malformed: true,
      error: malformedReason || 'Malformed URL syntax. Could not parse according to RFC 3986 / WHATWG URL specification.'
    };
  }

  // Parse Query Parameters
  const queryParams = [];
  parsed.searchParams.forEach((val, key) => {
    queryParams.push({
      key,
      value: val,
      decoded_value: decodeURIComponent(val),
      length: val.length
    });
  });

  // Evaluate Suspicious Characteristics (Non-judgmental heuristic indicators)
  const suspiciousIndicators = [];

  // 1. IP address in hostname (often used to bypass domain reputation)
  const isIpv4 = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(parsed.hostname);
  const isIpv6 = /^\[?[0-9a-fA-F:]+\]?$/.test(parsed.hostname) && parsed.hostname.includes(':');
  if (isIpv4 || isIpv6) {
    suspiciousIndicators.push({
      type: 'IP_ADDRESS_HOSTNAME',
      severity: 'WARNING',
      message: 'Hostname is an IP address rather than a registered domain name (bypasses standard domain reputation systems).'
    });
  }

  // 2. Embedded User Credentials / @ Symbol in Authority
  if (parsed.username || parsed.password || trimmed.includes('@')) {
    suspiciousIndicators.push({
      type: 'USERINFO_IN_URL',
      severity: 'HIGH',
      message: 'URL contains embedded user credentials or an "@" symbol before hostname, frequently used in URL spoofing and credential harvesting.'
    });
  }

  // 3. Non-standard Web Port
  const port = parsed.port;
  if (port && port !== '80' && port !== '443' && port !== '8080' && port !== '8443') {
    suspiciousIndicators.push({
      type: 'NON_STANDARD_PORT',
      severity: 'INFO',
      message: `Target connects on non-standard port :${port} instead of standard HTTP (80) or HTTPS (443).`
    });
  }

  // 4. Excessive Subdomains (Deep domain levels)
  const hostParts = parsed.hostname.split('.');
  if (hostParts.length > 4 && !isIpv4) {
    suspiciousIndicators.push({
      type: 'EXCESSIVE_SUBDOMAINS',
      severity: 'WARNING',
      message: `Hostname contains ${hostParts.length} levels/subdomains (common in phishing campaigns and dynamic DNS abuse).`
    });
  }

  // 5. Punycode / Homograph IDN (Internationalized Domain Names)
  if (parsed.hostname.startsWith('xn--') || /[^\u0000-\u007F]/.test(parsed.hostname)) {
    suspiciousIndicators.push({
      type: 'PUNYCODE_HOMOGRAPH',
      severity: 'HIGH',
      message: 'Hostname utilizes Punycode (IDN) encoding or non-ASCII characters, which may be visually indistinguishable from legitimate brands (IDN homograph attack risk).'
    });
  }

  // 6. Suspicious TLD / Common Phishing TLDs
  const suspiciousTlds = ['top', 'xyz', 'icu', 'tk', 'ml', 'ga', 'cf', 'gq', 'buzz', 'fit', 'rest', 'ru'];
  const tld = hostParts[hostParts.length - 1]?.toLowerCase();
  if (tld && suspiciousTlds.includes(tld)) {
    suspiciousIndicators.push({
      type: 'HIGH_RISK_TLD',
      severity: 'INFO',
      message: `Domain uses top-level domain (.${tld}) with statistically elevated spam and deceptive usage ratios.`
    });
  }

  // 7. Excessive URL Length
  if (trimmed.length > 2048) {
    suspiciousIndicators.push({
      type: 'EXCESSIVE_LENGTH',
      severity: 'WARNING',
      message: `URL length (${trimmed.length} characters) exceeds common web server buffer limits (2048 chars).`
    });
  }

  // 8. Open Redirect or Nested URL in Query
  const nestedUrlParam = queryParams.find(p => /^https?:\/\//i.test(p.value) || p.key.toLowerCase().includes('redirect') || p.key.toLowerCase().includes('url') || p.key.toLowerCase().includes('next'));
  if (nestedUrlParam) {
    suspiciousIndicators.push({
      type: 'POTENTIAL_OPEN_REDIRECT_PARAM',
      severity: 'WARNING',
      message: `Query parameter "${nestedUrlParam.key}" contains a potential destination redirect target: "${nestedUrlParam.value}".`
    });
  }

  // 9. Hex / Double URL encoding in Path
  if (/%[0-9a-fA-F]{2}/.test(parsed.pathname) || /%25[0-9a-fA-F]{2}/.test(parsed.pathname)) {
    suspiciousIndicators.push({
      type: 'ENCODED_PATH_CHARACTERS',
      severity: 'INFO',
      message: 'URL path contains URL-encoded or double-encoded characters (%XX).'
    });
  }

  // 10. Insecure Protocol
  if (parsed.protocol === 'http:') {
    suspiciousIndicators.push({
      type: 'INSECURE_SCHEME',
      severity: 'INFO',
      message: 'URL uses unencrypted plaintext HTTP scheme (data in transit is unprotected against interception).'
    });
  }

  return {
    raw_url: rawUrl,
    is_valid: true,
    is_malformed: false,
    scheme: parsed.protocol.replace(':', ''),
    hostname: parsed.hostname,
    port: parsed.port || (parsed.protocol === 'https:' ? '443' : parsed.protocol === 'http:' ? '80' : 'default'),
    path: parsed.pathname || '/',
    query_string: parsed.search || '',
    query_param_count: queryParams.length,
    query_params: queryParams,
    fragment: parsed.hash ? parsed.hash.replace('#', '') : null,
    origin: parsed.origin,
    username: parsed.username || null,
    password: parsed.password ? '••••••••' : null,
    length: trimmed.length,
    suspicious_indicators_count: suspiciousIndicators.length,
    suspicious_indicators: suspiciousIndicators
  };
}

module.exports = {
  analyzeUrl
};

