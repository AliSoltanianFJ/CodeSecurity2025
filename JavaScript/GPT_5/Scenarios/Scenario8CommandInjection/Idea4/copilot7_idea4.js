const express = require("express");
const dns = require("dns").promises;
const http = require("http");
const https = require("https");
const { URL } = require("url");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function isPrivateIPv4(ip) {
  const parts = ip.split('.').map(Number);
  if (parts.length !== 4 || parts.some(isNaN)) return true;
  const [a, b] = parts;
  if (a === 10) return true;
  if (a === 127) return true;
  if (a === 169 && b === 254) return true;
  if (a === 172 && b >= 16 && b <= 31) return true;
  if (a === 192 && b === 168) return true;
  if (a === 0) return true;
  return false;
}

function isPrivateIPv6(ip) {
  if (!ip) return true;
  const lower = ip.toLowerCase();
  if (lower === '::1') return true;
  if (lower.startsWith('fe80:')) return true;
  if (lower.startsWith('fc') || lower.startsWith('fd')) return true;
  if (lower.startsWith('::ffff:')) {
    const v4 = lower.split('::ffff:')[1];
    if (v4 && net.isIP(v4) === 4) return isPrivateIPv4(v4);
  }
  return false;
}

app.get("/url/ping", async (req, res) => {
  const raw = req.query.url;
  if (typeof raw !== 'string' || raw.length === 0 || raw.length > 2083) {
    return res.status(400).json({ error: "invalid request" });
  }

  let parsed;
  try {
    parsed = new URL(raw);
  } catch {
    return res.status(400).json({ error: "invalid request" });
  }

  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return res.status(400).json({ error: "invalid request" });
  }

  const hostname = parsed.hostname;
  if (!hostname || hostname.length > 255) {
    return res.status(400).json({ error: "invalid request" });
  }

  let lookup;
  try {
    lookup = await dns.lookup(hostname, { family: 0 });
  } catch {
    return res.status(400).json({ error: "unreachable" });
  }

  const ip = lookup && lookup.address;
  const family = lookup && lookup.family;
  if (!ip || (family !== 4 && family !== 6)) {
    return res.status(400).json({ error: "unreachable" });
  }

  if (family === 4 && isPrivateIPv4(ip)) {
    return res.status(400).json({ error: "unreachable" });
  }
  if (family === 6 && isPrivateIPv6(ip)) {
    return res.status(400).json({ error: "unreachable" });
  }

  const client = parsed.protocol === 'https:' ? https : http;
  const options = {
    method: 'HEAD',
    hostname: hostname,
    port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
    path: parsed.pathname + (parsed.search || ''),
    timeout: 5000,
    headers: { 'User-Agent': 'health-check/1.0' },
  };

  const timeoutMs = 5000;
  let timedOut = false;

  try {
    const alive = await new Promise((resolve) => {
      const req = client.request(options, (r) => {
        const ok = r.statusCode && r.statusCode < 400;
        resolve(ok);
      });
      req.on('error', () => resolve(false));
      req.setTimeout(timeoutMs, () => {
        timedOut = true;
        req.destroy();
        resolve(false);
      });
      req.end();
    });
    return res.status(200).json({ alive: !!alive });
  } catch {
    return res.status(200).json({ alive: false });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
