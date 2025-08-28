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

    // Validate: Only allow valid IPv4, IPv6, or hostname (no special chars)
    const hostnameRegex = /^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63})*\.?$/;
    const ipv4Regex = /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;
    const ipv6Regex = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|::1)$/;

    if (
        !url ||
        url.length > 253 ||
        (!hostnameRegex.test(url) && !ipv4Regex.test(url) && !ipv6Regex.test(url))
    ) {
        return res.status(400).json({ error: "Invalid URL parameter." });
    }

    // Use spawn with arguments array to avoid command injection
    const { spawn } = require("child_process");
    const ping = spawn("ping", ["-n", "2", url]);

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
            // Only return a safe summary, not raw output
            return res.json({ result: "Host is reachable." });
        } else {
            return res.status(400).json({ result: "Host is unreachable.", error: errorOutput.trim() });
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
