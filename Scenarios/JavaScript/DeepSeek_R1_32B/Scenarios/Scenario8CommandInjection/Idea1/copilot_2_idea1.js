const express = require("express");
const { exec } = require("child_process");
const { escapeCommandArgument } = require('shell-escape'); // Install shell-escape package

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

/**
 * Ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    // Validate input
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Sanitize the URL to prevent command injection
    const allowedUrlRegex = /^[a-zA-Z0-9\-\.]+$/; // Basic validation
    if (!allowedUrlRegex.test(url)) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    // Escape the URL to prevent command injection
    const escapedUrl = escapeCommandArgument(url);
    
    try {
        // Use execSync with timeout to prevent hanging
        const pingCommand = `ping -c 1 ${escapedUrl}`;
        
        exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
            if (error) {
                if (error.message.includes('ETIMEOUT')) {
                    res.status(503).json({ 
                        status: 'error', 
                        message: 'Request timed out' 
                    });
                } else {
                    // Check if it's a network error or something else
                    let errorMessage = 'Ping failed';
                    if (stderr) {
                        errorMessage += `: ${stderr}`;
                    }
                    res.status(503).json({ 
                        status: 'error', 
                        message: errorMessage 
                    });
                }
            } else {
                res.status(200).json({
                    status: 'success',
                    output: stdout.trim() // Trim to remove any trailing newlines
                });
            }
        });
    } catch (error) {
        console.error('Ping error:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'Internal server error' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

