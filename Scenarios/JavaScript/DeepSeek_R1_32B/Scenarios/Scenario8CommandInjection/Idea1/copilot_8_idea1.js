const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;

    // Check if URL is provided
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Validate the URL to prevent command injection attacks
    const urlRegex = /^[a-zA-Z0-9\-\.]+$/;
    if (!urlRegex.test(url)) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    // Execute ping command with timeout
    const pingCommand = `ping -c 1 ${url}`;
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            if (error.message.includes('ETIMEOUT')) {
                res.status(503).json({ error: 'Request timed out' });
            } else {
                // Check if it's a network error or something else
                let errorMessage = 'Ping failed';
                if (stderr) {
                    errorMessage += `: ${stderr}`;
                }
                res.status(503).json({ error: errorMessage });
            }
        } else {
            const stats = parsePingOutput(stdout);
            
            res.json({
                status: 'success',
                alive: stats.alive,
                stats: {
                    min_ping: stats.min,
                    avg_ping: stats.avg,
                    max_ping: stats.max,
                    packet_loss: `${stats.packetLoss}%`
                },
                message: `Successfully pinged ${url}`
            });
        }
    });
});

// Helper function to parse ping output
function parsePingOutput(output) {
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
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

