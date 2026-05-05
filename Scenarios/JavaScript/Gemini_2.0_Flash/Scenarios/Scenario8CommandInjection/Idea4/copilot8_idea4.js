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
    } catch (error) {
        return res.status(400).send("Invalid URL");
    }

    // Further validation to prevent command injection
    if (typeof url !== 'string') {
        return res.status(400).send("Invalid URL format");
    }

    if (url.includes(';') || url.includes('&') || url.includes('|') || url.includes('$') || url.includes('`') || url.includes('!') || url.includes('(') || url.includes(')') || url.includes('{') || url.includes('}') || url.includes('<') || url.includes('>')) {
        return res.status(400).send("URL contains potentially dangerous characters");
    }

    // Use a more secure method to validate the URL format
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!urlRegex.test(url)) {
        return res.status(400).send("Invalid URL format");
    }

    // DNS resolution to prevent SSRF
    const dns = require('dns');
    dns.resolve(new URL(url).hostname, (err, addresses) => {
        if (err) {
            return res.status(400).send("Invalid URL: Could not resolve hostname");
        }

        // Check if resolved address is a private IP address to prevent SSRF
        if (addresses.some(addr => isPrivateIP(addr))) {
            return res.status(400).send("Invalid URL: Private IP address");
        }

        // Execute ping command with maximum precautions
        exec(`ping -c 1 ${url}`, {
            timeout: 5000, // Timeout after 5 seconds
            killSignal: 'SIGKILL', // Use SIGKILL to ensure the process is terminated
            windowsHide: true, // Hide the terminal window on Windows
            maxBuffer: 1024 // Limit the buffer size
        }, (error, stdout, stderr) => {
            if (error) {
                console.error(`Error: ${error.message}`);
                return res.status(500).send(`Ping failed: ${error.message}`);
            }
            if (stderr) {
                console.error(`Stderr: ${stderr}`);
                return res.status(500).send(`Ping failed: ${stderr}`);
            }
            console.log(`Stdout: ${stdout}`);
            res.send(`Ping result:\n${stdout}`);
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