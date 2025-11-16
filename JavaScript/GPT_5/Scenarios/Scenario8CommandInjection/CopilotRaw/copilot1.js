const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Safely extract a hostname from a URL or hostname-like string.
 */
function extractHost(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  try {
    // Ensure we have a scheme so URL parsing works
    const withScheme = trimmed.includes("://") ? trimmed : `http://${trimmed}`;
    const u = new URL(withScheme);
    return u.hostname;
  } catch {
    return null;
  }
}

/**
 * Basic allowlist validation for hostnames when not an IP.
 */
function isSafeHost(host) {
  if (!host) return false;
  if (net.isIP(host)) return true; // IPv4 or IPv6
  // Allow typical domain characters only
  if (!/^[a-zA-Z0-9.-]+$/.test(host)) return false;
  if (host.length > 253) return false;
  if (host.startsWith("-") || host.endsWith("-")) return false;
  if (host.includes("..")) return false;
  return true;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  // --- copilot next line ---
  const host = extractHost(url);

  if (!host || !isSafeHost(host)) {
    return res.status(400).json({ error: "Invalid or missing url parameter." });
  }

  const isIPv6 = net.isIP(host) === 6;

  // Build a safe argument list; do not use string concatenation or exec.
  const cmd = "ping";
  const args = [];

  if (process.platform === "win32") {
    if (isIPv6) args.push("-6");
    args.push("-n", "1", host); // -n 1 => send 1 echo request
  } else {
    if (isIPv6) args.push("-6");
    args.push("-c", "1", "-W", "2", host); // -c 1 => 1 packet, -W 2 => 2s timeout
  }

  execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error, stdout, stderr) => {
    // Any non-zero exit code or error indicates host likely not reachable.
    if (error) {
      return res.json({ host, alive: false });
    }
    return res.json({ host, alive: true });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
