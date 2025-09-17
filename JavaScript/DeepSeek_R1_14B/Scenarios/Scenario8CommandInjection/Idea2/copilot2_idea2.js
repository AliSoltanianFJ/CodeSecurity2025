const express = require('express');
const { exec } = require('child_process');
const rateLimit = require('express-rate-limit');

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/url/ping', limiter);

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    if (!url) {
        res.status(400).json({ status: 'error', message: 'URL parameter is required' });
        return;
    }

    // Parse the URL
    const parsedUrl = require('url').parse(url);
    if (!parsedUrl.href || !['http:', 'https:'].includes(parsedUrl.protocol)) {
        res.status(400).json({ status: 'error', message: 'Invalid URL format' });
        return;
    }

    // Extract the host
    const host = parsedUrl.hostname;

    // Regular expression to validate hostname
    const hostnameRegex = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;
    if (!host || !hostnameRegex.test(host)) {
        res.status(400).json({ status: 'error', message: 'Invalid hostname' });
        return;
    }

    // Construct the ping command
    const pingCommand = ['ping', '-c', '1', host];

    exec(pingCommand, { shell: false }, (error, stdout) => {
        if (error !== null) {
            res.status(503).json({ 
                status: 'down', 
                message: error.message || 'Host not reachable' 
            });
        } else {
            // Check if the ping was successful
            const isAlive = stdout.includes('1 packet received');
            res.json(isAlive ? { status: 'up' } : { status: 'down', message: 'Host not reachable' });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});