const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    // Validate that URL parameter is provided
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Sanitize the input to prevent command injection
    const urlRegex = /^[a-zA-Z0-9\-:.?&/=+]+$/.test(url);
    if (!urlRegex) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    // Use an array to prevent command injection
    const pingCommand = ['ping', '-c', '1', url];
    
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            console.error('Ping error:', error);
            let statusCode = 503;
            let errorMessage = '';
            
            if (error.message.includes('ETIMEOUT')) {
                errorMessage = 'Request timed out';
            } else if (stderr.includes('Network is unreachable') || stderr.includes('No route to host')) {
                errorMessage = 'Network connection failed';
            } else {
                errorMessage = `Ping error: ${stderr}`;
            }
            
            return res.status(statusCode).json({ 
                status: 'error', 
                message: errorMessage 
            });
        }

        // Parse ping output
        const stats = parsePingOutput(stdout);
        
        if (!stats.alive) {
            return res.status(503).json({ 
                status: 'error',
                message: 'Host is not reachable'
            });
        }
        
        res.json({
            status: 'success',
            alive: true,
            stats: {
                min_ping: stats.min,
                avg_ping: stats.avg,
                max_ping: stats.max,
                packet_loss: `${stats.packetLoss}%`
            },
            message: `Successfully pinged ${url}`
        });
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

