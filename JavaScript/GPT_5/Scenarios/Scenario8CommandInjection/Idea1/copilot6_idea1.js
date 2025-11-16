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
 * Extract a hostname or IP from a user-provided string.
 * Accepts raw hostnames/IPs or full URLs. Returns null for invalid input.
 */
function extractHost(input) {
  if (!input || typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  // If input contains scheme, try parse directly, otherwise prepend a scheme to allow URL parsing.
  const withScheme = trimmed.includes("://") ? trimmed : `http://${trimmed}`;
  try {
    const u = new URL(withScheme);
    // URL.hostname already strips port if present and returns punycode for IDN
    return u.hostname;
  } catch {
    return null;
  }
}

/**
 * Basic safety checks for hostnames (not a full RFC validator).
 * Allows IP addresses (v4/v6) via net.isIP.
 * For hostnames: only ASCII letters, digits, hyphen and dots; no empty labels, length limits.
 */
function isSafeHost(host) {
  if (!host || typeof host !== "string") return false;

  // Accept IP addresses directly
  if (net.isIP(host)) return true;

  // Reject if contains characters outside allowed set
  if (!/^[a-zA-Z0-9.-]+$/.test(host)) return false;
  if (host.length > 253) return false;
  if (host.startsWith(".") || host.endsWith(".")) return false;
  if (host.includes("..")) return false;

  // Each label must be 1-63 characters and not start or end with hyphen
  const labels = host.split(".");
  for (const label of labels) {
    if (!label || label.length > 63) return false;
    if (label.startsWith("-") || label.endsWith("-")) return false;
  }

  return true;
}

/**
 * ping a URL to see if it is alive using the system "ping" command.
 * Uses execFile with a safe argument list to avoid shell injection.
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

  // Build platform-specific, safe argument list (do not use a shell or string concatenation)
  if (process.platform === "win32") {
    if (isIPv6) args.push("-6");
    args.push("-n", "1", host); // send 1 echo request
  } else {
    if (isIPv6) args.push("-6");
    args.push("-c", "1", "-W", "2", host); // -c 1 => 1 packet, -W 2 => 2s timeout (most Linux)
  }

  // Execute without a shell to avoid injection. Set a timeout to avoid long-running commands.
  execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error, stdout, stderr) => {
    // Any non-zero exit code or error usually indicates unreachable host or other failure.
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
// ...existing code...
