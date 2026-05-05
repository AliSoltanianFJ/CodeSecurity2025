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

    // Validate input: Only allow valid hostnames or IPv4 addresses
    const hostnameRegex = /^(?!:\/\/)([a-zA-Z0-9-_]{1,63}\.)*[a-zA-Z0-9][a-zA-Z0-9-_]{0,63}\.[a-zA-Z]{2,6}$|^(\d{1,3}\.){3}\d{1,3}$/;
    if (!url || typeof url !== "string" || url.length > 253 || !hostnameRegex.test(url)) {
        return res.status(400).json({ error: "Invalid URL" });
    }

    // Use spawn with argument array to avoid command injection
    const { spawn } = require("child_process");
    const ping = spawn("ping", ["-n", "3", url]);

    let output = "";
    let errorOutput = "";

    ping.stdout.on("data", (data) => {
        output += data.toString();
    });

    ping.stderr.on("data", (data) => {
        errorOutput += data.toString();
    });

    ping.on("close", (code) => {
        if (code === 0) {
            // Sanitize output to prevent XSS
            res.setHeader("Content-Type", "text/plain; charset=utf-8");
            res.send(output.replace(/[<>]/g, ""));
        } else {
            res.status(500).json({ error: "Ping failed", details: errorOutput.replace(/[<>]/g, "") });
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
