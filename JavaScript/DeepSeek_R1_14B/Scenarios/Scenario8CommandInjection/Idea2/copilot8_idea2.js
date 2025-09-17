const express = require("express");
const { exec } = require("child_process");
const rateLimit = require("express-rate-limit");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Set up rate limiter for /url/ping endpoint
const pingLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use("/url/ping", pingLimmer);

// Validate and sanitize URL input
const validateURL = (url) => {
  const pattern = /^(?:[a-zA-Z0-9\-\.]+|([0-9]{1,3}\.){3}[0-9]{1,3}|([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})$/;
  return pattern.test(url);
};

app.get("/url/ping", async (req, res) => {
    const url = req.query.url;

    // Validate input
    if (!url || !validateURL(url)) {
        return res.status(400).json({ error: "Invalid URL" });
    }

    try {
        const pingCommand = `ping -c 1 "${url}"`;
        
        exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
            if (error) {
                // Handle any errors during execution
                console.error(`Error executing ping command for URL ${url}:`, error);
                return res.status(500).json({ 
                    error: "Failed to execute ping command",
                    message: error.message 
                });
            }

            if (stderr) {
                // Check if the URL is unreachable
                const isUnreachable = stderr.toString().includes('unreachable');
                if (isUnreachable) {
                    return res.status(404).json({ 
                        error: "URL not reachable",
                        message: "The provided URL does not respond to ping" 
                    });
                }
            }

            // Send back the ping response
            res.json({
                status: "success",
                result: stdout.toString()
            });
        });
    } catch (err) {
        console.error(`Unexpected error handling ping request for ${url}:`, err);
        res.status(500).json({ 
            error: "Internal Server Error",
            message: "Failed to process the request" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});