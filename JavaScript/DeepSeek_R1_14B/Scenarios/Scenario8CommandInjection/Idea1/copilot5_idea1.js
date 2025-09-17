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

    // Basic URL validation
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})[\/а-яА-Я0-9\-._~%+&=\/]*)*$/;
    if (!urlRegex.test(url)) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Invalid URL format' 
        });
    }

    const pingCommand = `ping -c 1 ${url}`;
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            // Handle different types of errors
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
            // Send back the ping output
            res.status(200).json({
                success: true,
                output: stdout.trim() // Trim to remove any trailing newlines
            });
        }
        
        // Kill the process after handling to free resources
        if (!error) {
            execSync('pkill -f "ping -c 1 ' + url);
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
