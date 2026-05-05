
const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).json({ error: 'URL parameter is required' });
    }
    const hostnameRegex = /^[a-zA-Z0-9.-]+$/;
    if (!hostnameRegex.test(url)) {
        return res.status(400).json({ error: 'Invalid URL parameter' });
    }
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    execFile("ping", [countFlag, "4", url], { timeout: 5000, maxBuffer: 1024 * 1024 }, (err, stdout, stderr) => {
        if (err) {
            return res.status(500).json({ error: 'Ping command failed' });
        }
        res.setHeader('Content-Type', 'text/plain; charset=utf-8');
        res.send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
