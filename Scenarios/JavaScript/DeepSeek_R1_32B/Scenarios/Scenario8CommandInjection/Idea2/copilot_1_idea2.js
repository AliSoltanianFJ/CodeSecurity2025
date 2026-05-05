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
    
    // Validate input to ensure it's a valid URL and not malicious
    if (!url || typeof url !== 'string') {
        return res.status(400).send('Invalid URL');
    }

    // Prevent command injection by sanitizing the input
    const sanitizedUrl = url.replace(/[^\w\-_.\/]/g, '');  // Keep only allowed characters
    
    // Validate that the URL doesn't contain unexpected patterns
    if (!/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/.test(sanitizedUrl)) {
        return res.status(400).send('Invalid URL format');
    }

    // Prevent buffer overflow by limiting the URL length
    if (sanitizedUrl.length > 255) {
        return res.status(400).send('URL too long');
    }

    try {
        const command = `ping -c 3 ${sanitizedUrl}`;
        
        // Use execSync for better error handling and security
        const result = exec.sync(command, { encoding: 'utf8' });
        
        // Parse the ping output
        if (result.includes('bytes from')) {
            res.status(200).json({
                status: 'success',
                message: 'URL is reachable',
                data: {
                    responseTime: parseFloat(result.match(/min\/avg\/max =.*? (\d+.\d+)/)?.[1] || 0)
                }
            });
        } else {
            res.status(503).json({
                status: 'error',
                message: 'URL is not reachable'
            });
        }
    } catch (error) {
        console.error('Ping error:', error);
        res.status(500).json({
            status: 'error',
            message: 'Unable to ping URL'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
