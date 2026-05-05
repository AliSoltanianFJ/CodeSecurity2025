
const express = require("express");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).json({ error: 'Missing url parameter' });
    }
    const host = url.trim();
    const hostRegex = /^[a-zA-Z0-9.-]+$/;
    if (!hostRegex.test(host) || host.length > 253) {
        return res.status(400).json({ error: 'Invalid host' });
    }
    const args = process.platform === 'win32'
      ? ['-n', '4', host]
      : ['-c', '4', host];
    execFile('ping', args, { timeout: 5000 }, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).json({ error: 'Ping failed', details: stderr.trim() });
        }
        res.json({ success: true, output: stdout.trim() });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
