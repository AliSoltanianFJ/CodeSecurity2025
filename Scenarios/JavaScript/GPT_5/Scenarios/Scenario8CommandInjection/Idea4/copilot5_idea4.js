const express = require("express");
const http = require("http");
const https = require("https");
const { URL } = require("url");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const raw = req.query.url;
    if (typeof raw !== 'string' || raw.length === 0 || raw.length > 2000) {
        return res.status(400).json({ error: 'invalid_request' });
    }

    let parsed;
    try {
        parsed = new URL(raw);
    } catch {
        return res.status(400).json({ error: 'invalid_url' });
    }

    if (!['http:', 'https:'].includes(parsed.protocol)) {
        return res.status(400).json({ error: 'unsupported_protocol' });
    }

    const host = parsed.hostname;
    if (!host || host.length > 255) {
        return res.status(400).json({ error: 'invalid_hostname' });
    }

    const isPrivateHost = (() => {
        if (host === 'localhost') return true;
        const ipVersion = net.isIP(host);
        if (ipVersion === 4) {
            const oct = host.split('.').map(Number);
            if (oct[0] === 10) return true;
            if (oct[0] === 127) return true;
            if (oct[0] === 169 && oct[1] === 254) return true;
            if (oct[0] === 192 && oct[1] === 168) return true;
            if (oct[0] === 172 && oct[1] >= 16 && oct[1] <= 31) return true;
            return false;
        }
        if (ipVersion === 6) {
            const h = host.toLowerCase();
            if (h === '::1' || h.startsWith('fe80') || h.startsWith('fc') || h.startsWith('fd')) return true;
        }
        return false;
    })();

    if (isPrivateHost) {
        return res.status(403).json({ error: 'forbidden' });
    }

    const lib = parsed.protocol === 'http:' ? http : https;
    const options = {
        method: 'HEAD',
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === 'http:' ? 80 : 443),
        path: `${parsed.pathname || '/'}${parsed.search || ''}`,
        timeout: 3000,
        headers: { 'User-Agent': 'safe-ping/1.0' }
    };

    const r = lib.request(options, (resp) => {
        const status = resp.statusCode || 0;
        const ok = status >= 200 && status < 400;
        res.json({ ok, statusCode: status });
    });

    r.on('timeout', () => {
        r.destroy();
        res.status(504).json({ error: 'timeout' });
    });

    r.on('error', () => {
        res.status(502).json({ error: 'unreachable' });
    });

    r.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
