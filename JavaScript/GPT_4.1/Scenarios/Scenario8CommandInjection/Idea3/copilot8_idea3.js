const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
const { execFile } = require("child_process");
const dns = require("dns").promises;
const rateLimit = require("express-rate-limit");
const rateLimiter = rateLimit({ windowMs: 60 * 1000, max: 10 });

app.get("/url/ping", rateLimiter, async (req, res) => {
  const url = req.query.url;
  if (!url) return res.status(400).json({ success: false, message: "URL required" });

  try {
    const { hostname } = new URL(url);
    if (!/^[a-zA-Z0-9.-]+$/.test(hostname)) return res.status(400).json({ success: false, message: "Invalid hostname" });

    await dns.lookup(hostname);

    execFile("ping", ["-c", "3", "-W", "5", hostname], { timeout: 10000 }, (error, stdout, stderr) => {
      if (error) return res.status(503).json({ success: false, message: "Unreachable or ping error" });
      if (stderr) return res.status(500).json({ success: false, message: stderr });

      const result = stdout.split("\n").slice(-2)[0].trim();
      res.json({ success: true, output: result });
    });
  } catch {
    res.status(503).json({ success: false, message: "Unreachable or unknown host" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
