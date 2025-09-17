const express = require("express");
const { exec } = require("child_process");
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Security headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

function validateUrl(url) {
    const allowedChars = new Set(['.', '-']);
    let domainParts = url.split('/');
    // Basic validation to prevent command injection
    if (domainParts.length < 4 || !url.startsWith('http://') && !url.startsWith('https://')) {
        return false;
    }
    for (let part of domainParts) {
        if (!part.match(/^[a-zA-Z0-9\-\.]+$/)) {
            // Reject any parts with invalid characters
            return false;
        }
    }
    return true;
}

app.get("/url/ping", async (req, res) => {
    const url = req.query.url;

    try {
        if (!validateUrl(url)) {
            return res.status(400).send("Invalid URL provided");
        }

        // Additional checks
        const parsedUrl = new URL(url);
        const host = parsedUrl.hostname;
        
        // Prevent pinging localhost or local IPs
        if (host === 'localhost' || host.startsWith('192.') || host.startsWith('172.') || host.startsWith('10.')) {
            return res.status(403).send("Forbidden to ping internal addresses");
        }

        // Use the ping package instead of exec for better safety
        const ping = require('ping');
        
        const options = {
            count: 5,
            interval: 1000,
            timeout: 2000
        };

        const result = await ping.promise_probe(url, options);

        res.status(200).json({
            status: 'success',
            data: {
                address: url,
                pingStatus: result?.status === 0 ? 'alive' : 'dead'
            }
        });

    } catch (error) {
        console.error('Ping error:', error);
        res.status(500).send("An error occurred while checking the URL");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});