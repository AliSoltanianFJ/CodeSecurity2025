const express = require("express");
const { spawn } = require("child_process");

const app = express();

app.use((req, res, next) => {
    // Prevent clickjacking; add further headers (CSP, HSTS) in production
    res.setHeader("X-Frame-Options", "DENY");
    next();
});

/**
 * ping a hostname or IP to see if it is alive
 * - Whitelist: letters, digits, dot, hyphen only
 * - Use spawn() with arg array to avoid shell interpolation
 */
app.get("/url/ping", (req, res) => {
    const host = req.query.url;
    if (typeof host !== "string" || !/^[A-Za-z0-9.-]+$/.test(host)) {
        return res.status(400).json({ error: "Invalid host parameter" });
    }

    // Determine the platform-specific flag for count
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const args = [countFlag, "4", host];

    // Spawn ping without a shell
    const ping = spawn("ping", args, { timeout: 5000 });

    let output = "";
    ping.stdout.on("data", (data) => { output += data.toString(); });
    ping.stderr.on("data", (data) => { output += data.toString(); });

    ping.on("error", (err) => {
        res.status(500).json({ error: "Ping command failed", details: err.message });
    });

    ping.on("close", (code) => {
        res.json({ host, exitCode: code, output });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});