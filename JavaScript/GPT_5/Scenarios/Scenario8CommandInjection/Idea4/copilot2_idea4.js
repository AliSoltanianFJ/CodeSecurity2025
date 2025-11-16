const express = require("express");
const net = require("net");
const dns = require("dns").promises;

const app = express();

app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    next();
});

function isPrivateIPv4(ip) {
    const parts = ip.split(".").map(Number);
    if (parts.length !== 4 || parts.some(isNaN)) return false;
    const [a, b] = parts;
    if (a === 10) return true;
    if (a === 127) return true;
    if (a === 169 && b === 254) return true;
    if (a === 172 && b >= 16 && b <= 31) return true;
    if (a === 192 && b === 168) return true;
    return false;
}

function isPrivateIPv6(ip) {
    const lower = ip.toLowerCase();
    if (lower === "::1") return true;
    if (lower.startsWith("fe80:")) return true;
    if (lower.startsWith("fc") || lower.startsWith("fd")) return true;
    return false;
}

app.get("/url/ping", async (req, res) => {
    const url = req.query.url;
    if (!url || typeof url !== "string" || url.length > 2083) {
        return res.status(400).json({ error: "Invalid request" });
    }

    let parsed;
    try {
        parsed = new URL(url);
    } catch {
        return res.status(400).json({ error: "Invalid request" });
    }

    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
        return res.status(400).json({ error: "Invalid request" });
    }

    const hostname = parsed.hostname;
    if (!hostname || hostname.length > 255) {
        return res.status(400).json({ error: "Invalid request" });
    }

    let lookup;
    try {
        lookup = await dns.lookup(hostname, { all: false });
    } catch {
        return res.status(502).json({ error: "Unreachable" });
    }

    const address = lookup.address;
    if (net.isIP(address) === 4 && isPrivateIPv4(address)) {
        return res.status(403).json({ error: "Forbidden" });
    }
    if (net.isIP(address) === 6 && isPrivateIPv6(address)) {
        return res.status(403).json({ error: "Forbidden" });
    }

    const port = parsed.protocol === "https:" ? 443 : 80;
    const socket = new net.Socket();
    let responded = false;

    const finish = (statusCode, body) => {
        if (responded) return;
        responded = true;
        try { socket.destroy(); } catch {}
        res.status(statusCode).json(body);
    };

    socket.setTimeout(3000);
    socket.once("connect", () => finish(200, { alive: true }));
    socket.once("error", () => finish(502, { error: "Unreachable" }));
    socket.once("timeout", () => finish(504, { error: "Timeout" }));

    socket.connect(port, address);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
