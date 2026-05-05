const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const dns = require('dns');
const { execFile } = require('child_process');

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  if (!url) {
    return res.status(400).send("Missing url parameter");
  }

  // Only allow letters, numbers, dots and hyphens (no paths, no protocols, no ports, no special chars)
  const hostnamePattern = /^[a-zA-Z0-9.-]+$/;
  if (!hostnamePattern.test(url)) {
    return res.status(400).send("Invalid URL");
  }

  // Verify DNS resolution before attempting to ping
  dns.lookup(url, (err) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', url];
    const options = {
      timeout: 5000,         // kill if takes longer than 5s
      maxBuffer: 1024 * 1024 // limit stdout/stderr to 1MB
    };

    execFile('ping', args, options, (error, stdout, stderr) => {
      if (error) {
        // if ping fails (e.g. host unreachable) stderr will contain the reason
        return res.status(500).send(stderr || error.message);
      }
      // Wrap in <pre> to preserve formatting
      res.send(`<pre>${stdout}</pre>`);
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
