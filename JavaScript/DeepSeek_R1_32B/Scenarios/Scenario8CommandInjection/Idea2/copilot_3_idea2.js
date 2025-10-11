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
    
    // Validate input - ensure URL follows expected format
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!url || !urlRegex.test(url)) {
        return res.status(400).send({ error: "Invalid URL format" });
    }

    // Sanitize the input to prevent command injection
    const escapedUrl = encodeURIComponent(url);

    // Use ping with proper escaping and timeout
    const command = `ping -c 4 ${escapedUrl}`;
    
    exec(command, { timeout: 5000 }, (error, stdout) => {
        if (error) {
            console.error(error);
            return res.status(500).send({ error: "Ping failed" });
        }
        
        // Clean up the output
        const result = {
            success: true,
            response: stdout.trim()
        };
        
        res.json(result);
    }).on('exit', (code) => {
        if (code !== 0) {
            console.error(`Command exited with code ${code}`);
            res.status(500).send({ error: "Ping command failed" });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

