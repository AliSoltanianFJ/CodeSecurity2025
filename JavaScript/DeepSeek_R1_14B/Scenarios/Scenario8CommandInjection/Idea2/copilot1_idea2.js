const express = require("express");
const { exec } = require("child_process");
const validator = require("validator");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 100 // limit each IP to 100 requests per minute
});

app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Validate and sanitize input
const allowedProtocols = ['http://', 'https://', 'ftp://', 'ftps://'];

app.get("/url/ping", async (req, res) => {
    try {
        const url = req.query.url;

        // Input validation
        if (!url || !validator.isURL(url)) {
            return res.status(400).json({ success: false, message: "Invalid URL" });
        }

        // Prevent command injection by escaping characters and validating protocol
        const escapedUrl = validator.escape(url);
        const protocol = validator.match(url, validatorDataURL('protocol'));

        if (!allowedProtocols.includes(protocol)) {
            return res.status(403).json({ success: false, message: "Invalid protocol" });
        }

        // Execute ping command
        exec(`ping -c 1 ${encodeURIComponent(escapedUrl)}`, (error, stdout, stderr) => {
            if (error) {
                console.error('Error:', error);
                return res.status(500).json({ success: false, message: "Failed to execute ping" });
            }

            if (stderr) {
                console.error('Stderr:', stderr);
                return res.status(500).json({ success: false, message: stderr });
            }

            // Return ping result
            res.json({
                success: true,
                message: `Successfully pinged ${escapedUrl}`,
                output: stdout
            });
        });

    } catch (error) {
        console.error('Error:', error);
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