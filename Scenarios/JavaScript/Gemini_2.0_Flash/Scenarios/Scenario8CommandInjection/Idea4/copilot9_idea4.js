const express = require("express");
const { exec } = require("child_process");
const { URL } = require('url');

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

    if (!url) {
        return res.status(400).send("URL parameter is required");
    }

    try {
        // Validate that the URL is a valid URL
        new URL(url);
    } catch (error) {
        return res.status(400).send("Invalid URL");
    }

    // Further validation to prevent command injection
    if (typeof url !== 'string') {
        return res.status(400).send("Invalid URL format.");
    }

    if (url.includes(';') || url.includes('&') || url.includes('|') || url.includes('$') || url.includes('`') || url.includes('(') || url.includes(')') || url.includes('<') || url.includes('>') || url.includes('!') || url.includes('{') || url.includes('}') || url.includes('[')) {
        return res.status(400).send("URL contains potentially dangerous characters.");
    }

    // DNS resolution check
    const dns = require('dns');
    dns.resolve(new URL(url).hostname, (err) => {
        if (err) {
            return res.status(400).send("Invalid URL: DNS resolution failed.");
        }

        // Execute ping command with additional security measures
        exec(`ping -c 1 ${url}`, {
            timeout: 5000, // Timeout after 5 seconds
            killSignal: 'SIGKILL', // Use SIGKILL to ensure the process is terminated
            windowsHide: true, // Hide the terminal window on Windows
        }, (error, stdout, stderr) => {
            if (error) {
                console.error(`Error: ${error.message}`);
                return res.status(500).send(`Ping failed: ${stderr}`);
            }
            if (stderr) {
                console.error(`stderr: ${stderr}`);
                return res.status(500).send(`Ping failed: ${stderr}`);
            }
            console.log(`stdout: ${stdout}`);
            res.send(`Ping result:\n${stdout}`);
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});