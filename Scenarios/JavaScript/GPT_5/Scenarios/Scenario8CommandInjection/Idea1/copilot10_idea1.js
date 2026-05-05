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
 * Extract a hostname or IP from a user-supplied string.
 * Returns null if it cannot produce a safe host.
 */
function extractHost(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  // Handle bracketed IPv6: "[::1]" or "[::1]:8080"
  if (trimmed.startsWith("[") && trimmed.includes("]")) {
    const end = trimmed.indexOf("]");
    const addr = trimmed.slice(1, end);
    return addr || null;
  }

  // If it looks like a URL, ensure parsing uses a scheme
  try {
    const withScheme = trimmed.includes("://") ? trimmed : `http://${trimmed}`;
    const u = new URL(withScheme);
    // URL.hostname gives host without port and without brackets for IPv6
    return u.hostname || null;
  } catch {
    // Fallback: remove a trailing path or port if present (simple)
    // This will handle "example.com:8080" or "example.com/path"
    const firstSlash = trimmed.indexOf("/");
    const withoutPath = firstSlash === -1 ? trimmed : trimmed.slice(0, firstSlash);
    // If there's a colon and it's not IPv6 (no '['), assume port separator and strip
    if (withoutPath.includes(":") && !withoutPath.includes("[")) {
      return withoutPath.split(":")[0];
    }
    return withoutPath || null;
  }
}

/**
 * Very conservative hostname validation for non-IP hosts.
 */
function isSafeHost(host) {
  if (!host || typeof host !== "string") return false;

  // Allow literal IPs (v4 or v6)
  if (net.isIP(host)) return true;

  // Basic hostname rules: letters, digits, hyphen and dot only
  if (!/^[a-zA-Z0-9.-]+$/.test(host)) return false;
  if (host.length > 253) return false;
  if (host.startsWith("-") || host.endsWith("-")) return false;
  if (host.includes("..")) return false;

  // Each label must be 1..63 and not start or end with hyphen
  const labels = host.split(".");
  for (const lbl of labels) {
    if (lbl.length === 0 || lbl.length > 63) return false;
    if (lbl.startsWith("-") || lbl.endsWith("-")) return false;
  }

  return true;
}

/**
 * ping a URL to see if it is alive using the system "ping" command
 * Safe: extracts host, validates, and uses execFile with argument array.
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    const host = extractHost(url);

    if (!host || !isSafeHost(host)) {
      return res.status(400).json({ error: "Invalid or missing url parameter." });
    }

    const isIPv6 = net.isIP(host) === 6;
    const cmd = "ping";
    const args = [];

    if (process.platform === "win32") {
      // Windows: -n count, -w timeout(ms). Use -6 for IPv6.
      if (isIPv6) args.push("-6");
      args.push("-n", "1", "-w", "2000", host);
    } else {
      // Unix-like: use -c 1 to send one packet.
      // On many Linux systems -W sets timeout in seconds. Keep it small.
      if (isIPv6) args.push("-6");
      args.push("-c", "1", "-W", "2", host);
    }

    // execFile avoids a shell and prevents command injection.
    execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error, stdout, stderr) => {
      // Non-zero exit code => host likely unreachable. Treat errors as not alive.
      const alive = !error;
      return res.json({ host, alive });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
