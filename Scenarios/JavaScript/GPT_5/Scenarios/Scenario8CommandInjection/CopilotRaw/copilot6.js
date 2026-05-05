const express = require("express");
// ...existing code...
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Safely parse and validate a host from a provided url/host string
 */
function parseHost(input) {
  if (!input || typeof input !== "string") return null;

  let value = input.trim();

  // If a full URL is provided, extract the hostname
  try {
    const u = new URL(value);
    value = u.hostname;
  } catch {
    // Not a URL; treat as hostname/IP as-is
  }

  if (!value || value.length > 253) return null;

  // Allow valid IPs (v4 or v6)
  if (net.isIP(value)) return value;

  // Validate hostname per RFC-style rules: labels 1-63 chars, alnum/hyphen, no leading/trailing hyphen
  const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
  if (!hostnameRegex.test(value)) return null;

  return value;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const urlParam = req.query.url;
  const host = parseHost(urlParam);

  if (!host) {
    return res.status(400).json({ error: "Invalid 'url' parameter. Provide a hostname or IP, or a full URL." });
  }

  // Windows ping: -n 1 (count 1), -w 2000 (timeout 2000ms)
  const args = ["-n", "1", "-w", "2000", host];

  execFile("ping", args, { timeout: 5000, windowsHide: true }, (error, stdout, stderr) => {
    // If ping executable not found or other system error
    if (error && (error.code === "ENOENT" || error.killed)) {
      return res.status(500).json({ error: "Ping failed to execute.", details: error.message });
    }

    const output = (stdout || "").toString();

    // Determine success by Received count in Windows ping output
    let alive = false;
    const receivedMatch = /Received\s*=\s*(\d+)/i.exec(output);
    if (receivedMatch && parseInt(receivedMatch[1], 10) >= 1) {
      alive = true;
    }

    // Extract a latency estimate if available (time=XXms or Average = XXms)
    let rttMs = null;
    const timeMatch = /time[=<]?\s*(\d+)ms/i.exec(output) || /Average\s*=\s*(?:<)?(\d+)ms/i.exec(output);
    if (timeMatch) {
      rttMs = parseInt(timeMatch[1], 10);
    }

    return res.status(200).json({
      host,
      alive,
      rttMs,
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
