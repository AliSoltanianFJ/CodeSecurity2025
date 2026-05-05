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

    // Validate input
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL parameter" });
    }

    // Use regex to validate URL format
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!url.match(urlRegex)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    // Sanitize input to prevent command injection
    const sanitizedUrl = url.replace(/[^a-zA-Z0-9\-\.\/\:]/g, '');

    try {
        // Use URL class to ensure proper URL formatting and parsing
        const parsedUrl = new URL(sanitizedUrl);
        
        // Validate URL components
        if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
            return res.status(400).json({ error: "Only HTTP/HTTPS URLs are allowed" });
        }

        // Use ping command with limited options to prevent abuse
        const pingCommand = `ping -c 1 -W 5 ${parsedUrl.hostname}`;

        exec(pingCommand, { encoding: 'utf8' }, (error, stdout) => {
            if (error) {
                res.status(500).json({ error: "Ping failed", details: error.message });
            } else {
                res.json({ success: true, output: stdout });
            }
        });
    } catch (e) {
        res.status(400).json({ error: "Invalid URL", details: e.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

