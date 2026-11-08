/**
 * REDOPS CYBERSECURITY TOOLKIT — SECURITY UTILITIES ENGINE
 * Cryptographic Hashing, Multi-Encoding/Decoding, and Hash Identification
 */

const crypto = require('node:crypto');

/**
 * Multi-Format Encoder / Decoder
 */
function convertFormat(input, fromFormat, toFormat) {
  if (input === undefined || input === null) {
    return { input: '', from: fromFormat, to: toFormat, output: '' };
  }
  const text = String(input);
  let from = (fromFormat || 'plaintext').toLowerCase().trim();
  let to = (toFormat || 'base64').toLowerCase().trim();

  if (from === 'plain' || from === 'utf8' || from === 'utf-8' || from === 'text') from = 'plaintext';
  if (to === 'plain' || to === 'utf8' || to === 'utf-8' || to === 'text') to = 'plaintext';

  // 1. Convert input to standard utf8 string or Buffer
  let buffer;
  try {
    switch (from) {
      case 'plaintext':
        buffer = Buffer.from(text, 'utf8');
        break;
      case 'base64':
        const cleanB64 = text.trim();
        if (cleanB64 && !/^[A-Za-z0-9+/=]+$/.test(cleanB64.replace(/\s+/g, ''))) {
          throw new Error('Input contains invalid Base64 characters.');
        }
        buffer = Buffer.from(cleanB64, 'base64');
        break;
      case 'base64url':
        const padded = text.trim().replace(/-/g, '+').replace(/_/g, '/');
        buffer = Buffer.from(padded, 'base64');
        break;
      case 'hex':
        const cleanHex = text.replace(/[\s:,-]/g, '');
        if (cleanHex.length % 2 !== 0) {
          throw new Error('Hexadecimal string must have an even number of characters.');
        }
        if (cleanHex && !/^[0-9a-fA-F]+$/.test(cleanHex)) {
          throw new Error('Input contains non-hexadecimal characters.');
        }
        buffer = Buffer.from(cleanHex, 'hex');
        break;
      case 'binary':
        const cleanBin = text.replace(/[^01]/g, '');
        const bytes = [];
        for (let i = 0; i < cleanBin.length; i += 8) {
          bytes.push(parseInt(cleanBin.substr(i, 8), 2));
        }
        buffer = Buffer.from(bytes);
        break;
      case 'url':
        buffer = Buffer.from(decodeURIComponent(text), 'utf8');
        break;
      case 'html':
        const unescaped = text
          .replace(/&amp;/g, '&')
          .replace(/&lt;/g, '<')
          .replace(/&gt;/g, '>')
          .replace(/&quot;/g, '"')
          .replace(/&#039;/g, "'")
          .replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
          .replace(/&#([0-9]+);/g, (_, dec) => String.fromCharCode(parseInt(dec, 10)));
        buffer = Buffer.from(unescaped, 'utf8');
        break;
      case 'rot13':
        const rot13Dec = text.replace(/[a-zA-Z]/g, c => {
          const base = c <= 'Z' ? 65 : 97;
          return String.fromCharCode((c.charCodeAt(0) - base + 13) % 26 + base);
        });
        buffer = Buffer.from(rot13Dec, 'utf8');
        break;
      default:
        buffer = Buffer.from(text, 'utf8');
    }
  } catch (err) {
    throw new Error(`Failed to decode from format "${from}": ${err.message}`);
  }

  // 2. Convert standard buffer to target format
  let output = '';
  try {
    switch (to) {
      case 'plaintext':
        output = buffer.toString('utf8');
        break;
      case 'base64':
        output = buffer.toString('base64');
        break;
      case 'base64url':
        output = buffer.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
        break;
      case 'hex':
        output = buffer.toString('hex');
        break;
      case 'binary':
        output = Array.from(buffer).map(b => b.toString(2).padStart(8, '0')).join(' ');
        break;
      case 'url':
        output = encodeURIComponent(buffer.toString('utf8'));
        break;
      case 'html':
        output = buffer.toString('utf8')
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/"/g, '&quot;')
          .replace(/'/g, '&#039;');
        break;
      case 'rot13':
        const plain = buffer.toString('utf8');
        output = plain.replace(/[a-zA-Z]/g, c => {
          const base = c <= 'Z' ? 65 : 97;
          return String.fromCharCode((c.charCodeAt(0) - base + 13) % 26 + base);
        });
        break;
      default:
        output = buffer.toString('utf8');
    }
  } catch (err) {
    throw new Error(`Failed to encode to format "${to}": ${err.message}`);
  }

  return {
    input,
    from,
    to,
    output,
    inputLength: text.length,
    outputLength: output.length
  };
}

/**
 * Generate standard cryptographic hashes
 */
function generateHashes(rawInput, saltOrOptions = '', hmacSecretArg = '') {
  const isBuffer = Buffer.isBuffer(rawInput);
  let salt = '';
  let hmacSecret = '';

  if (saltOrOptions && typeof saltOrOptions === 'object') {
    salt = String(saltOrOptions.salt || '');
    hmacSecret = String(saltOrOptions.hmacKey || saltOrOptions.hmacSecret || '');
  } else {
    salt = String(saltOrOptions || '');
    hmacSecret = String(hmacSecretArg || '');
  }

  let dataToHash;
  let byteLength = 0;

  if (isBuffer) {
    dataToHash = salt ? Buffer.concat([rawInput, Buffer.from(salt, 'utf8')]) : rawInput;
    byteLength = rawInput.length;
  } else {
    const input = String(rawInput ?? '');
    dataToHash = salt ? input + salt : input;
    byteLength = Buffer.byteLength(input, 'utf8');
  }

  const algorithms = [
    { name: 'MD5', algo: 'md5', bits: 128 },
    { name: 'SHA-1', algo: 'sha1', bits: 160 },
    { name: 'SHA-224', algo: 'sha224', bits: 224 },
    { name: 'SHA-256', algo: 'sha256', bits: 256 },
    { name: 'SHA-384', algo: 'sha384', bits: 384 },
    { name: 'SHA-512', algo: 'sha512', bits: 512 }
  ];

  const results = algorithms.map(({ name, algo, bits }) => {
    const hash = crypto.createHash(algo);
    if (Buffer.isBuffer(dataToHash)) {
      hash.update(dataToHash);
    } else {
      hash.update(dataToHash, 'utf8');
    }
    const hex = hash.digest('hex');
    return {
      algorithm: name,
      bit_length: bits,
      hex,
      base64: Buffer.from(hex, 'hex').toString('base64')
    };
  });

  let hmacResult = null;
  if (hmacSecret) {
    const hmac = crypto.createHmac('sha256', hmacSecret);
    if (Buffer.isBuffer(rawInput)) {
      hmac.update(rawInput);
    } else {
      hmac.update(String(rawInput ?? ''), 'utf8');
    }
    const hmacHex = hmac.digest('hex');
    hmacResult = {
      algorithm: 'HMAC-SHA256',
      secret_applied: true,
      hex: hmacHex,
      base64: Buffer.from(hmacHex, 'hex').toString('base64')
    };
  }

  return {
    input_type: isBuffer ? 'binary_buffer' : 'text',
    input_length: isBuffer ? byteLength : String(rawInput ?? '').length,
    byte_length: byteLength,
    salt_applied: !!salt,
    hashes: results,
    hmac: hmacResult
  };
}

/**
 * Hash Type Identifier
 */
function identifyHash(rawHash) {
  const hash = (rawHash || '').trim();
  if (!hash) throw new Error('Hash string is required for identification.');

  const cleanHex = hash.replace(/[\s:-]/g, '');
  const len = cleanHex.length;
  const isHexOnly = /^[0-9a-fA-F]+$/.test(cleanHex);

  const candidates = [];

  // Match by length and specific signatures
  if (/^\$2[aby]\$\d{2}\$[./0-9A-Za-z]{53}$/.test(hash)) {
    candidates.push({ algorithm: 'Bcrypt Blowfish', category: 'Password KDF', hashcat_mode: 3200, confidence: 'HIGH' });
  }

  if (/^\$argon2(id|i|d)\$v=\d+\$m=\d+,t=\d+,p=\d+\$[A-Za-z0-9+/=]+\$[A-Za-z0-9+/=]+$/.test(hash)) {
    candidates.push({ algorithm: 'Argon2 Password Hash', category: 'Memory-Hard KDF', hashcat_mode: 16000, confidence: 'HIGH' });
  }

  if (/^\$6\$[a-zA-Z0-9./]{1,16}\$[a-zA-Z0-9./]{86}$/.test(hash)) {
    candidates.push({ algorithm: 'SHA-512 Crypt (Linux /etc/shadow)', category: 'OS Password Hash', hashcat_mode: 1800, confidence: 'HIGH' });
  }

  if (/^\$5\$[a-zA-Z0-9./]{1,16}\$[a-zA-Z0-9./]{43}$/.test(hash)) {
    candidates.push({ algorithm: 'SHA-256 Crypt (Linux /etc/shadow)', category: 'OS Password Hash', hashcat_mode: 7400, confidence: 'HIGH' });
  }

  if (/^\$1\$[a-zA-Z0-9./]{1,8}\$[a-zA-Z0-9./]{22}$/.test(hash)) {
    candidates.push({ algorithm: 'MD5 Crypt (Unix)', category: 'OS Password Hash', hashcat_mode: 500, confidence: 'HIGH' });
  }

  if (isHexOnly) {
    switch (len) {
      case 32:
        candidates.push({ algorithm: 'MD5', category: 'Cryptographic Hash', hashcat_mode: 0, confidence: 'HIGH' });
        candidates.push({ algorithm: 'NTLM', category: 'Windows Hash', hashcat_mode: 1000, confidence: 'HIGH' });
        candidates.push({ algorithm: 'MD4', category: 'Legacy Hash', hashcat_mode: 900, confidence: 'MEDIUM' });
        break;
      case 40:
        candidates.push({ algorithm: 'SHA-1', category: 'Cryptographic Hash', hashcat_mode: 100, confidence: 'HIGH' });
        candidates.push({ algorithm: 'MySQL 4.1 / 5+ (SHA1(SHA1($pass)))', category: 'Database Hash', hashcat_mode: 300, confidence: 'MEDIUM' });
        break;
      case 56:
        candidates.push({ algorithm: 'SHA-224', category: 'Cryptographic Hash', hashcat_mode: null, confidence: 'HIGH' });
        candidates.push({ algorithm: 'SHA3-224', category: 'Cryptographic Hash', hashcat_mode: null, confidence: 'MEDIUM' });
        break;
      case 64:
        candidates.push({ algorithm: 'SHA-256', category: 'Cryptographic Hash', hashcat_mode: 1400, confidence: 'HIGH' });
        candidates.push({ algorithm: 'Keccak-256 / SHA3-256', category: 'Cryptographic Hash', hashcat_mode: 5000, confidence: 'MEDIUM' });
        break;
      case 96:
        candidates.push({ algorithm: 'SHA-384', category: 'Cryptographic Hash', hashcat_mode: 10800, confidence: 'HIGH' });
        break;
      case 128:
        candidates.push({ algorithm: 'SHA-512', category: 'Cryptographic Hash', hashcat_mode: 1700, confidence: 'HIGH' });
        candidates.push({ algorithm: 'Whirlpool', category: 'Cryptographic Hash', hashcat_mode: 6100, confidence: 'LOW' });
        break;
    }
  }

  if (candidates.length === 0) {
    return {
      hash,
      char_length: hash.length,
      is_hex: isHexOnly,
      matched: false,
      message: 'Unknown hash signature or unsupported encoding format.'
    };
  }

  return {
    hash,
    char_length: hash.length,
    is_hex: isHexOnly,
    matched: true,
    candidate_count: candidates.length,
    candidates
  };
}

module.exports = {
  convertFormat,
  generateHashes,
  identifyHash
};
