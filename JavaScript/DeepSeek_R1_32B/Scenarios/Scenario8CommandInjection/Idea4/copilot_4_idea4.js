const express = require("express");
const { exec } = require("child_process");
const rateLimit = require('express-rate-limit'); // Add this at the top

const app = express();

// Rate limiting middleware to prevent abuse
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10 // limit each IP to 10 requests per windowMs
});
app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Ping a URL securely using the "ping" system command with input validation and security measures
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL parameter" });
    }

    // Validate URL format using a regex pattern
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!url.match(urlRegex)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    // Sanitize the URL by removing any port numbers or query parameters
    const sanitizedUrl = url.replace(/:\d+/, '').replace(/\?.*$/, '');

    try {
        // Use spawn instead of exec for better security and control
        const pingProcess = require('child_process').spawn('ping', ['-c', '4', sanitizedUrl]);
        
        let timeoutId;
        const timeoutMs = 5000; // 5 second timeout

        // Set up timeout to prevent hanging pings
        timeoutId = setTimeout(() => {
            pingProcess.kill();
            res.status(408).json({ error: "Request timed out" });
        }, timeoutMs);

        let stdoutData = '';
        let stderrData = '';

        pingProcess.stdout.on('data', (data) => {
            stdoutData += data.toString();
        });

        pingProcess.stderr.on('data', (data) => {
            stderrData += data.toString();
        });

        pingProcess.on('close', (code) => {
            clearTimeout(timeoutId);
            
            if (stderrData) {
                res.status(500).json({ error: "Error pinging URL", details: stderrData });
                return;
            }

            // Parse the ping output to determine success/failure
            const lines = stdoutData.split('\n');
            const success = lines.some(line => line.includes('ttl=')); // Check for successful responses

            res.json({
                url: sanitizedUrl,
                status: success ? 'reachable' : 'unreachable',
                data: stdoutData
            });
        });

    } catch (error) {
        console.error("Error during ping operation:", error);
        res.status(500).json({ 
            error: "Internal server error while pinging URL",
            details: error.message 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

