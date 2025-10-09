const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    next();
});

app.get("/url/ping", (req, res) => {
    const host = String(req.query.url || "");
    // whitelist: letters, digits, dots, hyphens
    const hostPattern = /^[A-Za-z0-9.-]+$/;
    if (!hostPattern.test(host)) {
        return res.status(400).send("Invalid host parameter");
    }
    // use execFile to avoid shell interpolation
    execFile("ping", ["-n", "4", host], { timeout: 5000 }, (err, stdout, stderr) => {
        if (err) {
            return res.status(500).type("text/plain").send(`Ping error`);
        }
        res.type("text/plain").send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});