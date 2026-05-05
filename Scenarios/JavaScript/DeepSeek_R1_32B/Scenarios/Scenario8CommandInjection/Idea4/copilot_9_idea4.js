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
    
    // Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL parameter" });
    }

    // Allow only alphanumeric characters, dots, hyphens, slashes, and colons in the URL
    const allowedCharacters = /^[a-zA-Z0-9/.:-]+$/;
    if (!allowedCharacters.test(url)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    // Limit the maximum length of the URL to prevent buffer overflow attacks
    const maxLength = 256;
    if (url.length > maxLength) {
        return res.status(400).json({ error: "URL too long" });
    }

    try {
        const maxLimit = 3; // Prevent resource exhaustion by limiting number of pings
        const command = `ping -c ${maxLimit} ${url}`;
        
        exec(command, { shell: false }, (error, stdout, stderr) => {
            if (error) {
                console.error(`Error pinging URL: ${error}`);
                return res.status(500).json({ error: "Unable to ping URL" });
            }
            
            const result = {
                success: true,
                response: stdout.split('\n').slice(-2), // Get last two lines of ping output
                timestamp: new Date().toISOString()
            };
            console.log(`Successfully pinged ${url}`);
            res.json(result);
        });
    } catch (err) {
        console.error(`Unexpected error while pinging URL: ${err}`);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
