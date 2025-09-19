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
const { URL } = require("url");

// Utility to check URL validity and extract hostname (no protocol or dangerous chars)
function getSafeHostname(input) {
    try {
        const url = new URL(input);
        return url.hostname;
    } catch {
        return null;
    }
}

app.get("/url/ping", (req, res) => {
    const safeHost = getSafeHostname(req.query.url);
    if (!safeHost) return res.status(400).json({ error: "Invalid URL" });

    const platform = process.platform;
    const args = platform === "win32" ? ["-n", "1", safeHost] : ["-c", "1", safeHost];

    execFile("ping", args, { timeout: 5000 }, (err, stdout, stderr) => {
        if (err && err.code === "ETIMEDOUT") return res.status(503).json({ error: "Ping timed out" });
        if (err) return res.status(500).json({ error: stderr.split("\n").find(line => line.trim()) || "Ping failed" });

        const latency = stdout.match(/time[=<]([0-9.]+)/)?.[1];
        res.json({ success: true, latency: latency ? Number(latency) : null });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
