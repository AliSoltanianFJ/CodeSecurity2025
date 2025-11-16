const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  next();
});

/**
 * Validate a hostname or IP address string
 */
function isValidHost(input) {
  if (!input || typeof input !== "string") return false;

  const trimmed = input.trim();

  // Disallow obvious bad chars quickly
  if (/[^\w\.\-\:\[\]]/i.test(trimmed)) return false; // only allow word chars, dot, dash, colon, [ ]

  // Strip IPv6 brackets if present
  const unbracketed =
    trimmed.startsWith("[") && trimmed.endsWith("]") ? trimmed.slice(1, -1) : trimmed;

  // IP (v4 or v6)
  if (net.isIP(unbracketed)) return true;

  // RFC 1035-ish hostname validation
  const hostnameRegex =
    /^(?=.{1,253}$)(?!-)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\.(?!-)[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$/;

  return hostnameRegex.test(unbracketed);
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const urlParam = req.query.url;

  if (!urlParam || typeof urlParam !== "string") {
    return res.status(400).json({ error: "Missing or invalid 'url' query parameter." });
  }

  let host = urlParam.trim();

  // If a full URL is provided, extract the hostname
  try {
    if (/^[a-z]+:\/\//i.test(host)) {
      const parsed = new URL(host);
      host = parsed.hostname;
    }
  } catch {
    // If URL parsing fails, treat the input as a raw host
  }

  // Strip IPv6 brackets if present
  if (host.startsWith("[") && host.endsWith("]")) {
    host = host.slice(1, -1);
  }

  if (!isValidHost(host)) {
    return res.status(400).json({ error: "Invalid host." });
  }

  const isWindows = process.platform === "win32";
  const args = isWindows ? ["-n", "1", host] : ["-c", "1", host];

  execFile(
    "ping",
    args,
    { timeout: 5000, windowsHide: true, maxBuffer: 1024 * 1024 },
    (error, stdout, stderr) => {
      // Normalize output
      const output = (stdout || stderr || "").toString();

      // Timeout or execution error still returns reachable: false
      if (error) {
        const timedOut = error.killed || error.signal || error.code === "ETIMEDOUT";
        const status = timedOut ? 504 : 200;
        return res.status(status).json({
          reachable: false,
          output,
        });
      }

      // Successful execution; consider it reachable
      return res.json({
        reachable: true,
        output,
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`Server running on port ${PORT}`);
});
