const express = require("express");
const { exec } = require("child_process");
const url = require("url"); // For URL validation and parsing
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 3, // limit each IP to 3 requests per windowMs
});

app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const { url } = req.query;

    // Validate and sanitize input
    if (!url || typeof url !== 'string' || url.trim() === '') {
        return res.status(400).json({ error: 'Invalid URL provided' });
    }

    try {
        // Parse the URL to validate it
        const parsedUrl = new url.URL(url);
        
        // Ensure the protocol is either http or https
        if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
            return res.status(400).json({ error: 'Only HTTP and HTTPS protocols are allowed' });
        }

        const hostname = parsedUrl.hostname;
        let port = parsedUrl.port;

        // Remove any query parameters or fragments
        if (parsedUrl.search || parsedUrl.hash) {
            return res.status(400).json({ 
                error: 'Query parameters and fragments are not allowed'
            });
        }

        const command = [
            'ping',
            '-c', // One shot
            '1', // Count of pings to send
            (port ? `-${port}` : '') // Include port if available
        ].join(' ');

        execFile('/bin/ping', [ '-c', '1', hostname, ...(port ? ['-p', port] : []) ], (error, stdout) => {
            if (error) {
                return res.status(500).json({ 
                    error: 'Ping failed',
                    details: error.message 
                });
            }

            const responseTimeMatch = stdout.match(/time=(\d+)ms/);
            if (!responseTimeMatch) {
                return res.status(500).json({ error: 'Unable to parse ping response' });
            }

            const responseTime = parseInt(responseTimeMatch[1], 10);
            
            res.json({
                status: 'success',
                response_time: responseTime
            });
        });

    } catch (error) {
        return res.status(500).json({ 
            error: 'Invalid URL format',
            details: error.message 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});