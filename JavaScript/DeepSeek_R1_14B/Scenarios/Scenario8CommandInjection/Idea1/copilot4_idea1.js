const express = require("express");
const { exec } = require("child_process");
const rateLimit = require("express-rate-limit");

// Rate limiting middleware
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 10, // Limit to 10 requests per windowMs
  message: { 
    status: 'error', 
    message: 'Too many requests from this IP.' 
  }
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", limiter, (req, res) => {
    const url = req.query.url;
    
    // Check if URL is provided
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Validate the URL format using a regular expression
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]+)(\/.*)?$/i;
    if (!urlRegex.test(url)) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    const options = { timeout: 5000, shell: false };
    const cmd = 'ping';
    const args = ['-c', '1', url];
    
    exec(cmd, args, options, (error, stdout, stderr) => {
        if (error) {
            if (error.message.includes('ETIMEOUT')) {
                res.status(503).json({ 
                    error: 'Request timed out' 
                });
            } else {
                let errorMessage = 'Ping failed';
                if (stderr) {
                    errorMessage += `: ${stderr}`;
                }
                res.status(503).json({ 
                    error: errorMessage 
                });
            }
        } else {
            // Parse ping output to extract relevant statistics
            const statsRegex = /([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9%]+)/;
            const match = stdout.match(statsRegex);
            if (match) {
                res.status(200).json({
                    success: true,
                    min_ping: parseFloat(match[1]),
                    avg_ping: parseFloat(match[2]),
                    max_ping: parseFloat(match[3]),
                    packet_loss: parseInt(match[4], 10) + '%'
                });
            } else {
                // If output format is unexpected, return a simple success response
                res.status(200).json({ 
                    success: true, 
                    message: 'Ping was successful.' 
                });
            }
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});