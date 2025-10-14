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
    
    // Validate URL input
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Valid URL parameter is required' 
        });
    }

    // Sanitize URL input to prevent command injection
    const safeUrl = url.trim();
    if (!/^[a-zA-Z0-9\-\.]+$/.test(safeUrl)) {
        return res.status(400).json({
            status: 'error',
            message: 'Invalid characters in URL'
        });
    }

    // Use exec with arguments array to prevent command injection
    const pingCommand = ['ping', '-c', '1', safeUrl];
    
    exec(pingCommand, { timeout: 5000 }, (error, stdout) => {
        if (error) {
            let statusCode = 503;
            let message = '';
            
            if (error.message.includes('ETIMEOUT')) {
                message = 'Request timed out';
            } else {
                // Extract only the error code for security
                const errorCode = error.code || 'UNKNOWN_ERROR';
                message = `Ping failed with error: ${errorCode}`;
            }

            return res.status(statusCode).json({
                status: 'error',
                message: message
            });
        }

        try {
            // Parse ping output safely
            const lines = stdout.split('\n');
            let latencyStats = { min: null, avg: null, max: null };
            
            for (let line of lines) {
                if (line.includes('min/avg/max')) {
                    const match = line.match(/\d+.\d+/g);
                    if (match && match.length >= 3) {
                        latencyStats = {
                            min: parseFloat(match[0]),
                            avg: parseFloat(match[1]),
                            max: parseFloat(match[2])
                        };
                    }
                }
            }

            res.json({
                status: 'success',
                alive: true,
                stats: latencyStats
            });
        } catch (parseError) {
            console.error('Failed to parse ping output:', parseError);
            res.status(500).json({
                status: 'error',
                message: 'Failed to process ping response'
            });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
