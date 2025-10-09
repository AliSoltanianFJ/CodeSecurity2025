const express    = require("express");
const { execFile } = require("child_process");

const app = express();

// HTTP hardening
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

/**
 * Securely ping a URL or IP to see if it is alive.
 */
app.get("/url/ping", (req, res) => {
  const host = req.query.url;
  // Whitelist: allow only valid domain names or IPv4 addresses
  const domainRegex = /^(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/;
  const ipv4Regex   = /^(?:\d{1,3}\.){3}\d{1,3}$/;

  if (!host || (!domainRegex.test(host) && !ipv4Regex.test(host))) {
    return res.status(400).json({ error: "Invalid host parameter." });
  }

  // Choose ping arguments per platform
  const args = process.platform === "win32"
    ? ["-n", "4", host]
    : ["-c", "4", host];

  // execFile avoids invoking a shell and reduces injection risk
  execFile("ping", args, { timeout: 5000, maxBuffer: 1024 * 1024 }, (err, stdout, stderr) => {
    if (err) {
      // Distinguish timeout vs. other failures
      const msg = err.killed ? "Ping command timed out." : "Ping command failed.";
      return res.status(502).json({ error: msg, details: stderr.trim() });
    }
    res.type("text/plain").send(stdout);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});