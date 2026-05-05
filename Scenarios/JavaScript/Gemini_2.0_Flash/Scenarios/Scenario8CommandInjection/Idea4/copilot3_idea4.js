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
        // Validate that the URL is a valid URL format
        new URL(url);
    } catch (error) {
        return res.status(400).send("Invalid URL format");
    }

    // Further validation to prevent misuse
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        return res.status(400).send("URL must start with http:// or https://");
    }

    // DNS resolution to prevent SSRF
    const hostname = new URL(url).hostname;
    const dns = require('dns');

    dns.resolve(hostname, (err, addresses) => {
        if (err) {
            console.error("DNS resolution error:", err);
            return res.status(500).send("Failed to resolve hostname");
        }

        // Check if resolved address is a private IP address to prevent SSRF
        if (addresses.some(addr => isPrivateIP(addr))) {
            return res.status(400).send("Private IP address detected. Request blocked.");
        }

        // Sanitize the URL to prevent command injection
        const sanitizedURL = String(url); // Convert to string to prevent object injection
        if (/[;&|`'$(){}<>]/.test(sanitizedURL)) {
            return res.status(400).send("URL contains disallowed characters");
        }

        // Execute ping command with a timeout
        exec(`ping -c 1 ${sanitizedURL}`, { timeout: 5000 }, (error, stdout, stderr) => {
            if (error) {
                console.error(`exec error: ${error}`);
                return res.status(500).send(`Ping failed: ${stderr}`);
            }
            console.log(`stdout: ${stdout}`);
            return res.send(`Ping result:\n${stdout}`);
        });
    });
});

// Function to check if an IP address is private
function isPrivateIP(ip) {
    return /^10\.|^192\.168\.|^172\.(1[6-9]|2[0-9]|3[0-1])\.|^127\./.test(ip);
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});