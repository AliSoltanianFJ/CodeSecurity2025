const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * Validate a hostname or IP address string.
 */
function isValidHost(input) {
  if (!input || typeof input !== "string") return false;
  if (input.length > 253) return false;
  // Quick character check (letters, digits, hyphen, dot, colon for IPv6)
  if (!/^[A-Za-z0-9.\-:]+$/.test(input)) return false;

  // IP checks
  if (net.isIP(input)) return true;

  // RFC 1035-ish hostname regex (no trailing dot, labels 1-63, whole <= 253)
  const label = "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)";
  const hostRegex = new RegExp(`^(?:${label}\\.)*${label}$`);
  return hostRegex.test(input);
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const raw = req.query.url;

  if (!raw || typeof raw !== "string" || raw.length > 2048) {
    return res.status(400).json({ error: "Missing or invalid 'url' query parameter." });
  }

  // Try to extract hostname from a full URL, otherwise treat as host
  let host = raw.trim();
  try {
    // If it's a URL like http(s)://example.com/path, take only the hostname
    const asUrl = new URL(raw);
    host = asUrl.hostname;
  } catch {
    // Not a full URL; keep as provided (e.g., example.com or 1.2.3.4)
  }

  if (!isValidHost(host)) {
    return res.status(400).json({ error: "Invalid host." });
  }

  const isWin = process.platform === "win32";
  const args = isWin ? ["-n", "1", host] : ["-c", "1", host];

  execFile("ping", args, { timeout: 5000, windowsHide: true, maxBuffer: 1024 * 1024 }, (error, stdout, stderr) => {
    // On many systems, ping returns exit code 0 if at least one reply was received.
    const alive = !error; // error indicates non-zero exit (no response, unreachable, etc.)
    res.status(200).json({
      host,
      alive,
      output: stdout ? stdout.toString() : "",
      error: stderr ? stderr.toString() : undefined,
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
