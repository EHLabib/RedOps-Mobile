/**
 * RedOps Mobile - Subnet & CIDR Calculator Tool
 * Pure JavaScript implementation with RFC compliance (RFC 1918, RFC 3021, RFC 5737).
 */

function ipToInt(ip) {
  return ip.split('.').reduce((acc, octet) => ((acc << 8) + parseInt(octet, 10)) >>> 0, 0);
}

function intToIp(intVal) {
  return [
    (intVal >>> 24) & 255,
    (intVal >>> 16) & 255,
    (intVal >>> 8) & 255,
    intVal & 255
  ].join('.');
}

function intToBinary(intVal) {
  const binary = (intVal >>> 0).toString(2).padStart(32, '0');
  return `${binary.slice(0, 8)}.${binary.slice(8, 16)}.${binary.slice(16, 24)}.${binary.slice(24, 32)}`;
}

function determineScope(ipInt) {
  const unsigned = ipInt >>> 0;
  // RFC 1918 Private
  const is10 = ((unsigned & 0xFF000000) >>> 0) === 0x0A000000; // 10.0.0.0/8
  const is172 = ((unsigned & 0xFFF00000) >>> 0) === 0xAC100000; // 172.16.0.0/12
  const is192 = ((unsigned & 0xFFFF0000) >>> 0) === 0xC0A80000; // 192.168.0.0/16
  if (is10 || is172 || is192) {
    return { type: 'Private', rfc: 'RFC 1918 (Private-Use)', routable: false };
  }

  // Loopback (127.0.0.0/8)
  if (((unsigned & 0xFF000000) >>> 0) === 0x7F000000) {
    return { type: 'Loopback', rfc: 'RFC 1122 (Host Loopback)', routable: false };
  }

  // Link-Local (169.254.0.0/16)
  if (((unsigned & 0xFFFF0000) >>> 0) === 0xA9FE0000) {
    return { type: 'Link-Local / APIPA', rfc: 'RFC 3927', routable: false };
  }

  // Carrier-Grade NAT (100.64.0.0/10)
  if (((unsigned & 0xFFC00000) >>> 0) === 0x64400000) {
    return { type: 'Shared Address (CGNAT)', rfc: 'RFC 6598', routable: false };
  }

  // Documentation (TEST-NET-1, 2, 3)
  if (((unsigned & 0xFFFFFF00) >>> 0) === 0xC0000200 || 
      ((unsigned & 0xFFFFFF00) >>> 0) === 0xC6336400 || 
      ((unsigned & 0xFFFFFF00) >>> 0) === 0xCB007100) {
    return { type: 'Documentation', rfc: 'RFC 5737', routable: false };
  }

  // Multicast (224.0.0.0/4)
  if (((unsigned & 0xF0000000) >>> 0) === 0xE0000000) {
    return { type: 'Multicast', rfc: 'RFC 5771', routable: false };
  }

  // Broadcast (255.255.255.255)
  if (unsigned === 0xFFFFFFFF) {
    return { type: 'Limited Broadcast', rfc: 'RFC 919', routable: false };
  }

  return { type: 'Public Internet', rfc: 'Global Unicast', routable: true };
}

function determineClass(firstOctet) {
  if (firstOctet >= 1 && firstOctet <= 126) return 'Class A (Large Enterprise / Government)';
  if (firstOctet === 127) return 'Class A (Reserved Loopback)';
  if (firstOctet >= 128 && firstOctet <= 191) return 'Class B (Medium-to-Large Organization)';
  if (firstOctet >= 192 && firstOctet <= 223) return 'Class C (Small-to-Medium Organization)';
  if (firstOctet >= 224 && firstOctet <= 239) return 'Class D (Multicast)';
  if (firstOctet >= 240 && firstOctet <= 255) return 'Class E (Experimental / Reserved)';
  return 'Unknown';
}

