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
    
    // Check if URL parameter is provided
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    try {
        // Validate and parse the URL
        const parsedUrl = new URL(url);
        const host = parsedUrl.hostname;
        
        // Execute ping command with sanitized hostname
        const pingCommand = ['ping', '-c', '1', host];
        exec(pingCommand, { timeout: 3000 }, (error, stdout, stderr) => {
            if (error) {
                let errorMessage = 'Ping failed';
                if (stderr) {
                    errorMessage += `: ${stderr}`;
                }
                // Check for specific error types
                if (error.message.includes('ETIMEOUT')) {
                    return res.status(503).json({ 
                        status: 'error', 
                        message: 'Request timed out' 
                    });
                } else if (stdout.toLowerCase().includes('unknown host')) {
                    return res.status(404).json({
                        status: 'error',
                        message: 'Host not found'
                    });
                }
                return res.status(503).json({ 
                    status: 'error', 
                    message: errorMessage 
                });
            } else {
                // Parse ping output
                const stats = parsePingOutput(stdout);
                if (stats.packetLoss < 100) {
                    return res.status(200).json({
                        status: 'success',
                        alive: true,
                        stats: {
                            min_ping: stats.min,
                            avg_ping: stats.avg,
                            max_ping: stats.max,
                            packet_loss: `${stats.packetLoss}%`
                        }
                    });
                } else {
                    return res.status(503).json({
                        status: 'error',
                        message: 'Host not reachable'
                    });
                }
            }
        });
    } catch (err) {
        // Handle any parsing errors
        res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }
});

// Helper function to parse ping output
function parsePingOutput(output) {
    const match = output.match(/(\d+.\d+)/g);
    if (match) {
        // The pattern matches three numbers for min, avg, max and then another number for packet loss percentage.
        // Example: '1.23 4.56 7.89 100%'
        const [min, avg, max] = match.slice(0, 3).map(Number);
        const packetLossMatch = output.match(/(\d+)% packet loss/);
        const packetLoss = packetLossMatch ? parseInt(packetLossMatch[1], 10) : 100;
        return {
            alive: true,
            min,
            avg,
            max,
            packetLoss
        };
    }
    return { 
        alive: false, 
        min: null, 
        avg: null, 
        max: null, 
        packetLoss: 100 
    };
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

