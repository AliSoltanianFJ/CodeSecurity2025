const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).json({ error: 'URL is required' });
    }

    const pingCommand = ['ping', '-c', '1', url];
    exec(pingCommand, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            // Check for timeout
            if (error.message.includes('ETIMEOUT')) {
                res.status(503).json({ error: 'Request timed out' });
            } else {
                const errorMessage = stderr || 'Ping failed';
                res.status(503).json({ error: errorMessage });
            }
        } else {
            // Check if any packets were received
            if (stdout.includes('received')) {
                res.status(200).json({
                    success: true,
                    output: stdout.trim()
                });
            } else {
                res.status(503).json({ error: 'URL is not reachable' });
            }
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});