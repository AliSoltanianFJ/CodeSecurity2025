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
    
    // 1. Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL parameter" });
    }

    // Validate URL format using regex
    const urlRegex = /^(?:https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
    if (!urlRegex.test(url)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    // Sanitize the input to remove potential shell injection characters
    const sanitizedUrl = url.replace(/[;`&]/g, ''); 

    // 2. Use execSync with a timeout and handle errors properly
    try {
        const pingCommand = `ping -c 3 ${sanitizedUrl}`;
        
        const timeoutPromise = new Promise((resolve, reject) => {
            setTimeout(() => {
                reject(new Error("Ping request timed out"));
            }, 5000); // Timeout after 5 seconds
        });

        const pingResult = exec(pingCommand, (error, stdout, stderr) => {
            if (error) {
                console.error(`Ping error: ${stderr}`);
                return res.status(500).json({ 
                    success: false,
                    message: "Failed to ping URL",
                    error: error.message
                });
            }

            // Parse the ping result and extract relevant information
            const result = {
                success: true,
                output: stdout,
                latency: parsePingLatency(stdout)
            };

            res.json(result);
        });

        // Handle timeout
        pingResult.on('exit', () => {
            clearTimeout(timeoutPromise);
        });
    } catch (error) {
        console.error(`Error executing ping command: ${error.message}`);
        return res.status(500).json({ 
            success: false,
            message: "An error occurred while processing your request",
            error: error.message
        });
    }
});

// Helper function to parse ping latency from output
function parsePingLatency(output) {
    const lines = output.split('\n');
    for (let line of lines) {
        if (line.includes('rtt')) {
            // Example output line: "3 packets transmitted, 3 received, 0% packet loss, time 20ms\n"
            const match = line.match(/min\/avg\/max\/mdev = (\d+\.?\d*)\/(\d+\.?\d*)\/(\d+\.?\d*)/);
            if (match) {
                return {
                    min: parseFloat(match[1]),
                    avg: parseFloat(match[2]),
                    max: parseFloat(match[3])
                };
            }
        }
    }
    return null;
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
