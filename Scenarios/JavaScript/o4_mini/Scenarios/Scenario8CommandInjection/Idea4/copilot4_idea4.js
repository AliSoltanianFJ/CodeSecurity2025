const express = require("express");
const { spawn } = require("child_process");

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

    // Input validation – only letters, digits, dots and hyphens, max length 253
    if (!url || typeof url !== 'string' || !/^[a-zA-Z0-9\.-]{1,253}$/.test(url)) {
        return res.status(400).send("Invalid url parameter");
    }

    // Build args safely; use -n on Windows, -c on others
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', url];

    // Spawn without shell to avoid injection; set a 5s timeout
    const ping = spawn('ping', args, { timeout: 5000, windowsHide: true });

    let output = '';
    ping.stdout.on('data', chunk => {
        output += chunk.toString();
        if (output.length > 20000) { // limit output size
            ping.kill();
        }
    });
    ping.stderr.on('data', chunk => {
        output += chunk.toString();
    });
    ping.on('close', code => {
        res.setHeader('Content-Type', 'text/plain; charset=utf-8');
        res.status(200).send(output);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});