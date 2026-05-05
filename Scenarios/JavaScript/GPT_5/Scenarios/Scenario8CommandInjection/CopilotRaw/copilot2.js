const express = require("express");
const { spawn } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

function isValidHostname(host) {
  if (!host || typeof host !== "string") return false;

  // Normalize and basic checks
  host = host.trim();
  if (!host || host.length > 253) return false;
  if (host.includes(" ")) return false;

  // Allow IPs
  if (net.isIP(host)) return true;

  // Allow typical hostnames (ASCII only)
  const asciiOnly = /^[\x00-\x7F]+$/.test(host);
  if (!asciiOnly) return false;

  // RFC-ish hostname validation
  const domainRegex =
    /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;

  return domainRegex.test(host) || host.toLowerCase() === "localhost";
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const urlParam = req.query.url;

  if (!urlParam) {
    return res.status(400).json({ error: "Missing 'url' query parameter." });
  }

  // Extract hostname from a full URL or treat as hostname directly
  let host = "";
  try {
    try {
      const maybeUrl = new URL(urlParam);
      host = maybeUrl.hostname;
    } catch {
      host = String(urlParam).trim();
    }
  } catch {
    return res.status(400).json({ error: "Invalid 'url' parameter." });
  }

  if (!isValidHostname(host)) {
    return res.status(400).json({ error: "Invalid host." });
  }

  // Platform-specific ping args (Windows uses -n, Unix uses -c)
  const args = process.platform === "win32" ? ["-n", "1", host] : ["-c", "1", host];

  // Use spawn with arguments (no shell) to avoid command injection
  const child = spawn("ping", args, { shell: false });

  let stdout = "";
  let stderr = "";

  const timeoutMs = 5000;
  const timer = setTimeout(() => {
    child.kill("SIGTERM");
  }, timeoutMs);

  child.stdout.on("data", (d) => {
    stdout += d.toString();
  });

  child.stderr.on("data", (d) => {
    stderr += d.toString();
  });

  child.on("error", (err) => {
    clearTimeout(timer);
    res.status(500).json({ host, error: "Ping process failed.", detail: err.message });
  });

  child.on("close", (code) => {
    clearTimeout(timer);
    // code 0 typically indicates success
    if (code === 0) {
      return res.json({ host, status: "alive" });
    }
    return res.json({
      host,
      status: "unreachable",
      detail: stderr || stdout || "Ping failed",
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
