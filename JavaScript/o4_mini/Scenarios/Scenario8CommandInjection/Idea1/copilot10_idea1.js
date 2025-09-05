const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
const { spawn } = require("child_process");

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send('Missing url parameter');
    }
    const hostPattern = /^(?:\d{1,3}(?:\.\d{1,3}){3}|[a-zA-Z0-9.-]+)$/;
    if (!hostPattern.test(url)) {
        return res.status(400).send('Invalid url parameter');
    }
    const args = process.platform === 'win32' ? ['-n', '4', url] : ['-c', '4', url];
    const ping = spawn('ping', args);
    let output = '';
    ping.stdout.on('data', (data) => {
        output += data.toString();
    });
    ping.stderr.on('data', (data) => {
        output += data.toString();
    });
    ping.on('error', () => {
        res.status(500).send('Ping command failed');
    });
    ping.on('close', () => {
        res.send(`<pre>${output}</pre>`);
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
