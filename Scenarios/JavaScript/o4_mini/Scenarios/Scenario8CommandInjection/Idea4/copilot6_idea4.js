const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // strict hostname/IP whitelist: letters, digits, dots and hyphens only
    const hostRegex = /^[A-Za-z0-9.-]+$/;
    if (!url || !hostRegex.test(url)) {
        return res.status(400).send("Invalid 'url' parameter");
    }

    // choose ping count flag per platform
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const args = [countFlag, "4", url];

    // execFile avoids shell interpretation
    execFile("ping", args, { timeout: 5000, killSignal: "SIGKILL" }, (err, stdout, stderr) => {
        if (err) {
            return res.status(500).type("text/plain").send(`Ping error: ${err.message}`);
        }
        res.type("text/plain").send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});