function calculateSubnet(rawInput) {
  if (!rawInput || typeof rawInput !== 'string') {
    throw new Error('Please provide an IPv4 address with CIDR notation (e.g. 192.168.1.0/24)');
  }

  const cleaned = rawInput.trim();
  let ipStr = '';
  let prefix = 32;

  if (cleaned.includes('/')) {
    const parts = cleaned.split('/');
    ipStr = parts[0].trim();
    prefix = parseInt(parts[1].trim(), 10);
    if (isNaN(prefix) || prefix < 0 || prefix > 32) {
      throw new Error(`Invalid prefix /${parts[1]}. Must be between /0 and /32.`);
    }
  } else if (cleaned.includes(' ') || cleaned.includes('\t')) {
    // E.g. "192.168.1.0 255.255.255.0"
    const parts = cleaned.split(/\s+/);
    ipStr = parts[0];
    const maskStr = parts[1];
    const maskInt = ipToInt(maskStr);
    const maskBinary = (maskInt >>> 0).toString(2);
    if (maskBinary.includes('01')) {
      throw new Error(`Invalid subnet mask: ${maskStr}`);
    }
    prefix = maskBinary.split('1').length - 1;
  } else {
    // Assume /32 host if single IP given
    ipStr = cleaned;
    prefix = 32;
  }

  // Validate IP format
  const octets = ipStr.split('.');
  if (octets.length !== 4) {
    throw new Error(`Invalid IPv4 address: "${ipStr}". Must contain 4 decimal octets.`);
  }

  for (let i = 0; i < 4; i++) {
    const num = parseInt(octets[i], 10);
    if (isNaN(num) || num < 0 || num > 255 || String(num) !== octets[i]) {
      throw new Error(`Invalid IPv4 octet "${octets[i]}" in address "${ipStr}".`);
    }
  }

  const ipInt = ipToInt(ipStr);
  const netmaskInt = prefix === 0 ? 0 : (((0xFFFFFFFF << (32 - prefix)) >>> 0));
  const wildcardInt = (~netmaskInt) >>> 0;
  const networkInt = (ipInt & netmaskInt) >>> 0;
  const broadcastInt = (networkInt | wildcardInt) >>> 0;

  let totalHosts = Math.pow(2, 32 - prefix);
  let usableHosts = 0;
  let firstUsableInt = 0;
  let lastUsableInt = 0;

  if (prefix === 32) {
    usableHosts = 1;
    firstUsableInt = networkInt;
    lastUsableInt = networkInt;
  } else if (prefix === 31) {
    // RFC 3021 Point-to-Point links
    usableHosts = 2;
    firstUsableInt = networkInt;
    lastUsableInt = broadcastInt;
  } else {
    usableHosts = totalHosts - 2;
    firstUsableInt = (networkInt + 1) >>> 0;
    lastUsableInt = (broadcastInt - 1) >>> 0;
  }

  const scopeInfo = determineScope(ipInt);
  const networkScope = determineScope(networkInt);
  const ipClass = determineClass(parseInt(octets[0], 10));

  return {
    input: rawInput,
    ip: ipStr,
    prefix: `/${prefix}`,
    prefixNumber: prefix,
    netmask: intToIp(netmaskInt),
    wildcard: intToIp(wildcardInt),
    network: intToIp(networkInt),
    broadcast: intToIp(broadcastInt),
    firstUsable: intToIp(firstUsableInt),
    lastUsable: intToIp(lastUsableInt),
    usableHostRange: `${intToIp(firstUsableInt)} - ${intToIp(lastUsableInt)}`,
    totalHosts: totalHosts.toLocaleString(),
    usableHosts: usableHosts.toLocaleString(),
    totalHostsRaw: totalHosts,
    usableHostsRaw: usableHosts,
    ipClass,
    scope: scopeInfo.type,
    rfc: scopeInfo.rfc,
    isRoutable: scopeInfo.routable,
    binary: {
      ip: intToBinary(ipInt),
      netmask: intToBinary(netmaskInt),
      network: intToBinary(networkInt),
      broadcast: intToBinary(broadcastInt)
    },
    hex: {
      ip: `0x${(ipInt >>> 0).toString(16).toUpperCase().padStart(8, '0')}`,
      netmask: `0x${(netmaskInt >>> 0).toString(16).toUpperCase().padStart(8, '0')}`,
      network: `0x${(networkInt >>> 0).toString(16).toUpperCase().padStart(8, '0')}`
    },
    cidrNotation: `${intToIp(networkInt)}/${prefix}`,
    ptrRecord: `${octets.reverse().join('.')}.in-addr.arpa`
  };
}

module.exports = {
  calculateSubnet,
  ipToInt,
  intToIp
};
