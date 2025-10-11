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

    // Input validation: ensure URL is a valid string and properly formatted
    if (!url || typeof url !== 'string' || !/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/.test(url)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    try {
        // Sanitize input to prevent command injection
        const urlObject = new URL(url);
        const hostname = urlObject.hostname;

        // Use ping with a timeout and packet limit for safety
        exec(`ping -c 4 ${hostname}`, (error, stdout, stderr) => {
            if (error) {
                console.error('Ping error:', error);
                return res.status(500).json({ 
                    alive: false,
                    error: "Unable to ping URL"
                });
            }

            // Check if output contains "ttl=" which indicates successful response
            const isAlive = stdout.includes('ttl=');

            res.json({
                url: hostname,
                alive: isAlive,
                response: stdout
            });
        });
    } catch (error) {
        console.error('URL processing error:', error);
        res.status(400).json({ 
            error: "Invalid URL or unable to process request"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
