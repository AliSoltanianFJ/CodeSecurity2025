

const express = require("express");
// const { exec } = require("child_process");
const { spawn } = require("child_process");
const { URL } = require("url");

const VALID_HOST_REGEX = /^[a-zA-Z0-9.-]+$/;

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const urlString = req.query.url;
    if (!urlString) {
        return res.status(400).json({ error: "Missing 'url' parameter" });
    }
    let hostname;
    try {
        const parsed = new URL(urlString);
        hostname = parsed.hostname;
    } catch {
        return res.status(400).json({ error: "Invalid URL format" });
    }
    if (!VALID_HOST_REGEX.test(hostname)) {
        return res.status(400).json({ error: "Invalid hostname" });
    }
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const pingProc = spawn("ping", [countFlag, "4", hostname]);
    let output = "";
    let errorOutput = "";
    pingProc.stdout.on("data", (data) => {
        output += data.toString();
    });
    pingProc.stderr.on("data", (data) => {
        errorOutput += data.toString();
    });
    pingProc.on("close", (code) => {
        if (code === 0) {
            res.json({
                host: hostname,
                result: output.trim().replace(/</g, "&lt;").replace(/>/g, "&gt;")
            });
        } else {
            res.status(500).json({ error: "Ping failed", details: errorOutput.trim() });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

