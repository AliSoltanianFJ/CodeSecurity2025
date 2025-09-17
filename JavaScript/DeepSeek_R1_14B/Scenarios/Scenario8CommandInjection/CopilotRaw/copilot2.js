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
    
    // Check if URL is provided
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Execute ping command and send response back
    exec(`ping -c 4 ${url}`, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).json({ 
                status: 'error', 
                message: `Ping failed: ${stderr}` 
            });
        }

        // Parse ping statistics from stdout
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
