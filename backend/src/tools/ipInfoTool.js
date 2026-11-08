/**
 * REDOPS CYBERSECURITY TOOLKIT — IP INFORMATION ENGINE
 * Validates IPv4 / IPv6 addresses and queries authoritative RIR RDAP registries.
 * Fully compliant with RFC 7480 - RFC 7484.
 */

const dns = require('node:dns').promises;

/**
 * Validates IPv4 syntax (RFC 791)
 */
function isValidIpv4(ip) {
  if (!ip || typeof ip !== 'string') return false;
  const parts = ip.trim().split('.');
  if (parts.length !== 4) return false;
  return parts.every(part => {
    const num = parseInt(part, 10);
    return !isNaN(num) && num >= 0 && num <= 255 && String(num) === part;
  });
}

/**
 * Validates IPv6 syntax (RFC 4291)
 */
function isValidIpv6(ip) {
  if (!ip || typeof ip !== 'string') return false;
  const trimmed = ip.trim().replace(/^\[|\]$/g, '');
  const ipv6Pattern = /^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$/;
  return ipv6Pattern.test(trimmed);
}

/**
 * Determines address scope and whether it is globally routable
 */
function evaluateIpScope(ipStr) {
  const isV4 = isValidIpv4(ipStr);
  const isV6 = isValidIpv6(ipStr);

  if (!isV4 && !isV6) {
    throw new Error(`Invalid IP address syntax: "${ipStr}". Please provide a valid IPv4 or IPv6 address.`);
  }

  if (isV4) {
    const octets = ipStr.trim().split('.').map(o => parseInt(o, 10));
    const [o1, o2] = octets;

    // Loopback 127.0.0.0/8
    if (o1 === 127) {
      return { isRoutable: false, type: 'Loopback', rfc: 'RFC 1122 (Host Loopback)' };
    }
    // RFC 1918 Private
    if (o1 === 10 || (o1 === 172 && o2 >= 16 && o2 <= 31) || (o1 === 192 && o2 === 168)) {
      return { isRoutable: false, type: 'RFC 1918 Private Network', rfc: 'RFC 1918 (Non-Routable)' };
    }
    // Link-Local APIPA 169.254.0.0/16
    if (o1 === 169 && o2 === 254) {
      return { isRoutable: false, type: 'Link-Local / APIPA', rfc: 'RFC 3927' };
    }
    // Carrier-Grade NAT 100.64.0.0/10
    if (o1 === 100 && (o2 >= 64 && o2 <= 127)) {
      return { isRoutable: false, type: 'Shared CGNAT Space', rfc: 'RFC 6598' };
    }
    // Multicast 224.0.0.0/4
    if (o1 >= 224 && o1 <= 239) {
      return { isRoutable: false, type: 'Multicast', rfc: 'RFC 5771' };
    }
    // Broadcast 255.255.255.255
    if (octets.every(o => o === 255)) {
      return { isRoutable: false, type: 'Limited Broadcast', rfc: 'RFC 919' };
    }

    return { isRoutable: true, type: 'Public Internet', rfc: 'Global Unicast' };
  } else {
    const cleanV6 = ipStr.trim().toLowerCase().replace(/^\[|\]$/g, '');
    if (cleanV6 === '::1' || cleanV6 === '0:0:0:0:0:0:0:1') {
      return { isRoutable: false, type: 'Loopback', rfc: 'RFC 4291 (Node Loopback)' };
    }
    if (cleanV6.startsWith('fe8') || cleanV6.startsWith('fe9') || cleanV6.startsWith('fea') || cleanV6.startsWith('feb')) {
      return { isRoutable: false, type: 'Link-Local IPv6', rfc: 'RFC 4291' };
    }
    if (cleanV6.startsWith('fc') || cleanV6.startsWith('fd')) {
      return { isRoutable: false, type: 'Unique Local IPv6', rfc: 'RFC 4193' };
    }
    return { isRoutable: true, type: 'Public Internet (IPv6)', rfc: 'Global Unicast IPv6' };
  }
}

/**
 * Retrieves registration and routing information for a given IP address
 */
