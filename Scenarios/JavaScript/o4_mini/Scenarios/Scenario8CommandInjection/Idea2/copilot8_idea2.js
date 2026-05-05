

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
    const target = req.query.url;
    const hostnamePattern = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(?:\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
    const ipv4Pattern = /^(?:\d{1,3}\.){3}\d{1,3}$/;
    if (!target || (!hostnamePattern.test(target) && !ipv4Pattern.test(target))) {
        return res.status(400).json({ error: "Invalid target" });
    }
    const args = process.platform === "win32" ? ["-n", "4", target] : ["-c", "4", target];
    execFile("ping", args, { timeout: 5000, maxBuffer: 1024 * 1024 }, (err, stdout, stderr) => {
        if (err) {
            return res.status(502).json({ error: "Ping command failed" });
        }
        res.type("text/plain; charset=utf-8").send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
