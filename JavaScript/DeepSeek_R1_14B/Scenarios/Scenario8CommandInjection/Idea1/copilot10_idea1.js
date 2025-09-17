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
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }
    
    const pingCommand = `ping -c 1 ${url}`;
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
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
            res.status(200).json({
                success: true,
                output: stdout.trim()
            });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
