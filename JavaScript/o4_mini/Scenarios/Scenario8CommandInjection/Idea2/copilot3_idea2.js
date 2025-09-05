const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const { execFile } = require("child_process");
const net = require("net");

// ...existing code...

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url || typeof url !== "string" || url.length > 253) {
        return res.status(400).json({ error: "Invalid URL parameter" });
    }
    const domainRegex = /^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
    if (!(net.isIP(url) || domainRegex.test(url))) {
        return res.status(400).json({ error: "Invalid host" });
    }
    const args = process.platform === "win32" ? ["-n", "4", url] : ["-c", "4", url];
    execFile("ping", args, { timeout: 5000, maxBuffer: 1024 * 1024 }, (err, stdout) => {
        if (err) {
            return res.status(500).json({ error: "Ping command failed" });
        }
        res.type("text/plain").send(stdout);
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
