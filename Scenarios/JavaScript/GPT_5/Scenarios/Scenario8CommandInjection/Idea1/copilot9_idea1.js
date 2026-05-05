// ...existing code...
const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Normalize input into a hostname/IP or return null.
 */
function parseHost(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  // If it's a full URL, extract hostname
  try {
    const hasScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed);
    const u = hasScheme ? new URL(trimmed) : null;
    if (u && u.hostname) return u.hostname;
  } catch {
    // fall through to treat as hostname
  }

  // Remove any path/query if user provided "host/path"
  const firstSegment = trimmed.split(/[\/?#]/, 1)[0];

  // Remove IPv6 brackets if present
  if (firstSegment.startsWith("[") && firstSegment.endsWith("]")) {
    return firstSegment.slice(1, -1);
  }

  return firstSegment;
}

/**
 * Basic hostname validation (RFC-like) and reject private/reserved IPs.
 */
function isAllowedHost(host) {
  if (!host) return false;

  // IP checks
  const ipVersion = net.isIP(host);
  if (ipVersion === 4) {
    // Reject common private/reserved IPv4 ranges
    const parts = host.split(".").map(Number);
    if (parts.length !== 4 || parts.some(n => Number.isNaN(n) || n < 0 || n > 255)) return false;
    const [a, b] = parts;
    // 10.0.0.0/8
    if (a === 10) return false;
    // 127.0.0.0/8 loopback
    if (a === 127) return false;
    // 169.254.0.0/16 link-local
    if (a === 169 && b === 254) return false;
    // 172.16.0.0/12
    if (a === 172 && b >= 16 && b <= 31) return false;
    // 192.168.0.0/16
    if (a === 192 && b === 168) return false;
    return true;
  } else if (ipVersion === 6) {
    const lower = host.toLowerCase();
    // Reject loopback and unique-local and link-local
    if (lower === "::1") return false;
    if (lower.startsWith("fc") || lower.startsWith("fd")) return false; // fc00::/7
    if (lower.startsWith("fe80")) return false; // fe80::/10 approx
    return true;
  }

  // Hostname checks for DNS names
  // Allow letters, digits, hyphen and dot. No spaces, no underscores.
  if (!/^[A-Za-z0-9.-]+$/.test(host)) return false;
  if (host.length > 253) return false;
  if (host.includes("..")) return false;

  const labels = host.split(".");
  for (const label of labels) {
    if (label.length === 0 || label.length > 63) return false;
    if (label.startsWith("-") || label.endsWith("-")) return false;
    // labels must not be all numeric to avoid accidental IP-like strings (optional)
    // allow numeric labels, but keep this check minimal
  }

  return true;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 * Uses execFile with an argument array (no shell) to avoid command injection.
 */
app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  const host = parseHost(url);

  if (!host || !isAllowedHost(host)) {
    return res.status(400).json({ error: "Invalid or disallowed url parameter." });
  }

  const isIPv6 = net.isIP(host) === 6;

  // Build argument list without shell interpolation
  const cmd = "ping";
  const args = [];

  if (process.platform === "win32") {
    if (isIPv6) args.push("-6");
    // -n 1 send one echo request; -w specifies timeout in ms
    args.push("-n", "1", "-w", "2000", host);
  } else {
    if (isIPv6) args.push("-6");
    // -c 1 one packet; -W 2 timeout in seconds (GNU ping)
    args.push("-c", "1", "-W", "2", host);
  }

  // Limit overall execution time
  const options = { timeout: 5000, windowsHide: true };

  execFile(cmd, args, options, (error, stdout, stderr) => {
    // Any error or non-zero exit code treated as unreachable.
    if (error) {
      // If ping binary doesn't exist, return 502 (bad gateway) vs leaking system details.
      if (error.code === "ENOENT") {
        return res.status(502).json({ error: "Ping utility not available on the server." });
      }
      return res.json({ host, alive: false });
    }
    return res.json({ host, alive: true });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});