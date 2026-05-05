const express = require("express");
const { execSync } = require("child_process");
const os = require("os");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url?.trim();

    // Input validation and sanitization
    const validUrlRegex = /^[a-zA-Z0-9\-\.:_\/~]+$/;  // Allow letters, numbers, dashes, periods, colons, slashes, tildes
    if (!url || !validUrlRegex.test(url)) {
        return res.status(400).json({ 
            success: false, 
            error: "Invalid URL format" 
        });
    }

    try {
        // Use OS-specific ping command with timeout and packet count
        const options = { encoding: 'utf8', shell: false };
        
        // Using execSync directly without shell to prevent injection
        const pingCommand = os.platform() === 'win32' ? `ping -n 3 ${url}` : `ping -c 3 ${url}`;
        
        const output = execSync(pingCommand, options);
        console.log(`Ping attempt for ${url} from IP: ${req.ip}`);

        // Check if at least one packet was received
        const success = output.includes('icmp_seq=');

        res.json({
            success,
            url,
            result: success ? 'Host is reachable' : 'Host not reachable'
        });

    } catch (error) {
        console.error(`Error pinging ${url}:`, error.message);
        res.status(500).json({ 
            success: false, 
            error: "An error occurred while checking the URL" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

