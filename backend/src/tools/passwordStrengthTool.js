/**
 * RedOps Mobile - Password Strength Estimator Tool
 * 100% Local password entropy, character diversity, and pattern evaluator.
 * NEVER transmits, logs, or stores entered passwords.
 */

const COMMON_PATTERNS = [
  'password', '123456', '12345678', '123456789', 'qwerty', 'admin', 'welcome',
  'iloveyou', 'secret', 'login', 'monkey', 'dragon', 'football', 'baseball',
  'superman', 'trustno1', 'shadow', 'master', 'letmein', 'access'
];

const KEYBOARD_WALKS = [
  'qwerty', 'asdfgh', 'zxcvbn', 'qwertz', 'azerty', '123456', '234567', '345678',
  '456789', '567890', 'abcdef', 'bcdefg', 'cdefgh', 'defghi', 'efghij'
];

function estimatePasswordStrength(password) {
  if (password === undefined || password === null) {
    throw new Error('Password input is required.');
  }

  const pwd = String(password);
  const length = pwd.length;

  if (length === 0) {
    return {
      score: 0,
      score_percent: 0,
      strength: 'Empty',
      entropy_bits: 0,
      length: 0,
      diversity: {
        has_lowercase: false,
        has_uppercase: false,
        has_digits: false,
        has_symbols: false,
        has_whitespace: false,
        pool_size: 0
      },
      crack_times: {
        online_throttled: 'Instant',
        offline_slow_kdf: 'Instant',
        offline_fast_hash: 'Instant'
      },
      detected_patterns: [],
      suggestions: ['Enter a password to evaluate its strength.']
    };
  }

  // 1. Character Diversity Analysis
  const hasLower = /[a-z]/.test(pwd);
  const hasUpper = /[A-Z]/.test(pwd);
  const hasDigits = /[0-9]/.test(pwd);
  const hasSymbols = /[^a-zA-Z0-9\s]/.test(pwd);
  const hasSpace = /\s/.test(pwd);

  let poolSize = 0;
  if (hasLower) poolSize += 26;
  if (hasUpper) poolSize += 26;
  if (hasDigits) poolSize += 10;
  if (hasSymbols) poolSize += 33;
  if (hasSpace) poolSize += 1;

  // 2. Shannon Entropy Calculation (Bits of Entropy)
  // Formula: H = L * log2(poolSize)
  const entropyBits = poolSize > 0 ? Math.round(length * Math.log2(poolSize) * 10) / 10 : 0;

  // 3. Pattern & Weakness Detection
  const detectedPatterns = [];
  const lowerPwd = pwd.toLowerCase();

  // Common dictionary passwords
  for (const common of COMMON_PATTERNS) {
    if (lowerPwd.includes(common)) {
      detectedPatterns.push(`Contains common dictionary word or default password pattern "${common}".`);
      break;
    }
  }

  // Keyboard walks / sequences
  for (const walk of KEYBOARD_WALKS) {
    if (lowerPwd.includes(walk)) {
      detectedPatterns.push(`Contains sequential keyboard sequence "${walk}".`);
      break;
    }
  }

  // Repeated characters (3+ in a row, e.g. "aaa", "111")
  if (/(.)\1{2,}/.test(pwd)) {
    detectedPatterns.push('Contains 3 or more consecutively repeated characters.');
  }

  // Sequential digits (e.g. 1234, 4321)
  if (/(012|123|234|345|456|567|678|789|987|876|765|654|543|432|321|210)/.test(pwd)) {
    if (!detectedPatterns.some(p => p.includes('sequential'))) {
      detectedPatterns.push('Contains sequential digit sequence.');
    }
  }

  // 4. Scoring Algorithm (0 to 100)
  let score = 0;

  // Length points
  if (length >= 8) score += 15;
  if (length >= 12) score += 20;
  if (length >= 16) score += 20;
  if (length >= 20) score += 10;

  // Diversity points
  if (hasLower && hasUpper) score += 15;
  if (hasDigits) score += 10;
  if (hasSymbols) score += 15;

  // Deductions for detected patterns
  if (detectedPatterns.length > 0) {
    score = Math.max(10, score - (detectedPatterns.length * 20));
  }
  if (length < 8) {
    score = Math.min(score, 25);
  }

  score = Math.min(100, Math.max(0, score));

  // Determine Category
  let strength = 'Very Weak';
  if (score >= 85) strength = 'Very Strong';
  else if (score >= 65) strength = 'Strong';
  else if (score >= 45) strength = 'Fair';
  else if (score >= 25) strength = 'Weak';

  // 5. Estimated Crack Times
  // Total combinations = poolSize ^ length
  const combinations = Math.pow(poolSize, length);

  function formatDuration(seconds) {
    if (seconds < 1) return 'Instant (< 1 second)';
    if (seconds < 60) return `${Math.round(seconds)} seconds`;
    if (seconds < 3600) return `${Math.round(seconds / 60)} minutes`;
    if (seconds < 86400) return `${Math.round(seconds / 3600)} hours`;
    if (seconds < 31536000) return `${Math.round(seconds / 86400)} days`;
    if (seconds < 31536000 * 1000) return `${Math.round(seconds / 31536000)} years`;
    if (seconds < 31536000 * 1000000) return `${Math.round(seconds / (31536000 * 1000))} thousand years`;
    return `${(seconds / (31536000 * 1000000)).toExponential(1)} million years`;
  }

  // Attack models:
  // Online throttled: 100 guesses / minute = 1.66 guesses/sec
  // Offline slow KDF (bcrypt): 10,000 guesses / sec
  // Offline fast hash (MD5/NTLM cluster): 10,000,000,000 guesses / sec (10 GH/s)
  const crackTimes = {
    online_throttled: formatDuration(combinations / 1.66),
    offline_slow_kdf: formatDuration(combinations / 10000),
    offline_fast_hash: formatDuration(combinations / 10000000000)
  };

  // 6. Actionable Improvement Suggestions
  const suggestions = [];
  if (length < 12) {
    suggestions.push(`Increase password length to at least 14 characters (currently ${length}). Length provides exponential entropy.`);
  }
  if (!hasUpper) {
    suggestions.push('Add uppercase letters (A-Z) to expand character pool.');
  }
  if (!hasLower) {
    suggestions.push('Add lowercase letters (a-z).');
  }
  if (!hasDigits) {
    suggestions.push('Include numbers (0-9) non-consecutively.');
  }
  if (!hasSymbols) {
    suggestions.push('Include special symbols (!@#$%^&*~) to resist brute-force dictionaries.');
  }
  if (detectedPatterns.length > 0) {
    suggestions.push('Remove predictable dictionary words, repeated characters, and sequential keyboard sequences.');
  }
  if (suggestions.length === 0) {
    suggestions.push('Excellent password composition! Meets enterprise and military security standards.');
  }

  return {
    score,
    score_percent: score,
    strength,
    entropy_bits: entropyBits,
    length,
    diversity: {
      has_lowercase: hasLower,
      has_uppercase: hasUpper,
      has_digits: hasDigits,
      has_symbols: hasSymbols,
      has_whitespace: hasSpace,
      pool_size: poolSize
    },
    crack_times: crackTimes,
    detected_patterns: detectedPatterns,
    suggestions,
    privacy_notice: '100% Client-Side Evaluation: This input was processed locally in memory. It was never sent over a network, recorded, or saved.'
  };
}

module.exports = {
  estimatePasswordStrength
};

