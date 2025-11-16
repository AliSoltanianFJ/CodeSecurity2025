const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

function getPingTarget(input) {
  if (typeof input !== "string") return null;
  let s = input.trim();
  if (!s) return null;

  let host = null;
  try {
    if (s.includes("://")) {
      host = new URL(s).hostname || null;
    } else {
      host = s;
    }
  } catch {
    return null;
  }

  // Strip IPv6 brackets if present
  if (host.startsWith("[") && host.endsWith("]")) {
    host = host.slice(1, -1);
  }

  // Basic safety checks to prevent flags or whitespace
  if (!host || host.length > 253) return null;
  if (host.startsWith("-")) return null; // prevent flag injection on Windows ping
  if (/[^\S\r\n]/.test(host) || host.includes("/")) return null; // no spaces or slashes

  // Allow IPs directly
  const ipVer = net.isIP(host);
  if (ipVer === 4 || ipVer === 6) return host;

  // Validate domain labels (ASCII)
  const hostnameRe = /^(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\.(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))*$/;
  if (!hostnameRe.test(host)) return null;

  return host.toLowerCase();
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const raw = req.query.url;
  const host = getPingTarget(raw);

  if (!host) {
    return res.status(400).json({ error: "Invalid or missing url parameter." });
  }

  const isIPv6 = net.isIP(host) === 6;

  const cmd = "ping";
  const args = [];

  if (process.platform === "win32") {
    if (isIPv6) args.push("-6");
    args.push("-n", "1", "-w", "2000", host); // 1 echo, 2s timeout
  } else {
    if (isIPv6) args.push("-6");
    args.push("-n", "-c", "1", "-W", "2", "--", host); // numeric, 1 packet, 2s wait, end of options
  }

  execFile(cmd, args, { timeout: 5000, windowsHide: true }, (error) => {
    // Non-zero exit or error -> not reachable
    const alive = !error;
    res.json({ target: host, alive });
  });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
