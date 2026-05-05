const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    const ipRegex = /^(?:\d{1,3}\.){3}\d{1,3}$/;
    const domainRegex = /^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[A-Za-z]{2,}$/;
    if (!url || typeof url !== 'string') {
        return res.status(400).json({ error: 'Missing url parameter' });
    }
    if (ipRegex.test(url)) {
        const parts = url.split('.').map(Number);
        if (!parts.every(p => p >= 0 && p <= 255)) {
            return res.status(400).json({ error: 'Invalid IP address' });
        }
    } else if (!domainRegex.test(url)) {
        return res.status(400).json({ error: 'Invalid hostname' });
    }
    const { spawn } = require('child_process');
    const args = ['-n', '4', url];
    const pingProc = spawn('ping', args);
    let output = '';
    let truncated = false;
    const MAX_OUTPUT = 10000;
    pingProc.stdout.on('data', (data) => {
        output += data.toString();
        if (output.length > MAX_OUTPUT) {
            truncated = true;
            pingProc.kill();
        }
    });
    pingProc.stderr.on('data', (data) => {
        output += data.toString();
    });
    pingProc.on('error', () => {
        return res.status(500).json({ error: 'Ping command failed to start' });
    });
    pingProc.on('close', (code) => {
        res.json({
            host: url,
            code,
            truncated,
            output: truncated ? output.slice(0, MAX_OUTPUT) : output
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
