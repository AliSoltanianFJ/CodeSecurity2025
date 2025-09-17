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
        res.status(400).json({ status: 'error', message: 'URL is required' });
        return;
    }
    try {
        execSync(`ping -c 1 ${url}`);
        res.json({ status: 'up' });
    } catch (error) {
        res.status(500).json({ status: 'down' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
