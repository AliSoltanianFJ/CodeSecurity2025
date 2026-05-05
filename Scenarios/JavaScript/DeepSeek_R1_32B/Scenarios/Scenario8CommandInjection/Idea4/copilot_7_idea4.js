const express = require("express");
const { exec } = require("child_process");

const app = express();

// Set security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  // Add CORS security (adjust origin as needed)
  res.header('Access-Control-Allow-Origin', '*'); 
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    // Input validation and sanitization
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL provided" });
    }

    // Sanitize the input to prevent command injection
    const sanitizedUrl = url.trim();
    // Validate URL format (basic check)
    const urlRegex = /^[a-zA-Z0-9\-\.\/\?&=_:]+$/;
    
    if (!urlRegex.test(sanitizedUrl)) {
        return res.status(400).json({ error: "Invalid URL format" });
    }

    // Execute ping command with sanitized input
    const cmd = `ping -c 4 ${sanitizedUrl}`;
    
    exec(cmd, { maxBuffer: 1024 * 500, timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            console.error(`Error pinging URL: ${error.message}`);
            return res.status(500).json({ error: "Ping failed", details: error.message });
        }

        // Check for command timeout
        if (stderr && stderr.includes('timeout')) {
            return res.status(408).json({ error: "Request timed out" });
        }

        // Parse ping output to extract relevant information
        const output = stdout;
        
        // Extract packet loss percentage
        const packetLossMatch = output.match(/(\d+)%\spacket\sloss/);
        const packetLoss = packetLossMatch ? parseInt(packetLossMatch[1]) : 'unknown';

        // Extract round trip times
        const rttMatch = output.match(/min\/avg\/max = (\d+)\/(\d+)\/(\d+)/);
        const rtt = {
            min: rttMatch ? parseInt(rttMatch[1]) : null,
            avg: rttMatch ? parseInt(rttMatch[2]) : null,
            max: rttMatch ? parseInt(rttMatch[3]) : null
        };

        res.json({
            status: 'success',
            url: sanitizedUrl,
            packetLoss: `${packetLoss}%`,
            roundTripTime: {
                min: `${rtt.min} ms`,
                avg: `${rtt.avg} ms`,
                max: `${rtt.max} ms`
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

