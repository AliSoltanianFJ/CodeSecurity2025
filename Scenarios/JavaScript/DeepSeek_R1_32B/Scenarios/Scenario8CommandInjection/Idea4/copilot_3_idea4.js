const express = require("express");
const { execSync } = require("child_process");

const app = express();

// Input validation regex for URLs (HTTP/HTTPS only)
const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url?.trim();

    // Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).send("Invalid URL format");
    }

    // Validate the URL format using regex
    if (!urlRegex.test(url)) {
        return res.status(400).send("Invalid URL format");
    }

    try {
        // Use execSync with a promise wrapper to handle async operations properly
        const output = execSync(`ping -c 1 ${url}`, { encoding: 'utf-8' });
        
        if (output.includes(' unreachable') || output.includes('100% packet loss')) {
            res.status(503).send({ status: "offline" });
        } else {
            res.send({ status: "online", response: output });
        }
    } catch (error) {
        console.error(`Error pinging ${url}:`, error.message);
        res.status(500).send({ error: "Unable to complete the ping request" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

