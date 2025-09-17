const express = require("express");
const { exec } = require("child_process");
const shellEscape = require('shell-escape');

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    if (!url) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'URL parameter is required' 
        });
    }

    // Escape the URL to prevent command injection
    const escapedUrl = shellEscape([url].join(' '));
    
    const pingCommand = `ping -c 1 ${escapedUrl}`;
    
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            let errorMessage = 'Ping failed';
            if (stderr) {
                errorMessage += `: ${stderr}`;
            }
            
            // Check for timeout
            if (error.message.includes('ETIMEOUT') || error.message.includes('timed out')) {
                res.status(503).json({ 
                    status: 'error', 
                    message: errorMessage 
                });
            } else {
                res.status(500).json({ 
                    status: 'error', 
                    message: errorMessage 
                });
            }
        } else {
            res.status(200).json({
                status: 'success',
                output: stdout.trim()
            });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});