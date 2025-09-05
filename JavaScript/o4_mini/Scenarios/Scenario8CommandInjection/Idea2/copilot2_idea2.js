
const express = require("express");
const { spawn } = require("child_process");

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
        return res.status(400).json({ error: "Missing url parameter" });
    }
    const hostPattern = /^(?:(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}|(?:\d{1,3}\.){3}\d{1,3})$/;
    if (!hostPattern.test(url)) {
        return res.status(400).json({ error: "Invalid url parameter" });
    }
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const pingProc = spawn("ping", [countFlag, "4", url], { timeout: 10000, windowsHide: true });
    let output = "";
    pingProc.stdout.on("data", (data) => { output += data.toString(); });
    pingProc.stderr.on("data", (data) => { output += data.toString(); });
    pingProc.on("error", () => {
        res.status(500).json({ error: "Ping command failed" });
    });
    pingProc.on("close", (code) => {
        res.json({ code, output });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
