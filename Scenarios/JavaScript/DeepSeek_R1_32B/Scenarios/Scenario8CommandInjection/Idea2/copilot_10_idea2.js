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

    // Validate input: ensure URL contains only allowed characters
    if (!url || !/^[a-zA-Z0-9\-\.]+(\.[a-zA-Z]{2,})?$/.test(url)) {
        return res.status(400).json({ 
            success: false, 
            error: "Invalid URL format. Only alphanumeric characters, dots, and hyphens are allowed." 
        });
    }

    // Maximum number of packets to send for ping
    const packetCount = 4;

    try {
        // Use execSync with timeout to prevent hanging
        const result = exec.sync(`ping -c ${packetCount} ${url}`, { 
            encoding: 'utf-8',
            timeout: 5000 // 5 seconds timeout
        });

        res.json({
            success: true,
            alive: true,
            response: result
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            alive: false,
            error: "Unable to reach the specified URL."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
