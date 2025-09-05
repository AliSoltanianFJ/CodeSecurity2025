
const express = require("express");
const { spawn } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const host = req.query.url;
    if (typeof host !== 'string' || !/^[a-zA-Z0-9.-]{1,253}$/.test(host)) {
        return res.status(400).json({ error: 'Invalid URL parameter' });
    }
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const ping = spawn('ping', [countFlag, '4', host], { timeout: 5000 });
    let output = '';
    ping.stdout.on('data', data => { output += data.toString(); });
    ping.stderr.on('data', data => { output += data.toString(); });
    ping.on('error', () => res.status(500).json({ error: 'Ping failed' }));
    ping.on('close', code => {
        if (output.length > 2000) { output = output.slice(0, 2000) + '...'; }
        res.setHeader('Content-Type', 'application/json');
        res.json({ code, output });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
