/**
 * RedOps Mobile - JWT Decoder Tool
 * 100% Local JSON Web Token parser, claims visualizer, timestamp interpreter.
 * 
 * IMPORTANT SECURITY NOTICE:
 * Decoding a JWT decodes the Base64URL payload and header. It does NOT cryptographically
 * verify the token signature or establish authenticity/trust.
 */

function base64UrlDecode(str) {
  // Convert Base64URL to standard Base64
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  // Pad with '='
  while (base64.length % 4) {
    base64 += '=';
  }
  const buffer = Buffer.from(base64, 'base64');
  return buffer.toString('utf8');
}

function formatTimestampExplanation(epochSeconds) {
  if (typeof epochSeconds !== 'number') return null;
  const date = new Date(epochSeconds * 1000);
  const now = Date.now();
  const diffMs = (epochSeconds * 1000) - now;
  const isPast = diffMs < 0;
  const absDiffSeconds = Math.abs(Math.floor(diffMs / 1000));

  let timeString = '';
  if (absDiffSeconds < 60) {
    timeString = `${absDiffSeconds} seconds`;
  } else if (absDiffSeconds < 3600) {
    timeString = `${Math.floor(absDiffSeconds / 60)} minutes`;
  } else if (absDiffSeconds < 86400) {
    timeString = `${Math.floor(absDiffSeconds / 3600)} hours`;
  } else {
    timeString = `${Math.floor(absDiffSeconds / 86400)} days`;
  }

  return {
    raw_epoch: epochSeconds,
    utc_string: date.toUTCString(),
    local_string: date.toLocaleString(),
    iso_string: date.toISOString(),
    is_expired: isPast,
    relative_time: isPast ? `${timeString} ago` : `in ${timeString}`
  };
}

function decodeJwt(rawToken) {
  if (!rawToken || typeof rawToken !== 'string') {
    throw new Error('Please provide a valid JWT token string.');
  }

  const token = rawToken.trim().replace(/^Bearer\s+/i, '');
  const parts = token.split('.');

  if (parts.length !== 3) {
    throw new Error(`Invalid JWT structure. Expected 3 dot-separated segments (Header.Payload.Signature), but received ${parts.length} segment(s).`);
  }

  const [rawHeader, rawPayload, signature] = parts;

  let header;
  let payload;

  try {
    const decodedHeader = base64UrlDecode(rawHeader);
    header = JSON.parse(decodedHeader);
  } catch (err) {
    throw new Error(`Failed to decode JWT Header: ${err.message}`);
  }

  try {
    const decodedPayload = base64UrlDecode(rawPayload);
    payload = JSON.parse(decodedPayload);
  } catch (err) {
    throw new Error(`Failed to decode JWT Payload: ${err.message}`);
  }

  // Interpret standard registered claims
  const claimExplanations = {};
  if (payload.exp !== undefined) {
    claimExplanations.exp = {
      label: 'Expiration Time (exp)',
      description: 'Identifies the expiration time on or after which the JWT must NOT be accepted for processing.',
      ...formatTimestampExplanation(payload.exp)
    };
  }
  if (payload.iat !== undefined) {
    claimExplanations.iat = {
      label: 'Issued At (iat)',
      description: 'Identifies the time at which the JWT was issued.',
      ...formatTimestampExplanation(payload.iat)
    };
  }
  if (payload.nbf !== undefined) {
    claimExplanations.nbf = {
      label: 'Not Before (nbf)',
      description: 'Identifies the time before which the JWT must NOT be accepted for processing.',
      ...formatTimestampExplanation(payload.nbf)
    };
  }
  if (payload.iss !== undefined) {
    claimExplanations.iss = {
      label: 'Issuer (iss)',
      value: payload.iss,
      description: 'Identifies the principal that issued the JWT.'
    };
  }
  if (payload.sub !== undefined) {
    claimExplanations.sub = {
      label: 'Subject (sub)',
      value: payload.sub,
      description: 'Identifies the principal that is the subject of the JWT (e.g. user ID).'
    };
  }
  if (payload.aud !== undefined) {
    claimExplanations.aud = {
      label: 'Audience (aud)',
      value: payload.aud,
      description: 'Identifies the recipients that the JWT is intended for.'
    };
  }
  if (payload.jti !== undefined) {
    claimExplanations.jti = {
      label: 'JWT ID (jti)',
      value: payload.jti,
      description: 'Provides a unique identifier for the JWT (replay defense).'
    };
  }

  const isExpired = claimExplanations.exp ? claimExplanations.exp.is_expired : null;

  return {
    is_valid_format: true,
    algorithm: header.alg || 'none',
    token_type: header.typ || 'JWT',
    header,
    payload,
    signature_preview: signature ? `${signature.slice(0, 16)}... (${signature.length} bytes)` : 'None (Unsigned token)',
    is_expired: isExpired,
    standard_claims: claimExplanations,
    disclaimer: 'DECODING ONLY: Decoding does NOT cryptographically verify the signature. Do not trust unverified claims from untrusted sources.'
  };
}

module.exports = {
  decodeJwt,
  base64UrlDecode
};

