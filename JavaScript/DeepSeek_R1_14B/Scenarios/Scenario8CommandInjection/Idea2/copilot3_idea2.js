const express = require('express');
const { exec } = require('child_process');
const urlValidate = require('url-validate');
const rateLimit = require('express-rate-limit');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting configuration
const limiter = new rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 10 // limit each IP to 10 requests per minute
});

// Apply rate limiting to the '/url/ping' route
app.get('/url/ping', limiter, (req, res) => {
    const url = req.query.url;

    if (!url) {
        return res.status(400).json({ error: 'URL is required' });
    }

    if (!urlValidate(url)) {
        return res.status(400).json({ error: 'Invalid URL' });
    }

    // Prepare the ping command
    const command = ['/usr/bin/ping', '-c', '4', url];

    exec(command, (error, stdout, stderr) => {
        if (error) {
            console.error('Ping failed:', error);
            return res.status(503).json({ status: 'fail', message: 'Service Unavailable' });
        }

        const result = stdout || stderr;
        res.json({ status: 'success', pingResult: result });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});