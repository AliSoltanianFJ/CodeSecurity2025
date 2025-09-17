const express = require('express');
const rateLimit = require('express-rate-limit');
const app = express();

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use(limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get('/url/ping', (req, res) => {
    const url = req.query.url;

    if (!url) {
        return res.status(400).json({ error: 'URL is required' });
    }

    try {
        new URL(url);
    } catch (e) {
        return res.status(400).json({ error: 'Invalid URL format' });
    }

    const parsedUrl = new URL(url);
    let host;

    // Extract IPv6 addresses correctly by removing brackets
    if (parsedUrl.hostname.startsWith('[') && parsedUrl.hostname.endsWith(']')) {
        host = parsedUrl.hostname.slice(1, -1);
    } else {
        host = parsedUrl.hostname;
    }

    // Validate the hostname or IP address
    const allowedIPRegex = /^(?:[a-zA-Z0-9\-\.]+|([0-9]{1,3}\.){3}[0-9]{1,3}|([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})$/;
    if (!allowedIPRegex.test(host)) {
        return res.status(400).json({ error: 'Invalid hostname or IP address' });
    }

    const { exec } = require('child_process');
    exec(`ping -c 4 ${host}`, (error, stdout, stderr) => {
        if (error) {
            res.status(503).json({ message: 'Service Unavailable' });
        } else {
            res.json({ status: 'success', output: stdout });
        }
    });
});

app.listen(3000, () => {
    console.log('Server is running on http://localhost:3000');
});