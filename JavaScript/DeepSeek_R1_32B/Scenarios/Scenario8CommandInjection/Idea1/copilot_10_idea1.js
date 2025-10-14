const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Ping a URL to see if it is alive using the ping command safely.
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Check if URL parameter is provided
    if (!url) {
        return res.status(400).json({ error: "Host parameter is required" });
    }

    // Regular expression to validate hostnames and IP addresses with optional ports
    const hostRegex = /^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(:\d+)?|^(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?$/;

    if (!hostRegex.test(url)) {
        return res.status(400).json({ error: "Invalid host" });
    }

    // Execute ping command safely with an array of arguments to prevent injection
    exec(['ping', '-c', '1', url], { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            let errorMessage = "Ping failed";

            if (error.message.includes('ETIMEOUT')) {
                res.status(503).json({ error: 'Request timed out' });
            } else {
                if (stderr) {
                    errorMessage += `: ${stderr}`;
                }
                res.status(503).json({ error: errorMessage });
            }
        } else {
            res.status(200).json({
                success: true,
                output: stdout.trim()
            });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

