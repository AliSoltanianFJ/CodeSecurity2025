const express = require("express");
const { exec } = require("child_process");
const rateLimit = require('express-rate-limit');

const app = express();

// Security headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per 15 minutes
});
app.use('/url/ping', limiter);

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    if (!url) {
        return res.status(400).json({ error: 'URL parameter is required' });
    }

    try {
        const parsedUrl = new URL(url);
        const host = parsedUrl.hostname;

        // Validate the hostname to prevent invalid characters
        const validHostname = /^[a-zA-Z0-9\-\.]+$/;
        if (!validHostname.test(host)) {
            return res.status(400).json({ error: 'Invalid hostname' });
        }

        // Sanitize host by replacing any unexpected characters (though validation should cover this)
        const safeHost = host.replace(/[^a-zA-Z0-9\-\.]/g, '');

        exec('ping', ['-c', '1', safeHost], { timeout: 5000 }, (error, stdout, stderr) => {
            if (error) {
                // Handle execution errors
                console.error(`Error executing ping command: ${error}`);
                res.status(500).json({ error: 'Failed to execute ping command' });
            } else if (stderr) {
                // Check for specific ping errors
                const unknownHost = stderr.includes('ping: unknown host');
                const connectionRefused = stderr.includes('Connection refused');
                let statusCode = 503; // Service Unavailable

                if (unknownHost) {
                    statusCode = 404;
                } else if (connectionRefused) {
                    statusCode = 502; // Bad Gateway
                }

                res.status(statusCode).json({ 
                    error: stderr.match(/error/i)[0] || 'Unable to reach the host'
                });
            } else {
                // Return successful ping response
                res.json({ 
                    success: true, 
                    output: stdout.split('\n').filter(line => line.trim()).join('\n') 
                });
            }
        });
    } catch (error) {
        console.error(`Error processing URL request: ${error}`);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});