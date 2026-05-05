const express = require("express");
const { exec } = require("child_process");
const { escape } = require("shell-escape");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Required parameter check
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Validate URL format to prevent command injection
    const urlPattern = /^(?:https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!url.match(urlPattern)) {
        return res.status(400).json({
            status: 'error',
            message: 'Invalid URL format'
        });
    }

    // Extract hostname from URL if it contains a protocol
    let hostname = url;
    try {
        const parsedUrl = new URL(hostname);
        hostname = parsedUrl.hostname;
    } catch (e) {
        // If URL doesn't have a protocol, assume it's just the hostname
        // and validate it separately
        const ipPattern = /^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/;
        if (!ipPattern.test(hostname)) {
            return res.status(400).json({
                status: 'error',
                message: 'Invalid URL or IP format'
            });
        }
    }

    // Escape the hostname to prevent command injection
    const escapedHostname = escape([hostname]);
    
    // Use an array to pass arguments safely to exec
    const pingCommand = ['ping', '-c', '1', '--timeout', '5', escapedHostname];
    
    exec(pingCommand, (error, stdout, stderr) => {
        if (error) {
            console.error(`Ping error: ${error.message}`);
            
            // Check for timeout specifically
            if (stderr.includes('Timeout')) {
                return res.status(503).json({
                    status: 'error',
                    message: 'Request timed out'
                });
            }

            return res.status(503).json({
                status: 'error',
                message: 'Ping failed'
            });
        }

        // Check if the host is reachable
        const isAlive = stdout.includes('bytes from') || 
                       stdout.includes('icmp_seq=0');

        res.json({
            status: 'success',
            alive: isAlive,
            output: stdout.trim()
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

