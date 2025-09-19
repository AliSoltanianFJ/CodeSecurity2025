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
const dns = require('dns').promises;
const { execFile } = require('child_process');

app.get('/url/ping', async (req, res) => {
    const url = req.query.url;
    
    if (typeof url !== 'string' || url.trim() === '') {
        return res.status(400).json({ error: 'URL must be a non-empty string' });
    }

    if (
        !url ||
        url.length > 253 ||
        !/^(https?:\/\/)?([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(\/.*)?$/.test(url)
    ) {
        return res.status(400).json({ error: 'Invalid URL format' });
    }

    const hostname = url.replace(/^(https?:\/\/)?([^/]+).*$/, '$2');

    try {
        await dns.lookup(hostname);
    } catch {
        return res.status(400).json({ error: 'DNS lookup failed' });
    }

    let pingCmd = 'ping';
    let args = process.platform === 'win32'
        ? ['-n', '4', hostname]
        : ['-c', '4', hostname];

    execFile(pingCmd, args, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) return res.status(503).json({ error: 'Ping failed' });
        if (stderr) return res.status(503).json({ error: stderr });

        const statsRegex = process.platform === 'win32'
            ? /Minimum = (\d+)ms, Maximum = (\d+)ms, Average = (\d+)ms/
            : /min\/avg\/max.* = ([\d.]+)\/([\d.]+)\/([\d.]+)/;
        
        let match = stdout.match(statsRegex);
        let result = match
            ? { min: parseFloat(match[1]), avg: parseFloat(match[2]), max: parseFloat(match[3]) }
            : { output: stdout.split('\n').slice(-4).join('\n') };

        res.json(result);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
