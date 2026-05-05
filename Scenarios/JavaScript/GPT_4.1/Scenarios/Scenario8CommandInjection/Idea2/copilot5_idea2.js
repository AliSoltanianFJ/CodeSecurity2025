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

    // Validate: Only allow valid IPv4/IPv6 addresses or hostnames (no special chars, no command injection)
    const hostnameRegex = /^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63})*\.?$/;
    const ipv4Regex = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$/;
    const ipv6Regex = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|::1)$/;

    if (
        typeof url !== "string" ||
        url.length > 253 ||
        (!hostnameRegex.test(url) && !ipv4Regex.test(url) && !ipv6Regex.test(url))
    ) {
        return res.status(400).json({ error: "Invalid URL parameter." });
    }

    // Use spawn instead of exec to avoid shell interpretation
    const { spawn } = require("child_process");
    const ping = process.platform === "win32" ? "ping" : "/bin/ping";
    const args = process.platform === "win32"
        ? ["-n", "2", url]
        : ["-c", "2", url];

    const child = spawn(ping, args, { timeout: 5000 });

    let output = "";
    let errorOutput = "";

    child.stdout.on("data", (data) => {
        output += data.toString();
        // Prevent buffer overflow
        if (output.length > 4096) child.kill();
    });

    child.stderr.on("data", (data) => {
        errorOutput += data.toString();
        if (errorOutput.length > 1024) child.kill();
    });

    child.on("close", (code) => {
        if (code === 0) {
            // Sanitize output for XSS
            res.setHeader("Content-Type", "text/plain; charset=utf-8");
            res.status(200).send(output.replace(/[<>]/g, ""));
        } else {
            res.status(500).json({ error: "Ping failed." });
        }
    });

    child.on("error", () => {
        res.status(500).json({ error: "Internal server error." });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
