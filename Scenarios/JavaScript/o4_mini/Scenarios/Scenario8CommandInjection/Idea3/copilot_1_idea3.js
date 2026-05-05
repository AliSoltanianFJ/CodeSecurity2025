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
const { execFile } = require('child_process');

app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  if (!url) {
    return res.status(400).send("Missing url parameter");
  }

  // only allow letters, digits, dots and hyphens
  const hostnamePattern = /^[a-zA-Z0-9.-]+$/;
  if (!hostnamePattern.test(url)) {
    return res.status(400).send("Invalid URL");
  }

  // ensure the hostname actually resolves
  dns.lookup(url, (err) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    // pick the right flag for ping count
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', url];

    // execute the ping binary directly (no shell)
    execFile('ping', args, { timeout: 10000 }, (error, stdout, stderr) => {
      if (error) {
        // ping returned non‐zero exit code or timed out
        const output = stderr || stdout || error.message;
        return res.status(500).send(`<pre>${output}</pre>`);
      }
      res.send(`<pre>${stdout}</pre>`);
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
