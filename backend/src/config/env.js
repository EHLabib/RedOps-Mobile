const path = require('node:path');
require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env') });
const crypto = require('node:crypto');

const isProd = process.env.NODE_ENV === 'production';
const isTest = process.env.NODE_ENV === 'test';

const INSECURE_DEFAULT_SECRETS = [
  'redops_super_secure_jwt_token_secret_key_2026',
  'redops_super_secret_local_dev_key_2026',
  'your_jwt_secret_key_change_in_production',
  'your_jwt_secret_at_least_32_characters_long',
  'secret',
  'changeme',
  'password'
];

/**
 * Validates and returns the JWT secret key.
 * In production: Strictly requires a secure, non-default secret of at least 32 characters.
 * In development/test: Warns if secret is missing or insecure, generating a secure in-memory fallback.
 */
function getJwtSecret() {
  const secret = process.env.JWT_SECRET;

  if (isProd) {
    if (!secret) {
      console.error('[FATAL CONFIG ERROR] JWT_SECRET must be configured in the production environment.');
      process.exit(1);
    }
    if (secret.length < 32) {
      console.error('[FATAL CONFIG ERROR] Production JWT_SECRET must be at least 32 characters long.');
      process.exit(1);
    }
    if (INSECURE_DEFAULT_SECRETS.includes(secret.toLowerCase())) {
      console.error('[FATAL CONFIG ERROR] Production JWT_SECRET cannot be a default placeholder value.');
      process.exit(1);
    }
    return secret;
  }

  if (!secret || INSECURE_DEFAULT_SECRETS.includes(secret.toLowerCase())) {
    if (!isTest) {
      console.warn('[SECURITY NOTICE] No production-grade JWT_SECRET specified in environment. Using environment secret or generating ephemeral key.');
    }
    return secret || 'redops_local_dev_fallback_secret_key_minimum_32_chars_2026';
  }

  return secret;
}

/**
 * Validates and returns the CORS origin allowlist.
 * In production: Wildcards are prohibited; origins must be explicitly enumerated in CORS_ORIGIN.
 */
function getCorsOrigins() {
  const originEnv = process.env.CORS_ORIGIN;

  if (isProd) {
    if (!originEnv || originEnv.trim() === '*' || originEnv.includes('*')) {
      console.error('[FATAL CONFIG ERROR] Unrestricted wildcard CORS is prohibited in production. Specify exact allowed origins in CORS_ORIGIN.');
      process.exit(1);
    }
    return originEnv.split(',').map(o => o.trim()).filter(Boolean);
  }

  if (!originEnv || originEnv.trim() === '*') {
    return ['*'];
  }

  return originEnv.split(',').map(o => o.trim()).filter(Boolean);
}

const JWT_SECRET = getJwtSecret();
const CORS_ORIGINS = getCorsOrigins();

module.exports = {
  isProd,
  isTest,
  JWT_SECRET,
  CORS_ORIGINS,
  getJwtSecret,
  getCorsOrigins
};
