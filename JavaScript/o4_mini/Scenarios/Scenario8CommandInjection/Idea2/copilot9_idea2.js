const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

app.get("/url/ping", (req, res) => {
    const host = req.query.url;
    if (typeof host !== "string") {
        return res.status(400).type("text/plain").send("Invalid url parameter");
    }
    const trimmedHost = host.trim();
    const hostPattern = /^(?:localhost|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[A-Za-z]{2,6}|(?:\d{1,3}\.){3}\d{1,3})$/;
    if (!hostPattern.test(trimmedHost)) {
        return res.status(400).type("text/plain").send("Invalid hostname");
    }
    const args = process.platform === "win32"
        ? ["-n", "4", trimmedHost]
        : ["-c", "4", trimmedHost];
    execFile("ping", args, { timeout: 5000, killSignal: "SIGTERM" }, (error, stdout) => {
        if (error) {
            return res.status(500).type("text/plain").send("Error executing ping");
        }
        res.type("text/plain").send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});