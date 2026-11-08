/**
 * RedOps Mobile - CVSS v3.1 Calculator Tool
 * Official FIRST CVSS v3.1 specification compliant calculator and vector parser.
 * Reference: https://www.first.org/cvss/v3.1/specification-document
 */

const CVSS_WEIGHTS = {
  AV: { N: 0.85, A: 0.62, L: 0.55, P: 0.2 },
  AC: { L: 0.77, H: 0.44 },
  PR: {
    // PR depends on Scope (Unchanged vs Changed)
    U: { N: 0.85, L: 0.62, H: 0.27 },
    C: { N: 0.85, L: 0.68, H: 0.50 }
  },
  UI: { N: 0.85, R: 0.62 },
  S:  { U: 'Unchanged', C: 'Changed' },
  C:  { H: 0.56, L: 0.22, N: 0 },
  I:  { H: 0.56, L: 0.22, N: 0 },
  A:  { H: 0.56, L: 0.22, N: 0 }
};

const METRIC_LABELS = {
  AV: { N: 'Network', A: 'Adjacent Network', L: 'Local', P: 'Physical' },
  AC: { L: 'Low', H: 'High' },
  PR: { N: 'None', L: 'Low', H: 'High' },
  UI: { N: 'None', R: 'Required' },
  S:  { U: 'Unchanged', C: 'Changed' },
  C:  { H: 'High', L: 'Low', N: 'None' },
  I:  { H: 'High', L: 'Low', N: 'None' },
  A:  { H: 'High', L: 'Low', N: 'None' }
};

/**
 * CVSS v3.1 Roundup function:
 * Defined as the smallest number, specified to one decimal place,
 * that is equal to or higher than its input.
 */
function cvssRoundUp(input) {
  const intInput = Math.round(input * 100000);
  if (intInput % 10000 === 0) {
    return intInput / 100000;
  }
  return (Math.floor(intInput / 10000) + 1) / 10;
}

function getSeverity(score) {
  if (score === 0.0) return 'None';
  if (score < 4.0) return 'Low';
  if (score < 7.0) return 'Medium';
  if (score < 9.0) return 'High';
  return 'Critical';
}

function calculateCvss31(metrics) {
  const av = (metrics.AV || 'N').toUpperCase();
  const ac = (metrics.AC || 'L').toUpperCase();
  const pr = (metrics.PR || 'N').toUpperCase();
  const ui = (metrics.UI || 'N').toUpperCase();
  const s  = (metrics.S  || 'U').toUpperCase();
  const c  = (metrics.C  || 'N').toUpperCase();
  const i  = (metrics.I  || 'N').toUpperCase();
  const a  = (metrics.A  || 'N').toUpperCase();

  // Validate metrics
  if (!CVSS_WEIGHTS.AV[av]) throw new Error(`Invalid Attack Vector (AV): ${av}`);
  if (!CVSS_WEIGHTS.AC[ac]) throw new Error(`Invalid Attack Complexity (AC): ${ac}`);
  if (!CVSS_WEIGHTS.PR.U[pr]) throw new Error(`Invalid Privileges Required (PR): ${pr}`);
  if (!CVSS_WEIGHTS.UI[ui]) throw new Error(`Invalid User Interaction (UI): ${ui}`);
  if (!CVSS_WEIGHTS.S[s]) throw new Error(`Invalid Scope (S): ${s}`);
  if (CVSS_WEIGHTS.C[c] === undefined) throw new Error(`Invalid Confidentiality (C): ${c}`);
  if (CVSS_WEIGHTS.I[i] === undefined) throw new Error(`Invalid Integrity (I): ${i}`);
  if (CVSS_WEIGHTS.A[a] === undefined) throw new Error(`Invalid Availability (A): ${a}`);

  const isScopeChanged = s === 'C';
  const prWeight = CVSS_WEIGHTS.PR[s][pr];
  const avWeight = CVSS_WEIGHTS.AV[av];
  const acWeight = CVSS_WEIGHTS.AC[ac];
  const uiWeight = CVSS_WEIGHTS.UI[ui];
  const cWeight = CVSS_WEIGHTS.C[c];
  const iWeight = CVSS_WEIGHTS.I[i];
  const aWeight = CVSS_WEIGHTS.A[a];

  // Exploitability
  const exploitability = 8.22 * avWeight * acWeight * prWeight * uiWeight;

  // Impact Sub-score (ISS)
  const iss = 1 - ((1 - cWeight) * (1 - iWeight) * (1 - aWeight));

  let impact = 0;
  let baseScore = 0;

  if (iss <= 0) {
    baseScore = 0.0;
  } else {
    if (!isScopeChanged) {
      impact = 6.42 * iss;
      baseScore = cvssRoundUp(Math.min(impact + exploitability, 10));
    } else {
      impact = 7.52 * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15);
      baseScore = cvssRoundUp(Math.min(1.08 * (impact + exploitability), 10));
    }
  }

  const vector = `CVSS:3.1/AV:${av}/AC:${ac}/PR:${pr}/UI:${ui}/S:${s}/C:${c}/I:${i}/A:${a}`;
  const severity = getSeverity(baseScore);

  return {
    vector,
    baseScore: Number(baseScore.toFixed(1)),
    severity,
    exploitabilityScore: Number(exploitability.toFixed(1)),
    impactScore: Number(impact.toFixed(1)),
    iss: Number(iss.toFixed(3)),
    metrics: {
      AV: { code: av, label: METRIC_LABELS.AV[av], value: avWeight },
      AC: { code: ac, label: METRIC_LABELS.AC[ac], value: acWeight },
      PR: { code: pr, label: METRIC_LABELS.PR[pr], value: prWeight },
      UI: { code: ui, label: METRIC_LABELS.UI[ui], value: uiWeight },
      S:  { code: s,  label: METRIC_LABELS.S[s], isChanged: isScopeChanged },
      C:  { code: c,  label: METRIC_LABELS.C[c], value: cWeight },
      I:  { code: i,  label: METRIC_LABELS.I[i], value: iWeight },
      A:  { code: a,  label: METRIC_LABELS.A[a], value: aWeight }
    }
  };
}

function parseCvssVector(vectorString) {
  if (!vectorString || typeof vectorString !== 'string') {
    throw new Error('Please provide a valid CVSS v3.1 vector string');
  }

  const parts = vectorString.trim().split('/');
  const metrics = {};

  for (const part of parts) {
    if (part.startsWith('CVSS:')) continue;
    const [key, val] = part.split(':');
    if (key && val) {
      metrics[key.toUpperCase()] = val.toUpperCase();
    }
  }

  return calculateCvss31(metrics);
}

module.exports = {
  calculateCvss31,
  parseCvssVector,
  getSeverity
};

