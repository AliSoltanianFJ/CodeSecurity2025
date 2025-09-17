const express = require("express");
const { exec } = require("child_process");
const rateLimit = require('express-rate-limit');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting middleware
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 100 // limit each IP to 100 requests per minute
});

app.use(limiter);

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    try {
        new URL(url);
    } catch (e) {
        return res.status(400).json({ error: 'Invalid URL' });
    }

    const parsedUrl = new URL(url);
    const host = parsedUrl.hostname;

    // Sanitize the hostname to prevent command injection
    const allowedChars = /^[a-zA-Z0-9\-_.&%]+$/;
    if (!allowedChars.test(host)) {
        return res.status(400).json({ error: 'Invalid hostname' });
    }

    const execOptions = {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
    };

    exec('ping', ['-c', '1', host], execOptions, (error, stdout) => {
        if (error) {
            res.status(500).json({ status: 'down' });
        } else {
            const isAlive = stdout.includes('bytes received');
            res.json({ status: isAlive ? 'up' : 'down' });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});