async function getIpInformation(rawIp) {
  if (!rawIp || typeof rawIp !== 'string') {
    throw new Error('IP address string is required.');
  }

  const cleanIp = rawIp.trim().replace(/^\[|\]$/g, '');
  const scope = evaluateIpScope(cleanIp);
  const isV4 = isValidIpv4(cleanIp);

  // If private or reserved, return honest local network assessment
  if (!scope.isRoutable) {
    return {
      ip: cleanIp,
      ip_version: isV4 ? 'IPv4' : 'IPv6',
      is_valid: true,
      is_routable: false,
      scope: scope.type,
      rfc_standard: scope.rfc,
      network_name: 'Private/Reserved Network Space',
      organization: 'Local Network / Host Authority',
      country: 'N/A (Local / Internal Scope)',
      rir_registry: 'IANA Reserved',
      notice: 'This address is non-routable on the public internet. Regional Internet Registries (RIRs) do not catalog public routing allocations for private address space.',
      data_provider: 'Local Address Scope Engine (RFC 1918 / RFC 4291)',
      limitations: 'Address is restricted to internal subnets or local interfaces and cannot be queried via public internet registries.'
    };
  }

  // For public IPs, query official RIR RDAP endpoint
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);

  const rdapUrl = `https://rdap.org/ip/${cleanIp}`;

  try {
    const res = await fetch(rdapUrl, {
      signal: controller.signal,
      headers: {
        'Accept': 'application/rdap+json, application/json',
        'User-Agent': 'RedOps-Mobile-Security-Platform/1.0 (Authorized Info Inquirer; +https://redops-platform.local)'
      }
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
      if (res.status === 404) {
        return {
          ip: cleanIp,
          ip_version: isV4 ? 'IPv4' : 'IPv6',
          is_valid: true,
          is_routable: true,
          scope: scope.type,
          rfc_standard: scope.rfc,
          network_name: 'Unallocated or Unregistered Block',
          organization: 'Unknown / Not Found in RIR Registry',
          data_provider: 'IANA / Regional Internet Registry RDAP Protocol',
          limitations: 'Record was not found in authoritative RIR registry databases.'
        };
      }
      throw new Error(`RDAP registry query returned HTTP ${res.status}: ${res.statusText}`);
    }

    const rdap = await res.json();

    // Extract network name, handle, CIDR, organization
    let orgName = 'Unknown Entity';
    let country = rdap.country || 'International / Multi-Regional';

    if (Array.isArray(rdap.entities)) {
      for (const ent of rdap.entities) {
        if (ent.vcardArray && Array.isArray(ent.vcardArray[1])) {
          const fnCard = ent.vcardArray[1].find(item => item[0] === 'fn');
          if (fnCard && fnCard[3]) {
            orgName = fnCard[3];
            break;
          }
        }
      }
    }

    let allocatedCidr = '';
    if (rdap.cidr0_cidrs && Array.isArray(rdap.cidr0_cidrs) && rdap.cidr0_cidrs.length > 0) {
      allocatedCidr = `${rdap.cidr0_cidrs[0].v4prefix || rdap.cidr0_cidrs[0].v6prefix}/${rdap.cidr0_cidrs[0].length}`;
    } else if (rdap.startAddress && rdap.endAddress) {
      allocatedCidr = `${rdap.startAddress} - ${rdap.endAddress}`;
    }

    // Determine RIR
    let rir = 'RIR';
    const linkStr = JSON.stringify(rdap.links || []);
    if (linkStr.includes('arin.net')) rir = 'ARIN (American Registry for Internet Numbers)';
    else if (linkStr.includes('ripe.net')) rir = 'RIPE NCC (Europe, Middle East, Central Asia)';
    else if (linkStr.includes('apnic.net')) rir = 'APNIC (Asia-Pacific Network Information Centre)';
    else if (linkStr.includes('lacnic.net')) rir = 'LACNIC (Latin America and Caribbean)';
    else if (linkStr.includes('afrinic.net')) rir = 'AFRINIC (African Network Information Centre)';

    return {
      ip: cleanIp,
      ip_version: isV4 ? 'IPv4' : 'IPv6',
      is_valid: true,
      is_routable: true,
      scope: scope.type,
      network_handle: rdap.handle || 'N/A',
      network_name: rdap.name || rdap.handle || 'Autonomous Network',
      cidr_range: allocatedCidr || 'Direct Allocation',
      type: rdap.type || 'ASSIGNED',
      organization: orgName,
      country,
      rir_registry: rir,
      registration_status: Array.isArray(rdap.status) ? rdap.status.join(', ') : (rdap.status || 'ACTIVE'),
      start_address: rdap.startAddress || cleanIp,
      end_address: rdap.endAddress || cleanIp,
      data_provider: 'Authoritative Regional Internet Registry (RIR) via ICANN RDAP Protocol (RFC 7480 - RFC 7484)',
      limitations: 'LIMITATIONS: IP registration data reflects administrative allocation to autonomous systems and organizations. It does not provide real-time physical GPS device tracking, and geolocation approximations are subject to dynamic routing, VPNs, and CDN anycast infrastructure.'
    };
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error(`RIR RDAP registry query timed out after 8 seconds for IP ${cleanIp}. The registry service may be rate-limiting or unreachable.`);
    }
    throw new Error(`IP information lookup failed: ${err.message}`);
  }
}

module.exports = {
  isValidIpv4,
  isValidIpv6,
  evaluateIpScope,
  getIpInformation
};

