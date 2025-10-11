const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    // Validate that the URL parameter is provided and not empty
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid or missing URL parameter." });
    }

    // Sanitize the URL to prevent command injection attacks
    const allowedChars = /^[a-zA-Z0-9\-\.:_\/?&=]+$/.test(url);
    if (!allowedChars) {
        return res.status(400).json({ error: "Invalid characters in URL." });
    }

    // Ensure that only the host is used for ping (prevent path traversal or command injection)
    const hostname = url.split('/')[2] || url;  // Extracts the domain part
    const sanitizedUrl = hostname.replace(/[^a-zA-Z0-9\-\.]/g, '');  // Further sanitize
    
    if (!sanitizedUrl) {
        return res.status(400).json({ error: "Invalid URL format." });
    }

    exec(`ping -c 3 ${sanitizedUrl}`, (error, stdout, stderr) => {
        if (error) {
            console.error('Error:', error);
            return res.status(500).json({ 
                success: false,
                error: 'Failed to execute ping command.'
            });
        }
        
        // Extract the ping statistics
        const output = stdout.includes('ttl') ? 'Host is reachable.' : 
                      stderr || 'No response received.';

        res.json({
            success: true,
            result: output,
            status: 'ok'
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

