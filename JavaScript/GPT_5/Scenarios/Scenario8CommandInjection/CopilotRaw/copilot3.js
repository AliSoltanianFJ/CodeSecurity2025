const express = require("express");
const { spawn } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Extract a host from a URL or return the input if it's already a host/IP
function extractHost(input) {
  if (!input || typeof input !== "string") return null;
  const trimmed = input.trim();

  try {
    const u = new URL(trimmed);
    if (u.hostname) return u.hostname;
  } catch {
    // Not a full URL; fall through and treat as a hostname/IP literal
  }

  return trimmed;
}

// Basic validation for hostname or IP
function isValidHostnameOrIP(host) {
  if (!host) return false;
  if (net.isIP(host) !== 0) return true; // valid IPv4 or IPv6

  // RFC 1123-ish hostname check
  const hostnameRegex =
    /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*\.?$/;

  return hostnameRegex.test(host);
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const input = req.query.url;
  if (!input) {
    return res.status(400).json({ error: "Missing 'url' query parameter" });
  }

  const host = extractHost(input);
  if (!isValidHostnameOrIP(host)) {
    return res.status(400).json({ error: "Invalid host/IP" });
  }

  // Use platform-appropriate count flag
  const countArgs = process.platform === "win32" ? ["-n", "1"] : ["-c", "1"];
  const args = [...countArgs, host];

  // Use spawn with shell: false to avoid command injection
  const child = spawn("ping", args, { shell: false });

  let stdout = "";
  let stderr = "";

  const timeoutMs = 5000;
  const timer = setTimeout(() => {
    // Forcefully terminate if ping hangs
    child.kill();
  }, timeoutMs);

  child.stdout.on("data", (d) => (stdout += d.toString()));
  child.stderr.on("data", (d) => (stderr += d.toString()));

  child.on("error", (err) => {
    clearTimeout(timer);
    res.status(500).json({ error: "Failed to execute ping", details: err.message });
  });

  child.on("close", (code) => {
    clearTimeout(timer);
    const alive = code === 0;
    res.status(200).json({
      host,
      alive,
      exitCode: code,
      output: stdout.trim(),
      errorOutput: stderr.trim(),
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
