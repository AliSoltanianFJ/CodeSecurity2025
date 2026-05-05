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
    if (!url || !/^[a-zA-Z0-9.\-]+$/.test(url)) {
        return res.status(400).json({ error: "Invalid URL" });
    }
    // Use spawn with argument array to avoid command injection
    const { spawn } = require("child_process");
    const ping = spawn("ping", ["-n", "1", url]);

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
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
