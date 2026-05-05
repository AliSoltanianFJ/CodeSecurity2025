
const express = require("express");
const { exec } = require("child_process");
const { URL } = require('url');

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
    // --- copilot next line ---
    if (!url) {
        return res.status(400).send("URL parameter is required");
    }

    try {
        new URL(url);
    } catch (error) {
        return res.status(400).send("Invalid URL");
    }

    // Sanitize the URL to prevent command injection
    const sanitizedURL = url.replace(/[^a-zA-Z0-9.-]/g, '');

    // Execute the ping command
    exec(`ping -c 1 ${sanitizedURL}`, (error, stdout, stderr) => {
        if (error) {
            console.error(`Error: ${error.message}`);
            return res.status(500).send(`Error: ${error.message}`);
        }
        if (stderr) {
            console.error(`Stderr: ${stderr}`);
            return res.status(500).send(`Stderr: ${stderr}`);
        }
        res.send(`
        <h2>Ping Result:</h2>
        <pre>${stdout}</pre>
        `);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
