const express = require("express");
const dns = require("dns");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader("X-Frame-Options", "DENY");
  // Prevent MIME sniffing
  res.setHeader("X-Content-Type-Options", "nosniff");
  // Restrictive CSP for API responses
  res.setHeader("Content-Security-Policy", "default-src 'none'");
  next();
});

/**
 * Safely check whether a host (from a URL or hostname) is reachable by attempting
 * a short TCP connection (no shell commands, no child processes).
 *
 * Input validation & sanitisation:
 *  - url parameter required, max length enforced
 *  - accepts full URL or plain hostname/IP
 *  - hostname validated (IPv4/IPv6 via net.isIP or domain name pattern)
 *  - DNS resolution is attempted to ensure the host exists
 *  - No command execution, no eval, no deserialization of user-controlled data
 *  - Responses are JSON only and avoid leaking stack traces or internal details
 */
app.get("/url/ping", async (req, res) => {
  try {
    const raw = (req.query.url || "").toString().trim();

    if (!raw) {
      return res.status(400).json({ error: "Missing 'url' query parameter." });
    }

    // Limit length to avoid abuse / memory issues
    if (raw.length > 2083) {
      return res.status(400).json({ error: "Input too long." });
    }

    // Accept a full URL or a bare hostname. If no scheme, prefix with http:// for parsing.
    let candidate = raw;
    if (!/^[a-zA-Z][a-zA-Z0-9.+-]*:\/\//.test(candidate)) {
      candidate = "http://" + candidate;
    }

    let hostname;
    try {
      // Using WHATWG URL parser to safely extract the hostname
      const u = new URL(candidate);
      hostname = u.hostname;
    } catch (err) {
      return res.status(400).json({ error: "Invalid URL/hostname." });
    }

    // Validate hostname: allow IPs (v4/v6) or sensible domain names including localhost
    const ipVersion = net.isIP(hostname); // 0 = not IP, 4 or 6 otherwise
    const domainNamePattern = /^(?:localhost|[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z]{2,63})*)$/;
    if (ipVersion === 0 && !domainNamePattern.test(hostname)) {
      return res.status(400).json({ error: "Hostname contains invalid characters." });
    }

    // Resolve DNS to ensure hostname exists and to get a concrete address (prevents raw user-controlled DNS usage later)
    const lookup = () =>
      new Promise((resolve, reject) => {
        // Use dns.lookup (uses OS resolver) to avoid uncontrolled remote lookups
        dns.lookup(hostname, { family: 0 }, (err, address, family) => {
          if (err) return reject(err);
          resolve({ address, family });
        });
      });

    let resolved;
    try {
      resolved = await lookup();
    } catch (err) {
      // Don't leak internal details; return safe message
      return res.status(404).json({ alive: false, reason: "Host could not be resolved." });
    }

    // Try connecting to common service ports (443 then 80) with a short timeout.
    const tryConnect = (addr, port, timeoutMs = 5000) =>
      new Promise((resolve) => {
        const socket = new net.Socket();
        let settled = false;
        socket.setTimeout(timeoutMs);

        socket.once("connect", () => {
          settled = true;
          socket.destroy();
          resolve({ ok: true, port });
        });

        socket.once("timeout", () => {
          if (!settled) {
            settled = true;
            socket.destroy();
            resolve({ ok: false, reason: "timeout" });
          }
        });

        socket.once("error", () => {
          if (!settled) {
            settled = true;
            socket.destroy();
            resolve({ ok: false, reason: "connection refused" });
          }
        });

        // Use numeric address from DNS to avoid any further name resolution
        socket.connect(port, addr);
      });

    const addr = resolved.address;
    // Try 443 first (HTTPS), then 80 (HTTP)
    const ports = [443, 80];
    let result = { ok: false, reason: "unreachable" };
    for (const port of ports) {
      // short-circuit if already successful
      const r = await tryConnect(addr, port, 4000);
      if (r.ok) {
        result = r;
        break;
      } else {
        // continue trying next port
      }
    }

    // Return a safe JSON response; do not include stack traces or internal errors.
    res.setHeader("Content-Type", "application/json; charset=utf-8");
    if (result.ok) {
      return res.json({
        alive: true,
        host: hostname,
        address: addr,
        family: resolved.family,
        port: result.port,
      });
    } else {
      return res.status(200).json({
        alive: false,
        host: hostname,
        address: addr,
        family: resolved.family,
        reason: result.reason,
      });
    }
  } catch (err) {
    // Catch-all: do not expose internal error details to clients
    return res.status(500).json({ error: "Internal server error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
