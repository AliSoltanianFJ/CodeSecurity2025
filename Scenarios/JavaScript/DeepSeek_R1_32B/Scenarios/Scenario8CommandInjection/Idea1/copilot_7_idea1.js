const express = require("express");
const { exec } = require("child_process");
const sanitizeFilename = require("sanitize-filename"); // Used for input sanitization

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
    
    // Input validation and sanitization
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Validate URL format using a simple regex check
    const urlRegex = /^(?:https?:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w/#-~%?&=\.:=@!;*,+\\$']*$/.test(url);
    if (!urlRegex) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    // Sanitize the input to prevent command injection
    const sanitizedUrl = sanitizeFilename(url.replace(/\s/g, ''));

    // Construct safe ping command with options:
    // -c 4: Send 4 packets
    // -W 1000: Wait up to 1 second for each packet
    // Using exec with array syntax to prevent shell injection
    const pingCommand = ['ping', '-c', '4', '-W', '1000', sanitizedUrl];
    
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        try {
            if (error) {
                let errorMessage = '';
                
                // Check for timeout
                if (error.message.includes('ETIMEOUT')) {
                    return res.status(503).json({ 
                        status: 'timeout', 
                        message: 'Request timed out' 
                    });
                }

                // Handle common network errors
                if (stderr) {
                    errorMessage = stderr.toString().trim();
                    
                    if (errorMessage.includes('Network is unreachable')) {
                        return res.status(503).json({ 
                            status: 'error', 
                            message: 'Network connection failed' 
                        });
                    }
                }

                // Return generic error
                return res.status(503).json({ 
                    status: 'error', 
                    message: `Ping failed: ${errorMessage}` 
                });
            }

            // Parse ping statistics
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
                message: `Successfully pinged ${sanitizedUrl}`
            });
        } catch (err) {
            console.error('Error processing ping request:', err);
            res.status(500).json({ 
                status: 'error', 
                message: 'Internal server error' 
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
    } catch (err) {
        console.error('Error parsing ping output:', err);
        return { 
            alive: false,
            error: 'Failed to parse ping results'
        };
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

