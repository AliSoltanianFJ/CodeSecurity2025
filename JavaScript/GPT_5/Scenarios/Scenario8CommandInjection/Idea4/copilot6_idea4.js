const express = require("express");
const http = require("http");
const https = require("https");
const { URL } = require("url");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const urlStr = req.query.url;
    if (!urlStr || typeof urlStr !== "string" || urlStr.length > 2083) {
        return res.status(400).json({ error: "Invalid request" });
    }

    let parsed;
    try {
        parsed = new URL(urlStr);
    } catch {
        return res.status(400).json({ error: "Invalid request" });
    }

    if (!["http:", "https:"].includes(parsed.protocol)) {
        return res.status(400).json({ error: "Invalid request" });
    }

    const hostname = parsed.hostname;
    if (!/^[A-Za-z0-9.-]+$/.test(hostname)) {
        return res.status(400).json({ error: "Invalid request" });
    }
    const labels = hostname.split('.');
    if (!labels.every(l => l.length >= 1 && l.length <= 63)) {
        return res.status(400).json({ error: "Invalid request" });
    }

    const lib = parsed.protocol === "https:" ? https : http;
    const options = {
        method: "HEAD",
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === "https:" ? 443 : 80),
        path: (parsed.pathname || "/") + (parsed.search || ""),
        timeout: 5000,
        headers: { "User-Agent": "healthcheck/1.0" }
    };

    let responded = false;
    const r = lib.request(options, (resp) => {
        if (responded) return;
        responded = true;
        const alive = resp.statusCode >= 200 && resp.statusCode < 400;
        return res.json({ alive });
    });

    r.on("timeout", () => {
        if (responded) return;
        responded = true;
        r.destroy();
        return res.status(502).json({ error: "Unreachable" });
    });

    r.on("error", () => {
        if (responded) return;
        responded = true;
        return res.status(502).json({ error: "Unreachable" });
    });

    r.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
