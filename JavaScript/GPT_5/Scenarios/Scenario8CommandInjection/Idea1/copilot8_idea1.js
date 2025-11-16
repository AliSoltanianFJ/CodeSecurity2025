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
 * Safely extract hostname from a user-provided URL or host string.
 */
function extractHost(input) {
  if (!input || typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed || /\s/.test(trimmed)) return null;

  // Ensure a scheme exists so URL parsing works for bare hostnames
  const withScheme = trimmed.includes("://") ? trimmed : `http://${trimmed}`;
  try {
    const u = new URL(withScheme);
    return u.hostname; // returns hostname without port
  } catch {
    return null;
  }
}

function isPrivateIPv4(ip) {
  const parts = ip.split(".").map(Number);
  if (parts.length !== 4 || parts.some(n => Number.isNaN(n))) return false;
  const [a, b] = parts;
  if (a === 10) return true;
  if (a === 127) return true; // loopback
  if (a === 169 && b === 254) return true; // link-local
  if (a === 172 && b >= 16 && b <= 31) return true; // 172.16.0.0/12
  if (a === 192 && b === 168) return true; // 192.168.0.0/16
  return false;
}

function isPrivateIPv6(ip) {
  // basic checks for common non-routable ranges
  const lower = ip.toLowerCase();
  if (lower === "::1" || lower === "::") return true;
  // Unique local addresses fc00::/7 (fc or fd)
  if (lower.startsWith("fc") || lower.startsWith("fd")) return true;
  // Link-local fe80::/10
  if (lower.startsWith("fe80")) return true;
  return false;
}

function isSafeHostname(host) {
  if (!host || typeof host !== "string") return false;
  if (host.length > 253) return false;

  // Allow IPs but ensure they're not private/reserved
  const ipVersion = net.isIP(host);
  if (ipVersion === 4) return !isPrivateIPv4(host);
  if (ipVersion === 6) return !isPrivateIPv6(host);

  // Hostname: only allow letters, digits, hyphen, dot; no leading/trailing hyphen segments or consecutive dots
  if (!/^[A-Za-z0-9.-]+$/.test(host)) return false;
  if (host.includes("..")) return false;
  const labels = host.split(".");
  if (labels.some(l => !l || l.length > 63)) return false;
  if (labels.some(l => l.startsWith("-") || l.endsWith("-"))) return false;

  return true;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    const host = extractHost(url);

    if (!host || !isSafeHostname(host)) {
      return res.status(400).json({ error: "Invalid or missing url parameter." });
    }

    const isIPv6 = net.isIP(host) === 6;
    const cmd = "ping";
    const args = [];

    if (process.platform === "win32") {
      if (isIPv6) args.push("-6");
      args.push("-n", "1", host); // send 1 echo request
    } else {
      if (isIPv6) args.push("-6");
      args.push("-c", "1", "-W", "2", host); // 1 packet, 2s timeout
    }

    // Use execFile with an argv array to avoid shell interpolation/injection
    execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error, stdout, stderr) => {
      // Any non-zero exit code or error indicates host likely not reachable.
      const alive = !error;
      return res.json({ host, alive });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
