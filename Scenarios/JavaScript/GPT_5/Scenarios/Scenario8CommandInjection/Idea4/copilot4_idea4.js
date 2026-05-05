const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    next();
});

app.get("/url/ping", (req, res) => {
    const raw = req.query.url;
    if (!raw || typeof raw !== "string" || raw.length > 2000) {
        return res.status(400).json({ error: "invalid request" });
    }

    let parsed;
    try {
        parsed = new URL(raw);
    } catch (e) {
        return res.status(400).json({ error: "invalid url" });
    }

    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
        return res.status(400).json({ error: "unsupported url scheme" });
    }

    const host = parsed.hostname;
    if (!host || host.length > 255) {
        return res.status(400).json({ error: "invalid host" });
    }

    const isIp = net.isIP(host);
    const hostnameAllowed = isIp !== 0 || /^[a-zA-Z0-9-\.]+$/.test(host);
    if (!hostnameAllowed) {
        return res.status(400).json({ error: "invalid host" });
    }

    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const args = [countFlag, "1", host];

    execFile("ping", args, { timeout: 3000, windowsHide: true }, (err, stdout, stderr) => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(500).json({ error: "service unavailable" });
            }
            return res.status(502).json({ alive: false });
        }
        return res.json({ alive: true });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
