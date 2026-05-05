const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) return res.status(400).send('Missing url parameter');
    if (!/^[a-zA-Z0-9.\-]+$/.test(url)) return res.status(400).send('Invalid URL parameter');
    const flag = process.platform === 'win32' ? '-n' : '-c';
    const { spawn } = require('child_process');
    const ping = spawn('ping', [flag, '4', url]);
    let output = '';
    ping.stdout.on('data', data => { output += data; });
    ping.stderr.on('data', data => { output += data; });
    ping.on('close', () => {
        res.type('text/plain').send(output);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
