// ...existing code...
const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function extractHost(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  // Ensure we have a scheme so URL parsing works
  const withScheme = trimmed.includes("://") ? trimmed : `http://${trimmed}`;
  try {
    const u = new URL(withScheme);
    return u.hostname;
  } catch {
    return null;
  }
}

function isSafeHost(host) {
  if (!host || typeof host !== "string") return false;
  // Allow IPs
  if (net.isIP(host)) return true;

  // Basic hostname checks (ASCII only)
  if (!/^[a-zA-Z0-9.-]+$/.test(host)) return false;
  if (host.length > 253) return false;
  if (host.startsWith("-") || host.endsWith("-")) return false;
  if (host.includes("..")) return false;
  return true;
}

/**
 * ping a URL to see if it is alive using a safe execFile call (no shell).
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
    if (isIPv6) args.push("-6");
    args.push("-n", "1", host);
  } else {
    if (isIPv6) args.push("-6");
    // -c 1 => send 1 packet, -W 2 => wait up to 2 seconds for reply (Linux). On some BSD/macOS, -W semantics differ but this is a safe reasonable default.
    args.push("-c", "1", "-W", "2", host);
  }

  execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error) => {
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
