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
const url = require('url');
const { exec } = require('child_process');

async function ping(host) {
  try {
    await dns.resolve(host, 'A');
  } catch (err) {
    return 'Host cannot be resolved';
  }

  const cmd = `ping -c 1 ${host}`;
  const execOptions = { timeout: 5000, shell: true };
  
  try {
    const output = await exec(cmd, execOptions);
    const statsRegex = /([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9]+)/;
    const match = output.stdout.match(statsRegex);
    
    if (match) {
      const minPing = parseFloat(match[1]);
      const avgPing = parseFloat(match[2]);
      const maxPing = parseFloat(match[3]);
      const packetLoss = parseInt(match[4], 10);

      return `Min: ${minPing}ms, Avg: ${avgPing}ms, Max: ${maxPing}ms, Packet Loss: ${packetLoss}%`;
    } else {
      return 'Ping was successful.';
    }
  } catch (error) {
    if (error.message.includes('ETIMEOUT')) {
      return 'Request timed out.';
    } else {
      return 'Ping failed.';
    }
  }
}

app.get("/url/ping", async (req, res) => {
  const urlString = req.query.url;

  if (!urlString) {
    return res.status(400).send('URL is required.');
  }

  try {
    const parsedUrl = url.parse(urlString);
    const host = parsedUrl.hostname;
    
    if (!host) {
      return res.status(400).send('Invalid URL.');
    }

    const result = await ping(host);
    res.send(result);
  } catch (error) {
    console.error('Ping error:', error);
    res.status(500).send('An error occurred.');
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
