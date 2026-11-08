/**
 * REDOPS CYBERSECURITY TOOLKIT — PASSIVE RECONNAISSANCE ENGINE
 * DNS & WHOIS/RDAP Registry Resolver
 */

const dns = require('node:dns').promises;

/**
 * Validates domain name syntax (RFC 1035 / RFC 1123)
 */
function isValidDomain(domain) {
  if (!domain || typeof domain !== 'string') return false;
  const clean = domain.trim().toLowerCase();
  if (clean.length > 253) return false;
  const domainPattern = /^(?!-)[a-zA-Z0-9-]{1,63}(?<!-)(\.[a-zA-Z0-9-]{1,63})*\.[a-zA-Z]{2,}$/;
  return domainPattern.test(clean) || clean === 'localhost';
}

/**
 * Validates IPv4 or IPv6 address syntax
 */
function isValidIp(ip) {
  if (!ip || typeof ip !== 'string') return false;
  const ipv4Pattern = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
  const ipv6Pattern = /^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$/;
  return ipv4Pattern.test(ip.trim()) || ipv6Pattern.test(ip.trim());
}

/**
 * Resolves DNS records for a given domain
 * @param {string} rawDomain 
 * @param {string} recordType - 'A', 'AAAA', 'MX', 'TXT', 'NS', 'CNAME', 'SOA', or 'ALL'
 */
