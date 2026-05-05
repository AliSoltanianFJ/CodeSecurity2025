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
app.get("/url/ping", async (req, res) => {
    const url = req.query.url;

    // Rate limiting (if not already applied globally)
    const limiter = rateLimit({
        windowMs: 60 * 1000, // 1 minute
        max: 10,
        message: 'Too many requests, please try again later.',
        standardHeaders: true,
        legacyHeaders: false
    });

    // Input validation
    if (typeof url !== 'string' || url.length === 0 || url.length > 253) return res.status(400).json({ error: 'Invalid URL' });

    // Check for valid hostname or IP address
    const hostnameRegex = /^(?=.{1,253}$)(([a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)\.)*([A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)$/;
    const ipv4Regex = /^(25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})(\.(25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})){3}$/;
    const ipv6Regex = /^(([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$/;

    if (!hostnameRegex.test(url) && !ipv4Regex.test(url) && !ipv6Regex.test(url)) return res.status(400).json({ error: 'URL must be a valid hostname or IP address' });

    // Prevent SSRF by blocking internal/private IP ranges
    const privateIpRegex = /^(localhost|127\.0\.0\.1|0\.0\.0\.0|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3})$/;
    if (privateIpRegex.test(url)) return res.status(403).json({ error: 'Access to private/internal resources is not allowed' });

    // Use execFile instead of exec to avoid shell injection
    const pingCmd = process.platform === 'win32' ? 'ping.exe' : 'ping';
    const pingArgs = process.platform === 'win32' ? ['-n', '1', url] : ['-c', '1', url];

    try {
        const { execFile } = require('child_process');
        await new Promise((resolve, reject) => {
            execFile(pingCmd, pingArgs, { timeout: 5000 }, (error, stdout, stderr) => {
                if (error) return reject(error);
                resolve();
            });
        });
        res.json({ success: true, message: `Ping to ${url} successful` });
    } catch (error) {
        res.status(503).json({ success: false, message: `Ping to ${url} failed` });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
