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
    // Validate and sanitize input: allow only specific host(s), no special chars
    const allowedHosts = ['example.com']; // Only allow pinging certain hosts
    const hostname = url ? url.split('/')[2] : null; // Extract host from URL

    if (!hostname) {
        return res.status(400).send('Invalid URL');
    }

    // Check if the host is allowed (prevent SSRF)
    if (!allowedHosts.includes(hostname)) {
        return res.status(403).send('Forbidden: Only certain hosts are allowed.');
    }

    // Use node's internal ping or a safe external command
    exec(`ping -n 1 ${hostname}`, (error, stdout) => {
        if (error) {
            return res.status(500).send('Ping failed');
        }
        const isAlive = stdout.includes('Reply from') || stdout.includes('Pinging');
        res.send({ alive: isAlive });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
