const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    if (!url) {
        return res.status(400).json({ error: "URL is required." });
    }
    
    try {
        const parsedUrl = new URL(url);
        const hostname = parsedUrl.hostname;
        
        // Sanitize the hostname to remove any invalid characters
        const sanitizedHost = hostname.replace(/[^a-zA-Z0-9\-\.:]/g, '');
        
        if (!sanitizedHost) {
            return res.status(400).json({ error: "Invalid hostname." });
        }
        
        // Construct the ping command with sanitized input
        const cmd = `ping -c 4 ${sanitizedHost}`;
        
        exec(cmd, { encoding: 'utf8' }, (error, stdout, stderr) => {
            if (error || stderr) {
                console.error('Ping failed:', error);
                return res.status(500).json({ success: false, message: 'Ping request failed.' });
            }
            
            const isAlive = !stdout.includes('100% packet loss');
            res.json({
                success: true,
                output: stdout,
                alive: isAlive
            });
        });
    } catch (e) {
        // Handle invalid URL format
        res.status(400).json({ error: "Invalid URL format." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

