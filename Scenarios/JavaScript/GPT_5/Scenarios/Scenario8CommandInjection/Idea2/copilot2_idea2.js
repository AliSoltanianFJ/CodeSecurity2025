const express = require("express");
const net = require("net");
const dns = require("dns").promises;
const { URL } = require("url");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

/**
 * Safely check reachability of a URL by performing a TCP connect to the resolved
 * public IP and appropriate port (80 for http, 443 for https). This avoids
 * shelling out to system `ping` and prevents command injection.
 *
 * Security measures:
 * - Strict input validation (required, length limit, URL parse)
 * - Allow only http/https schemes
 * - DNS resolution with checking for private/internal addresses (SSRF protection)
 * - Connect using net.connect (no shell) with a timeout
 * - Return JSON only (no HTML) and avoid reflecting raw user input
 */

function isPrivateIPv4(addr) {
  const parts = addr.split('.').map(n => parseInt(n, 10));
  if (parts.length !== 4 || parts.some(n => Number.isNaN(n))) return false;
  const [a, b] = parts;
  if (a === 10) return true;                     // 10.0.0.0/8
  if (a === 127) return true;                    // 127.0.0.0/8 loopback
  if (a === 169 && b === 254) return true;       // 169.254.0.0/16 link local
  if (a === 172 && b >= 16 && b <= 31) return true; // 172.16.0.0/12
  if (a === 192 && b === 168) return true;      // 192.168.0.0/16
  if (a === 100 && b >= 64 && b <= 127) return true; // 100.64.0.0/10 carrier-grade NAT
  if (a === 0) return true;                      // 0.0.0.0
  return false;
}

function isPrivateIPv6(addr) {
  const a = addr.toLowerCase();
  if (a === '::1') return true;                  // loopback
  if (a.startsWith('fe80:')) return true;        // link-local
  // Unique local addresses fc00::/7 (fc or fd)
  if (a.startsWith('fc') || a.startsWith('fd')) return true;
  // ::ffff: IPv4-mapped addresses -> check embedded IPv4
  if (a.includes('::ffff:')) {
    const v4 = a.split(':').pop();
    return isPrivateIPv4(v4);
  }
  return false;
}

app.get("/url/ping", async (req, res) => {
  try {
    const raw = String(req.query.url || '').trim();

    // Validate presence and length
    if (!raw) {
      return res.status(400).json({ error: "Missing 'url' query parameter" });
    }
    if (raw.length > 2083) {
      return res.status(400).json({ error: "URL too long" });
    }

    // Parse and allow only http/https
    let parsed;
    try {
      parsed = new URL(raw);
    } catch (err) {
      return res.status(400).json({ error: "Invalid URL" });
    }
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return res.status(400).json({ error: "Only http and https URLs are allowed" });
    }

    const hostname = parsed.hostname;
    // Resolve all addresses for the hostname
    let addrs;
    try {
      addrs = await dns.lookup(hostname, { all: true });
    } catch (err) {
      return res.status(502).json({ error: "DNS resolution failed" });
    }
    if (!Array.isArray(addrs) || addrs.length === 0) {
      return res.status(502).json({ error: "DNS resolution returned no addresses" });
    }

    // Ensure at least one public (non-private) address is present
    const publicAddr = addrs.find(a => {
      if (a.family === 4) return !isPrivateIPv4(a.address);
      if (a.family === 6) return !isPrivateIPv6(a.address);
      return false;
    });

    if (!publicAddr) {
      // Prevent SSRF by rejecting targets that only resolve to internal addresses
      return res.status(403).json({ error: "Target resolves only to private/internal addresses" });
    }

    const port = parsed.port ? Number(parsed.port) : (parsed.protocol === 'https:' ? 443 : 80);
    if (!(Number.isInteger(port) && port > 0 && port <= 65535)) {
      return res.status(400).json({ error: "Invalid port" });
    }

    // Attempt TCP connect to the resolved public address (no shell, no exec)
    const timeoutMs = 3000;
    await new Promise((resolve, reject) => {
      const socket = net.connect({ host: publicAddr.address, port, family: publicAddr.family }, () => {
        socket.end();
        resolve();
      });

      socket.setTimeout(timeoutMs, () => {
        socket.destroy();
        reject(new Error("timeout"));
      });

      socket.on('error', (err) => {
        socket.destroy();
        reject(err);
      });
    });

    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.json({ alive: true });
  } catch (err) {
    // Generic error response without leaking internal details
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.status(502).json({ alive: false, error: "unreachable" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
