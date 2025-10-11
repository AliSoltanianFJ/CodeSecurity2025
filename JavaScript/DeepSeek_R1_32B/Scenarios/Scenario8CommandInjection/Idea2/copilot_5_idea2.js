const express = require("express");
const { exec } = require("child_process");
const url = require('url');

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const urlParam = req.query.url;
    if (!urlParam) {
        return res.status(400).json({ error: "URL parameter is required." });
    }

    try {
        // Parse the URL to extract the hostname
        const parsedUrl = new url.URL(urlParam);
        let hostname = parsedUrl.hostname;

        if (!hostname) {
            // If parsing fails, check if it's a direct hostname or IP
            hostname = urlParam.split('/')[0];
            if (hostname.includes(':')) {
                // Extract hostname part before port
                hostname = hostname.split(':')[0];
            }
        }

        // Sanitize the hostname to prevent command injection
        const sanitizedHost = hostname.replace(/[^a-zA-Z0-9\-\.]/g, '');

        if (!sanitizedHost) {
            return res.status(400).json({ error: "Invalid URL." });
        }

        // Construct the ping command with timeout and packet count
        const pingCommand = `ping -c 4 -W 1 ${sanitizedHost}`;

        exec(pingCommand, (err, stdout, stderr) => {
            if (err) {
                console.error(`Error pinging url`, err);
                return res.status(500).json({ error: "An error occurred while pinging the URL." });
            }

            // Check if there was packet loss
            const hasNoLoss = stdout.includes('0% packet loss');
            res.json({ alive: hasNoLoss });
        });

    } catch (error) {
        console.error(`Error processing request for provided url`, error);
        res.status(500).json({ error: "An unexpected error occurred." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

