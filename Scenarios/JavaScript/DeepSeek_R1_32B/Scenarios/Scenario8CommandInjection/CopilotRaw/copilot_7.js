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
    
    // Check if URL is provided and is a string
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Please provide a valid URL in the query parameters'
        });
    }

    const escapedUrl = escapeShellString(url);
    
    // Use ping command to check connectivity
    exec(`ping -c 4 ${escapedUrl}`, (error, stdout) => {
        if (error) {
            console.error('Error:', error);
            return res.status(500).json({
                status: 'down',
                message: `An error occurred while pinging the URL: ${error.message}`
            });
        }

        // Check ping output to determine if host is reachable
        const received = stdout.includes('4 packets received');
        const result = {
            status: received ? 'up' : 'down'
        };

        // Additional parsing for uptime or specific message can be added here
        
        res.json({
            ...result,
            message: received 
                ? `Successfully pinged ${url}` 
                : `${url} is unreachable`
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

// Helper function to escape special characters in URLs for shell commands
function escapeShellString(str) {
    return str.replace(/([^\w])/g, '\\$1');
}

