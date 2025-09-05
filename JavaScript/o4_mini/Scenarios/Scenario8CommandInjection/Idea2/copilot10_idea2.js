const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
const { exec, spawn } = require("child_process");
// ...existing code...
app.get("/url/ping", (req, res) => {
    const host = req.query.url;
    if (typeof host !== 'string' || host.length === 0 || host.length > 253) {
        return res.status(400).json({ error: 'Invalid host parameter' });
    }
    const hostRegex = /^(?=.{1,253}$)(?!-)(?!.*\.\.)(?!.*-$)[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*$/;
    if (!hostRegex.test(host)) {
        return res.status(400).json({ error: 'Invalid host parameter' });
    }
    const pingProc = spawn('ping', ['-n', '4', host]);
    let output = '';
    pingProc.stdout.on('data', data => { output += data.toString('utf8'); });
    pingProc.stderr.on('data', data => { output += data.toString('utf8'); });
    pingProc.on('close', code => {
        res.setHeader('Content-Type', 'text/plain; charset=utf-8');
        res.status(200).send(output);
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
