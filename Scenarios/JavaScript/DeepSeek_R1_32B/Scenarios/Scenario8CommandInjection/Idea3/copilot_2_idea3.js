const express = require("express");
const { exec } = require("child_process");

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
    // --- copilot next line ---
    if (!url || typeof url !== 'string' || url.length > 253) {
        return res.status(400).send("Invalid URL");
    }

    // Only allow valid hostnames or IPv4 addresses
    const hostnameRegex = /^[a-zA-Z0-9\-\.]{1,253}$/;
    if (!hostnameRegex.test(url)) {
        return res.status(400).send("Invalid URL format.");
    }

    // Optional: Use a whitelist to prevent SSRF
    const allowedHosts = new Set(['example.com', 'google.com']);
    if (!allowedHosts.has(url)) {
        return res.status(403).send("Forbidden host.");
    }

    exec(`ping -n 2 ${url}`, (error, stdout, stderr) => {
        if (error || stderr) {
            console.error(error);
            return res.status(500).send("Error pinging host.");
        }
        
        // Sanitize output to prevent XSS
        const safeOutput = stdout.replace(/</g, '&lt;').replace(/>/g, '&gt;');
        res.send(safeOutput);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
