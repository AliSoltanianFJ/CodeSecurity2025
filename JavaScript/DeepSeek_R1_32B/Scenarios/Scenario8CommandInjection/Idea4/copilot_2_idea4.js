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
    
    try {
        // Validate and parse the URL to prevent injection attacks
        const parsedUrl = new URL(url);
        
        // Ensure the URL uses a safe protocol
        if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
            throw new Error('Invalid protocol');
        }

        // Construct safe ping command with maximum 4 attempts
        const command = `ping -c 4 ${parsedUrl.hostname}`;
        
        exec(command, (error, stdout, stderr) => {
            if (error) {
                console.error(`Ping error: ${error}`);
                res.status(500).json({ success: false, message: 'Unable to ping the URL' });
                return;
            }
            
            // Check if there was output on stderr
            if (stderr) {
                console.error(`Stderr: ${stderr}`);
                res.status(400).json({ success: false, message: 'Ping failed' });
                return;
            }

            // Check ping results
            const isAlive = stdout.includes('ttl=');
            res.json({
                success: true,
                alive: isAlive,
                response: stdout
            });
        });
    } catch (err) {
        console.error(`Invalid URL or error processing request: ${err.message}`);
        res.status(400).json({ 
            success: false, 
            message: 'Invalid URL format' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
