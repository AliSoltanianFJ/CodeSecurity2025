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
    
    // Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ success: false, message: "Invalid URL" });
    }

    // Validate URL format using regex
    const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!url.match(urlRegex)) {
        return res.status(400).json({ success: false, message: "Invalid URL format" });
    }

    // Define allowed domains or IP addresses (update this list as needed)
    const allowedHosts = ['example.com', 'localhost', '127.0.0.1'];
    
    try {
        const parsedUrl = new URL(url);
        if (!allowedHosts.includes(parsedUrl.hostname)) {
            return res.status(403).json({ 
                success: false, 
                message: "Forbidden - Host not allowed"
            });
        }

        // Build the ping command safely
        const command = ['ping', '-c', '1', parsedUrl.hostname];
        
        exec(command, { shell: false }, (error, stdout, stderr) => {
            if (error) {
                console.error(`Ping error: ${error.message}`);
                return res.status(500).json({
                    success: false,
                    message: "Error pinging URL"
                });
            }
            
            const response = {
                success: true,
                data: stdout.trim()
            };

            // Log the ping result (use a proper logging system in production)
            console.log(`Pinged ${url}:`, response);

            res.json(response);
        });
    } catch (error) {
        console.error(`Error processing request: ${error.message}`);
        res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

