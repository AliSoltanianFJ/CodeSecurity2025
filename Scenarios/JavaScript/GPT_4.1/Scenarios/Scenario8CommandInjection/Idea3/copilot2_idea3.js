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

    if (!url) return res.status(400).json({ error: "URL parameter is required" });

    const allowedPatterns = [
        /^https:\/\//,
        /^(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,}$/
    ];

    if (!allowedPatterns.some(pattern => pattern.test(url))) {
        return res.status(400).json({ 
            error: "Invalid URL format" 
        });
    }

    const safeUrl = url.trim();
    const ping = spawn('ping', ['-c', '4', safeUrl]);

    let output = '';
    ping.stdout.on('data', data => output += data.toString());
    ping.stderr.on('data', data => output += data.toString());

    ping.on('close', code => res.json({ success: code === 0, output }));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
