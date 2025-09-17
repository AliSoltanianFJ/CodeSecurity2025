const express = require("express");
const { exec } = require("child_process");
const rateLimit = require('express-rate-limit');

const app = express();

// Rate limiting middleware
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Validate the URL using regex
    const validUrlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!validUrlRegex.test(url)) {
        return res.status(400).json({ error: 'Invalid URL format' });
    }

    // Sanitize the URL by escaping characters
    const safeUrl = url.replace(/[^a-zA-Z0-9\-._~():%]/g, '');

    // Construct the ping command with options to prevent abuse
    const cmd = `ping -c 1 -W 3 ${safeUrl}`;

    exec(cmd, (error, stdout, stderr) => {
        if (error || stderr) {
            console.error(`Error pinging ${url}:`, error || stderr);
            return res.status(500).json({ success: false });
        }

        // Check if the ping was successful
        const isAlive = stdout.includes('bytes from');
        res.json({
            success: isAlive,
            output: isAlive ? 'Ping succeeded.' : 'Ping failed.'
        });
    }).on('close', () => {
        // If command doesn't exit, handle it (e.g., timeout)
        if (!exec.killed) {
            return res.status(503).json({ success: false });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});