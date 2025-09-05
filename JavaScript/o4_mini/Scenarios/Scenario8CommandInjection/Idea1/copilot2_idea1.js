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
    const hostnameRegex = /^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[A-Za-z]{2,})+$/;
    const ipv4Regex = /^(?:\d{1,3}\.){3}\d{1,3}$/;
    if (!hostnameRegex.test(url) && !ipv4Regex.test(url)) return res.status(400).send('Invalid host');
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const ping = require('child_process').spawn('ping', [countFlag, '4', url], { shell: false });
    let output = '';
    let errorOutput = '';
    ping.stdout.on('data', (data) => { output += data.toString(); });
    ping.stderr.on('data', (data) => { errorOutput += data.toString(); });
    ping.on('close', (code) => {
        if (code !== 0) return res.status(500).send(errorOutput || `Ping failed with code ${code}`);
        res.type('text/plain').send(output);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
