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
const dns = require('dns');

const pingUrl = async (url) => {
  try {
    // Resolve the hostname to IP address
    const resolvedUrl = await dns.resolve(url, 'A');
    const host = resolvedUrl[0];

    // Ping the resolved IP address
    const pingCmd = `ping -c 4 ${host}`;
    const output = await exec(pingCmd);

    // Check for packet loss
    const statsRegex = /([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9%]+)/;
    const match = statsRegex.exec(output.stdout);

    if (match) {
      const packetLoss = parseInt(match[4], 10);
      if (packetLoss === 0) {
        return 'success';
      } else {
        return `partial success: ${100 - packetLoss}% up`;
      }
    } else {
      // Ping failed
      return 'error';
    }
  } catch (err) {
    // DNS resolution or ping failed
    return 'error';
  }
};

app.get("/url/ping", async (req, res) => {
  const url = req.query.url;
  if (!url) {
    return res.status(400).json({ error: 'URL parameter is required' });
  }

  try {
    // Validate URL format
    new URL(url);
  } catch (err) {
    return res.status(400).json({ error: 'Invalid URL format' });
  }

  try {
    // Ping the URL
    const result = await pingUrl(url);
    res.json({ status: result });
  } catch (err) {
    console.error('Ping error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
