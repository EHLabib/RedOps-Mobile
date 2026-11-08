/**
 * RedOps Mobile - JSON Formatter & Validator Tool
 * Formats, minifies, validates JSON, and provides precise syntax error diagnostics.
 */

function locateJsonError(rawJson, errMessage) {
  const lines = rawJson.split('\n');

  // 1. Try extracting position from standard V8 error: "at position 42"
  let errorPos = -1;
  const posMatch = errMessage.match(/position\s+(\d+)/i);
  if (posMatch) {
    errorPos = parseInt(posMatch[1], 10);
  }

  let lineNum = 1;
  let colNum = 1;

  if (errorPos >= 0) {
    let currentPos = 0;
    for (let i = 0; i < lines.length; i++) {
      const lineLen = lines[i].length + 1; // +1 for \n
      if (currentPos + lineLen > errorPos) {
        lineNum = i + 1;
        colNum = errorPos - currentPos + 1;
        break;
      }
      currentPos += lineLen;
    }
  } else {
    // 2. Try matching "line X column Y"
    const lineColMatch = errMessage.match(/line\s+(\d+)\s+column\s+(\d+)/i);
    if (lineColMatch) {
      lineNum = parseInt(lineColMatch[1], 10);
      colNum = parseInt(lineColMatch[2], 10);
    } else {
      // 3. Fallback: Parse unexpected token character from "Unexpected token 'X'" or similar
      const tokenMatch = errMessage.match(/Unexpected token\s+['"]?([^'",\s]+)['"]?/i);
      if (tokenMatch && tokenMatch[1]) {
        const token = tokenMatch[1];
        // Search for this token in the lines
        for (let i = 0; i < lines.length; i++) {
          const idx = lines[i].indexOf(token);
          if (idx !== -1) {
            lineNum = i + 1;
            colNum = idx + 1;
            break;
          }
        }
      }
    }
  }

  // Create context snippet with error marker
  const errorLine = lines[lineNum - 1] || '';
  const pointerLine = ' '.repeat(Math.max(0, colNum - 1)) + '^';

  return {
    line: lineNum,
    column: colNum,
    position: errorPos >= 0 ? errorPos : undefined,
    snippet: `${errorLine}\n${pointerLine}`
  };
}

function formatJson(rawJson, indent = 2) {
  if (rawJson === undefined || rawJson === null || rawJson.trim() === '') {
    throw new Error('Please provide JSON text to format.');
  }

  const trimmed = rawJson.trim();

  try {
    const parsed = JSON.parse(trimmed);
    const spaces = typeof indent === 'number' ? indent : 2;
    const formatted = JSON.stringify(parsed, null, spaces);
    const minified = JSON.stringify(parsed);

    // Compute metrics
    const originalBytes = Buffer.byteLength(trimmed, 'utf8');
    const formattedBytes = Buffer.byteLength(formatted, 'utf8');
    const minifiedBytes = Buffer.byteLength(minified, 'utf8');

    return {
      is_valid: true,
      formatted,
      minified,
      parsed_type: Array.isArray(parsed) ? 'Array' : typeof parsed === 'object' ? 'Object' : typeof parsed,
      item_count: Array.isArray(parsed) ? parsed.length : typeof parsed === 'object' && parsed !== null ? Object.keys(parsed).length : 1,
      stats: {
        original_bytes: originalBytes,
        formatted_bytes: formattedBytes,
        minified_bytes: minifiedBytes,
        compression_ratio: `${Math.round((1 - (minifiedBytes / formattedBytes)) * 100)}%`
      }
    };
  } catch (err) {
    const errorLocation = locateJsonError(rawJson, err.message);
    return {
      is_valid: false,
      error_message: err.message,
      line: errorLocation.line,
      column: errorLocation.column,
      snippet: errorLocation.snippet,
      raw_input: rawJson
    };
  }
}

module.exports = {
  formatJson,
  locateJsonError
};
