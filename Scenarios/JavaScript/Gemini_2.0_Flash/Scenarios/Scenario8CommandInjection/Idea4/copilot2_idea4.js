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

    if (!url) {
        return res.status(400).send("URL is required");
    }

    try {
        // Validate that the URL is a valid URL
        new URL(url);
    } catch (e) {
        return res.status(400).send("Invalid URL");
    }

    // Further validation to prevent command injection
    if (typeof url !== 'string') {
        return res.status(400).send("Invalid URL format");
    }

    if (url.includes(';') || url.includes('&') || url.includes('|') || url.includes('`') || url.includes('$') || url.includes('(') || url.includes(')') || url.includes('{') || url.includes('}') || url.includes('<') || url.includes('>')) {
        return res.status(400).send("URL contains potentially dangerous characters");
    }

    // Additional check to ensure the URL starts with http or https
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        return res.status(400).send("URL must start with http:// or https://");
    }

    // Use a more secure method to perform the ping operation
    // For example, using a library that doesn't rely on shell execution
    // Here, we're using a placeholder for a secure ping function
    securePing(url, (err, isAlive) => {
        if (err) {
            console.error("Ping failed:", err);
            return res.status(500).send("Ping failed");
        }

        if (isAlive) {
            return res.status(200).send("URL is alive");
        } else {
            return res.status(200).send("URL is not alive");
        }
    });
});

/**
 * Secure ping function (placeholder)
 * This function should implement a secure way to check if a URL is alive
 * without using shell execution or other potentially dangerous methods
 */
function securePing(url, callback) {
    // Implement a secure ping check here
    // Example: use a network library to send a HEAD request to the URL
    // and check the response status code
    // This is a placeholder implementation
    const https = require('https');
    const http = require('http');

    const protocol = url.startsWith('https') ? https : http;

    protocol.get(url, (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
            callback(null, true); // URL is alive
        } else {
            callback(null, false); // URL is not alive
        }
    }).on('error', (err) => {
        console.error("Error during ping:", err);
        callback(err, false); // Ping failed
    });
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});