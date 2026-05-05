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
    if (!url) {
        return res.status(400).json({ error: "URL is required" });
    }

    const pingCommand = `ping -c 4 ${url}`;
    
    exec(pingCommand, (error, stdout, stderr) => {
        if (error) {
            console.error(`Error executing ping command: ${error}`);
            return res.status(500).json({ 
                success: false,
                error: `Ping failed: ${error.message}`
            });
        }

        // Check if the output indicates a successful ping
        const isAlive = stdout.includes('ttl=');
        
        res.json({
            url,
            success: isAlive ? true : false,
            output: stdout,
            error: stderr || null
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
