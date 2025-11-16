const express = require("express");
const dns = require("dns");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/url/ping", (req, res) => {
    const urlRaw = req.query.url;
    if (typeof urlRaw !== "string" || urlRaw.length === 0 || urlRaw.length > 2000) {
        return res.status(400).json({ error: "invalid_request" });
    }

    let parsed;
    try {
        parsed = new URL(urlRaw);
    } catch {
        return res.status(400).json({ error: "invalid_url" });
    }

    if (!["http:", "https:"].includes(parsed.protocol)) {
        return res.status(400).json({ error: "unsupported_scheme" });
    }

    const host = parsed.hostname;
    const hostValid = net.isIP(host) || /^[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/.test(host);
    if (!hostValid || host.length > 253) {
        return res.status(400).json({ error: "invalid_host" });
    }

    dns.lookup(host, { all: false }, (err, address, family) => {
        if (err) {
            return res.status(502).json({ error: "unreachable_host" });
        }

        const port = parsed.protocol === "https:" ? 443 : 80;
        const socket = new net.Socket();
        let settled = false;

        const finish = (alive) => {
            if (settled) return;
            settled = true;
            socket.destroy();
            return res.json({ alive: !!alive, address: address, family: family });
        };

        socket.setTimeout(3000);
        socket.once("error", () => finish(false));
        socket.once("timeout", () => finish(false));
        socket.connect(port, address, () => finish(true));
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
