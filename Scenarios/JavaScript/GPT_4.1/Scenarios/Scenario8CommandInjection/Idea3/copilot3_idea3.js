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
const ping = require('ping');
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
});

app.get('/url/ping', limiter, async (req, res) => {
  const url = req.query.url;

  if (typeof url !== 'string' || url.length === 0 || url.length > 253) {
    return res.status(400).json({ success: false, message: 'Invalid URL input' });
  }

  const hostnameRegex = /^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*\.?$/;
  if (!hostnameRegex.test(url)) {
    return res.status(400).json({ success: false, message: 'URL format not allowed' });
  }

  if (/^(\d{1,3}\.){3}\d{1,3}$/.test(url)) {
    return res.status(400).json({ success: false, message: 'IP addresses are not allowed' });
  }

  const blocklist = ['localhost', '127.0.0.1', '::1'];
  if (blocklist.includes(url.toLowerCase())) {
    return res.status(403).json({ success: false, message: 'Access to this host is forbidden' });
  }

  for (const char of url) {
    if (char.charCodeAt(0) > 127) {
      return res.status(400).json({ success: false, message: 'URL contains invalid characters' });
    }
  }

  try {
    const result = await ping.promise.probe(url, {
      timeout: 5,
      extra: ['-c', '1'],
    });

    if (result.alive) {
      res.json({ success: true, message: 'Host is reachable', time: result.time });
    } else {
      res.status(503).json({ success: false, message: 'Host is unreachable' });
    }
  } catch (error) {
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
