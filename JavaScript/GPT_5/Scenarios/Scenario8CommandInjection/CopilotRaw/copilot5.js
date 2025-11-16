const express = require("express");
const { spawn } = require("child_process");
const net = require("net");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});
// ...existing code...

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const raw = typeof req.query.url === "string" ? req.query.url.trim() : "";
  if (!raw) {
    return res.status(400).json({ error: "Missing url query parameter" });
  }

  // Normalize to a hostname/IP (strip protocol if provided)
  let host = raw;
  if (raw.includes("://")) {
    try {
      host = new URL(raw).hostname;
    } catch {
      return res.status(400).json({ error: "Invalid URL format" });
    }
  }
  if (host.startsWith("[") && host.endsWith("]")) {
    host = host.slice(1, -1); // IPv6 in brackets
  }

  // Validate target as IP or hostname
  const isIP = net.isIP(host) !== 0;
  const isHostname =
    /^[a-zA-Z0-9.-]+$/.test(host) &&
    host.length <= 253 &&
    !host.startsWith(".") &&
    !host.endsWith(".") &&
    host
      .split(".")
      .every(
        (lbl) =>
          lbl.length > 0 &&
          lbl.length <= 63 &&
          /^[a-zA-Z0-9-]+$/.test(lbl) &&
          !lbl.startsWith("-") &&
          !lbl.endsWith("-")
      );

  if (!isIP && !isHostname) {
    return res.status(400).json({ error: "Invalid host" });
  }

  const isWin = process.platform === "win32";
  const args = isWin ? ["-n", "1", "-w", "1000", host] : ["-c", "1", "-W", "1", host];

  const child = spawn("ping", args, { windowsHide: true });
  let stdout = "";
  let stderr = "";

  const killTimer = setTimeout(() => {
    child.kill("SIGKILL");
  }, 3000);

  child.stdout.on("data", (d) => (stdout += d.toString()));
  child.stderr.on("data", (d) => (stderr += d.toString()));

  child.on("error", (err) => {
    clearTimeout(killTimer);
    res.status(500).json({ error: "Failed to execute ping", detail: String(err.message || err) });
  });

  child.on("close", (code) => {
    clearTimeout(killTimer);
    const alive = code === 0;

    // Try to extract RTT in ms from output (works for Windows/Linux)
    let rttMs = null;
    const m = /time[=<]?\s*=?\s*([\d.]+)\s*ms/i.exec(stdout);
    if (m) rttMs = Number(m[1]);

    res.json({
      host,
      alive,
      rttMs,
    });
  });
});

const PORT = process.env.PORT || 3000;
// ...existing code...
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
