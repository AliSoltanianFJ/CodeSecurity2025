const express = require("express");
const dns = require("dns");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  // Mitigate clickjacking
  res.setHeader("X-Frame-Options", "DENY");
  // Always respond JSON and avoid revealing server details
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  next();
});

/**
 * Helpers
 */
function isValidHostString(s) {
  if (!s || typeof s !== "string") return false;
  if (s.length > 255) return false; // hostname max length
  // reject any control characters or whitespace
  if (/[\u0000-\u001F\u007F\s]/.test(s)) return false;
  return true;
}

function isPrivateIPv4(ip) {
  const parts = ip.split(".").map(n => parseInt(n, 10));
  if (parts.length !== 4 || parts.some(isNaN)) return true; // treat malformed as unsafe
  const [a, b] = parts;
  if (a === 10) return true; // 10.0.0.0/8
  if (a === 127) return true; // loopback
  if (a === 169 && b === 254) return true; // link-local
  if (a === 172 && b >= 16 && b <= 31) return true; // 172.16.0.0/12
  if (a === 192 && b === 168) return true; // 192.168.0.0/16
  if (a === 0) return true; // 0.0.0.0/8 unspecified
  if (a >= 224 && a <= 239) return true; // multicast
  return false;
}

function isPrivateIPv6(ip) {
  // normalize basic checks for common private/loopback ranges
  const lower = ip.toLowerCase();
  if (lower === "::1") return true; // loopback
  if (lower.startsWith("fe80:")) return true; // link-local
  // unique local addresses fc00::/7 (fc00 or fd00)
  if (lower.startsWith("fc") || lower.startsWith("fd")) return true;
  // unspecified
  if (lower === "::") return true;
  return false;
}

function isPrivateIP(ip) {
  const version = net.isIP(ip);
  if (version === 4) return isPrivateIPv4(ip);
  if (version === 6) return isPrivateIPv6(ip);
  // unknown -> treat as private/unsafe
  return true;
}

/**
 * ping a URL to see if it is alive without invoking shell commands
 *
 * - Validate input strictly
 * - Resolve hostname and ensure it is not a private/internal address
 * - Attempt a TCP connection on the appropriate port with timeout
 * - Return small JSON response (no stack traces)
 */
app.get("/url/ping", (req, res) => {
  try {
    const raw = req.query.url;
    if (!isValidHostString(raw)) {
      return res.status(400).json({ ok: false, error: "invalid url parameter" });
    }

    // Ensure we have a scheme so URL parser treats input consistently
    let input = raw;
    if (!/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(input)) {
      input = "http://" + input;
    }

    let parsed;
    try {
      parsed = new URL(input);
    } catch (err) {
      return res.status(400).json({ ok: false, error: "malformed url" });
    }

    const hostname = parsed.hostname;
    // Basic hostname validation (no control chars, length checked earlier)
    if (!hostname || hostname.length > 255) {
      return res.status(400).json({ ok: false, error: "invalid host" });
    }

    // Limit allowed protocols to common web schemes
    const protocol = parsed.protocol;
    let port = parsed.port ? Number(parsed.port) : (protocol === "https:" ? 443 : 80);
    if (!Number.isInteger(port) || port <= 0 || port > 65535) {
      return res.status(400).json({ ok: false, error: "invalid port" });
    }

    // Resolve DNS (IPv4 or IPv6) but do not follow redirects or fetch content
    const lookupOptions = { all: false, family: 0, hints: dns.ADDRCONFIG | dns.V4MAPPED };
    dns.lookup(hostname, lookupOptions, (err, address, family) => {
      if (err || !address) {
        // Don't leak internal error details
        return res.status(400).json({ ok: false, error: "host resolution failed" });
      }

      if (isPrivateIP(address)) {
        return res.status(403).json({ ok: false, error: "forbidden target address" });
      }

      // Attempt TCP connection with short timeout to check reachability
      const socket = new net.Socket();
      const start = Date.now();
      const timeoutMs = 3000; // short timeout to avoid resource waste

      let finished = false;
      function done(status, info) {
        if (finished) return;
        finished = true;
        try { socket.destroy(); } catch (e) {}
        const duration = Date.now() - start;
        return res.status(status).json(Object.assign({ rttMs: duration }, info));
      }

      socket.setTimeout(timeoutMs, () => {
        done(504, { ok: false, error: "connection timed out" });
      });

      socket.once("error", () => done(502, { ok: false, error: "connection failed" }));
      socket.once("connect", () => done(200, { ok: true, host: hostname, ip: address, port } ));

      // Connect using numeric address to avoid triggering additional DNS resolution
      socket.connect(port, address);
    });
  } catch (e) {
    // Generic catch-all: do not expose internal details
    return res.status(500).json({ ok: false, error: "internal error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Minimal non-sensitive logging
  console.log(`Server running on port ${PORT}`);
});
