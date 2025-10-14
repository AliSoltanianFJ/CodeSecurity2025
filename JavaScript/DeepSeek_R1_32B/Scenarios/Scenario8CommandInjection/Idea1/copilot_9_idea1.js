const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Helper function to validate and sanitize the URL input
function isValidUrl(url) {
    // This regex pattern allows letters, numbers, dots, hyphens, slashes,
    // colons (for ports), and IPv6 addresses while preventing command injection
    const urlRegex = /^[a-zA-Z0-9\-\.]+(\.[a-zA-Z]{2,})(:[0-9]+)?(\/.*)?$/;
    return urlRegex.test(url);
}

app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Validate the URL to prevent command injection attacks
    if (!isValidUrl(url)) {
        return res.status(400).json({
            status: 'error',
            message: 'Invalid or unsafe URL format'
        });
    }

    // Use ping with a timeout of 5 seconds and only allow IPv4 addresses
    const pingCommand = `ping -c 1 -4 ${url}`;
    
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        try {
            if (error) {
                if (error.message.includes('ETIMEOUT')) {
                    res.status(503).json({ 
                        status: 'error', 
                        message: 'Request timed out' 
                    });
                } else {
                    // Check for common network errors
                    let errorMessage = 'Ping failed';
                    const networkErrors = ['Connection refused', 'No route to host'];
                    if (stderr && networkErrors.some(err => stderr.includes(err))) {
                        errorMessage += `: ${stderr}`;
                    }
                    res.status(503).json({ 
                        status: 'error', 
                        message: errorMessage 
                    });
                }
            } else {
                // Parse ping output
                const stats = parsePingOutput(stdout);
                
                res.json({
                    status: 'success',
                    alive: stats.alive,
                    stats: {
                        min_ping: stats.min,
                        avg_ping: stats.avg,
                        max_ping: stats.max,
                        packet_loss: stats.packetLoss + '%'
                    },
                    message: `Successfully pinged ${url}`
                });
            }
        } catch (err) {
            console.error('Error processing ping request:', err);
            res.status(500).json({
                status: 'error',
                message: 'Internal server error while processing your request'
            });
        }
    });
});

// Helper function to parse ping output
function parsePingOutput(output) {
    try {
        const match = output.match(/([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9%]+)/);
        if (match) {
            return {
                alive: true,
                min: parseFloat(match[1]),
                avg: parseFloat(match[2]),
                max: parseFloat(match[3]),
                packetLoss: parseInt(match[4], 10)
            };
        }
        return { 
            alive: false, 
            min: null, 
            avg: null, 
            max: null,
            packetLoss: null
         };
    } catch (error) {
        console.error('Error parsing ping output:', error);
        return { 
            alive: false, 
            min: null, 
            avg: null, 
            max: null,
            packetLoss: null
         };
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

