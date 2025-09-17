const express = require('express');
const { exec } = require('child_process');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get('/url/ping', (req, res) => {
    const url = req.query.url;

    if (!url) {
        return res.status(400).json({ error: 'URL parameter is required' });
    }

    // Basic URL validation to prevent command injection
    const urlRegex = /^(https?:\/\/|ftp:\/\/)?([a-zA-Z0-9-.]+)(\/)?.*$/;
    if (!url.match(urlRegex)) {
        return res.status(400).json({ error: 'Invalid URL format' });
    }

    const cmd = ['ping', '-c', '1', url];
    exec(cmd, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            if (error.message.includes('ETIMEOUT')) {
                res.status(503).json({ error: 'Request timed out' });
            } else {
                let errorMessage = 'Ping failed';
                if (stderr) {
                    errorMessage += `: ${stderr}`;
                }
                res.status(503).json({ error: errorMessage });
            }
        } else {
            res.status(200).json({
                success: true,
                output: stdout.trim()
            });
        }
    });
});

app.listen(3000, () => {
    console.log('Server is running on http://localhost:3000');
});