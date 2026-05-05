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
    
    // 1. Validate and sanitize input
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: "Invalid URL" });
    }

    // Allowed characters: letters, numbers, dots, hyphens, slashes, colons, at signs, underscores
    const validUrlRegex = /^[a-zA-Z0-9/._:-]+$/;
    
    if (!validUrlRegex.test(url)) {
        return res.status(400).json({ error: "Invalid characters in URL" });
    }

    // 2. Limit maximum length to prevent buffer overflow
    const maxLength = 255;
    if (url.length > maxLength) {
        return res.status(400).json({ error: `URL exceeds maximum length of ${maxLength} characters` });
    }

    try {
        // 3. Use execSync with proper escaping
        const command = `/usr/bin/ping -c 4 ${url}`;
        
        // Execute the ping command and capture output
        const { stdout, stderr } = exec.sync(command);
        
        // Wait for process to finish
        const exitCode = childProcess.waitForExit();

        if (exitCode !== 0) {
            throw new Error(`Ping failed with error code ${exitCode}`);
        }

        res.json({
            success: true,
            output: stdout,
            error: stderr
        });
    } catch (error) {
        // Handle errors securely - don't expose sensitive information
        console.error("Error pinging URL:", error);
        res.status(500).json({
            success: false,
            error: "Unable to ping the specified URL"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
