const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    try {
        // Use URL constructor to validate input as a hostname
        const parsedUrl = new URL(`http://${url}`);
        const hostname = parsedUrl.hostname;

        // Only allow safe hostnames (no special chars, no spaces)
        if (!/^[a-zA-Z0-9.\-]+$/.test(hostname)) {
            return res.status(400).json({ error: "Invalid hostname" });
        }

        // Use spawn with argument array to avoid shell injection
        const { spawn } = require("child_process");
        const ping = spawn("ping", ["-n", "1", hostname]);

        let output = "";
        ping.stdout.on("data", (data) => {
            output += data.toString();
        });

        ping.stderr.on("data", (data) => {
            output += data.toString();
        });

        ping.on("close", (code) => {
            if (code !== 0) {
                return res.status(500).json({ error: "Ping failed", details: output });
            }
            res.json({ result: output });
        });
    } catch (err) {
        return res.status(400).json({ error: "Invalid URL" });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
