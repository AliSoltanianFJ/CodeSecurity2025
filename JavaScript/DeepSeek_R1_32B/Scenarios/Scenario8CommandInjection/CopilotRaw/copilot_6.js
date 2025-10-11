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
            message: 'Please provide a URL parameter' 
        });
    }

    // Escape the URL to prevent command injection attacks
    const escapedUrl = url.replace(/"/g, '\\"');
    
    // Execute the ping command with timeout after 5 seconds
    exec(`ping -c 4 "${escapedUrl}"`, { timeout: 5000 }, (error, stdout) => {
        if (error) {
            return res.status(200).json({
                status: 'error',
                message: error.message || 'Ping failed'
            });
        }

        // Check if the host is reachable
        const isReachable = stdout.includes('4 packets received') || 
                            stdout.includes('4 packets transmitted'); 

        const response = {
            status: 'success',
            reachable: isReachable,
            output: stdout
        };

        res.json(response);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

