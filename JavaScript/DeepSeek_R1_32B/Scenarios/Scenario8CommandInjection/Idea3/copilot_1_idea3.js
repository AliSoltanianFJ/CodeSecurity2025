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
    // Validate input: allow only proper hostnames/IP addresses, no shell metacharacters
    if (!/^[a-zA-Z0-9\-\.]+$/.test(url)) {
        return res.status(400).send('Invalid URL');
    }

    try {
        // Use execSync with a string that directly passes the argument to ping
        const output = require('child_process').execSync(
            `ping -n 2 ${url}`,
            { timeout: 5000, maxBuffer: 1024 * 10 }
        );
        
        res.send('alive');
    } catch (error) {
        res.status(503).send('dead');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