async function resolveDns(rawDomain, recordType = 'ALL') {
  const domain = (rawDomain || '').trim().toLowerCase().replace(/^https?:\/\//, '').split('/')[0].split(':')[0];

  if (!isValidDomain(domain)) {
    throw new Error(`Invalid domain name format: "${rawDomain}". Please provide a valid FQDN (e.g. example.com).`);
  }

  const requestedType = (recordType || 'ALL').toUpperCase();
  const results = {
    domain,
    queried_at: new Date().toISOString(),
    records: []
  };

  const tasks = [];

  const addQuery = (type, fn) => {
    if (requestedType === 'ALL' || requestedType === type) {
      tasks.push(
        fn().then(data => ({ type, data })).catch(err => ({
          type,
          error: err.code === 'ENODATA' ? 'No records of this type' :
                 err.code === 'ENOTFOUND' ? 'Domain not found' :
                 err.message
        }))
      );
    }
  };

  addQuery('A', async () => {
    try {
      const res = await dns.resolve4(domain, { ttl: true });
      return res.map(r => ({ address: r.address, ttl: r.ttl }));
    } catch {
      const ips = await dns.resolve4(domain);
      return ips.map(ip => ({ address: ip }));
    }
  });

  addQuery('AAAA', async () => {
    try {
      const res = await dns.resolve6(domain, { ttl: true });
      return res.map(r => ({ address: r.address, ttl: r.ttl }));
    } catch {
      const ips = await dns.resolve6(domain);
      return ips.map(ip => ({ address: ip }));
    }
  });

  addQuery('MX', async () => {
    const records = await dns.resolveMx(domain);
    return records.sort((a, b) => a.priority - b.priority);
  });

  addQuery('TXT', async () => {
    const entries = await dns.resolveTxt(domain);
    return entries.map(chunk => chunk.join(' '));
  });

  addQuery('NS', async () => {
    return await dns.resolveNs(domain);
  });

  addQuery('CNAME', async () => {
    return await dns.resolveCname(domain);
  });

  addQuery('SOA', async () => {
    return await dns.resolveSoa(domain);
  });

  const settled = await Promise.all(tasks);

  settled.forEach(item => {
    if (!item.error && item.data) {
      results.records.push({
        type: item.type,
        data: item.data,
        status: 'SUCCESS'
      });
    } else {
      results.records.push({
        type: item.type,
        status: item.error === 'No records of this type' ? 'EMPTY' : 'ERROR',
        message: item.error
      });
    }
  });

  return results;
}

/**
 * Reverse DNS Lookup (PTR)
 */
async function reverseDns(rawIp) {
  const ip = (rawIp || '').trim();
  if (!isValidIp(ip)) {
    throw new Error(`Invalid IP address format: "${rawIp}". Must be a valid IPv4 or IPv6 address.`);
  }

  try {
    const hostnames = await dns.reverse(ip);
    return {
      ip,
      hostnames: hostnames || [],
      count: hostnames.length,
      queried_at: new Date().toISOString()
    };
  } catch (err) {
    if (err.code === 'ENOTFOUND' || err.code === 'ENODATA') {
      return {
        ip,
        hostnames: [],
        count: 0,
        message: 'No PTR (reverse DNS) records registered for this IP address.',
        queried_at: new Date().toISOString()
      };
    }
    throw new Error(`Reverse DNS lookup failed: ${err.message}`);
  }
}

/**
 * Modern WHOIS via Registration Data Access Protocol (RDAP)
 */
async function lookupRdap(rawQuery) {
  const query = (rawQuery || '').trim().toLowerCase().replace(/^https?:\/\//, '').split('/')[0].split(':')[0];
  if (!query) throw new Error('Domain or IP address is required for registration lookup.');

  const isIp = isValidIp(query);
  const endpoint = isIp ? `https://rdap.org/ip/${encodeURIComponent(query)}` : `https://rdap.org/domain/${encodeURIComponent(query)}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 7000);

  try {
    const res = await fetch(endpoint, {
      signal: controller.signal,
      headers: {
        'Accept': 'application/rdap+json, application/json',
        'User-Agent': 'RedOps-Mobile-Cybersecurity-Toolkit/1.0'
      }
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
      if (res.status === 404) {
        return {
          query,
          type: isIp ? 'IP' : 'DOMAIN',
          status: 'NOT_FOUND',
          message: `No public RDAP registration record found for "${query}".`
        };
      }
      throw new Error(`RDAP registry returned HTTP ${res.status} (${res.statusText})`);
    }

    const rdap = await res.json();

    // Extract standardized WHOIS metadata
    const events = {};
    if (Array.isArray(rdap.events)) {
      rdap.events.forEach(e => {
        if (e.eventAction && e.eventDate) {
          events[e.eventAction] = e.eventDate;
        }
      });
    }

    // Extract nameservers
    const nameservers = [];
    if (Array.isArray(rdap.nameservers)) {
      rdap.nameservers.forEach(ns => {
        if (ns.ldhName) nameservers.push(ns.ldhName.toLowerCase());
      });
    }

    // Extract registrar / entities
    let registrarName = 'Unknown / Registry Redacted';
    if (Array.isArray(rdap.entities)) {
      for (const ent of rdap.entities) {
        if (Array.isArray(ent.roles) && ent.roles.includes('registrar')) {
          if (ent.vcardArray && Array.isArray(ent.vcardArray[1])) {
            const fn = ent.vcardArray[1].find(item => item[0] === 'fn');
            if (fn && fn[3]) registrarName = fn[3];
          }
          break;
        }
      }
    }

    return {
      query,
      type: isIp ? 'IP' : 'DOMAIN',
      status: 'FOUND',
      handle: rdap.handle || query,
      registration_date: events.registration || events['last update of RDAP database'] || null,
      expiration_date: events.expiration || null,
      last_changed: events['last changed'] || null,
      registrar: registrarName,
      status_flags: Array.isArray(rdap.status) ? rdap.status : [],
      nameservers: nameservers.length > 0 ? nameservers : undefined,
      network_name: rdap.name || undefined,
      ip_version: rdap.ipVersion || undefined,
      rdap_conformance: rdap.rdapConformance || []
    };
  } catch (err) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new Error('RDAP registry lookup timed out after 7 seconds. The registry may be rate-limiting or unreachable.');
    }
    throw new Error(`RDAP registry lookup failed: ${err.message}`);
  }
}

module.exports = {
  isValidDomain,
  isValidIp,
  resolveDns,
  reverseDns,
  lookupRdap
};

