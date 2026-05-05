const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

function extractHost(input) {
  try {
    return new URL(input).hostname;
  } catch {
    return input;
  }
}

function isValidHost(host) {
  if (!host || typeof host !== "string") return false;
  if (host.length > 253) return false;
  if (net.isIP(host)) return true;
  // RFC 1035-ish hostname validation
  const hostnameRegex =
    /^(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)*(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)$/;
  return hostnameRegex.test(host);
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const input = req.query.url;
  if (!input) {
    return res.status(400).json({ error: "Missing 'url' query parameter." });
  }

  const host = extractHost(input);
  if (!isValidHost(host)) {
    return res.status(400).json({ error: "Invalid host." });
  }

  const isWin = process.platform === "win32";
  const args = isWin ? ["-n", "1", host] : ["-c", "1", host];

  execFile(
    "ping",
    args,
    { timeout: 5000, windowsHide: true, maxBuffer: 1024 * 64 },
    (error, stdout) => {
      const reachable = !error;
      res.status(200).json({
        host,
        reachable,
        output: stdout ? String(stdout) : "",
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
