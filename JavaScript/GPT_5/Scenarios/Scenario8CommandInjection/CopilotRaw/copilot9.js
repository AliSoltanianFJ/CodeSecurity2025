const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const url = req.query.url;

  if (!url || typeof url !== "string") {
    return res.status(400).json({ error: "Missing url query parameter" });
  }

  // Parse and extract the hostname
  let host = url.trim();
  try {
    const parsed = new URL(host.includes("://") ? host : `http://${host}`);
    host = parsed.hostname || host;
  } catch {
    return res.status(400).json({ error: "Invalid URL" });
  }

  // Normalize IPv6 literals like [2001:db8::1] -> 2001:db8::1
  if (host.startsWith("[") && host.endsWith("]")) {
    host = host.slice(1, -1);
  }

  // Basic allowlist validation for hostnames/IPs
  const isIPv4 = /^(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)$/.test(host);
  const isIPv6 = /^[A-Fa-f0-9:]+$/.test(host) && host.includes(":");
  const isDomain =
    /^(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/.test(host);
  const isLocalhost = /^localhost$/i.test(host);

  if (!(isIPv4 || isIPv6 || isDomain || isLocalhost)) {
    return res.status(400).json({ error: "Invalid host" });
  }

  // Build platform-specific ping arguments
  const isWin = process.platform === "win32";
  const cmd = "ping";
  const args = isWin ? ["-n", "1", host] : ["-c", "1", host];

  execFile(
    cmd,
    args,
    { timeout: 5000, windowsHide: true, maxBuffer: 64 * 1024 },
    (error, stdout, stderr) => {
      if (error) {
        const timedOut = error.killed || error.code === "ETIMEDOUT";
        return res.status(200).json({
          host,
          alive: false,
          reason: timedOut ? "timeout" : "unreachable",
          output: (stdout || stderr || "").toString()
        });
      }
      return res.json({
        host,
        alive: true,
        output: (stdout || "").toString()
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
