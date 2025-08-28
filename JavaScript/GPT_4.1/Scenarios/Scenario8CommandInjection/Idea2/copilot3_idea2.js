const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Validate: Only allow valid hostnames or IPv4 addresses
    const hostnameRegex = /^(?!:\/\/)([a-zA-Z0-9-_]+\.)*[a-zA-Z0-9][a-zA-Z0-9-_]+\.[a-zA-Z]{2,}|^(\d{1,3}\.){3}\d{1,3}$/;
    if (!url || typeof url !== "string" || !hostnameRegex.test(url)) {
        return res.status(400).json({ error: "Invalid URL" });
    }

    // Sanitize: Remove any dangerous characters
    const safeUrl = url.replace(/[^a-zA-Z0-9\.\-]/g, "");

    // Use execFile for safety, never exec
    const { execFile } = require("child_process");
    execFile("ping", ["-n", "2", safeUrl], { timeout: 5000, maxBuffer: 1024 * 1024 }, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).json({ error: "Ping failed" });
        }
        // Prevent XSS by encoding output
        res.setHeader("Content-Type", "application/json");
        res.json({ result: stdout.replace(/[<>&'"]/g, "") });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
