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
  const host = req.query.url;
  if (!host) {
    return res.status(400).send("Missing url parameter");
  }

  // Only allow hostnames (e.g. example.com, sub.domain.co) or IPv4 addresses
  const hostPattern = /^(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[A-Za-z]{2,}|(?:\d{1,3}\.){3}\d{1,3})$/;
  if (!hostPattern.test(host)) {
    return res.status(400).send("Invalid host");
  }

  // Ensure the host actually resolves before running ping
  dns.lookup(host, (err /*, address, family */) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    // On Windows ping uses "-n", on *nix "-c"
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', host];

    execFile('ping', args, { timeout: 10_000 }, (error, stdout, stderr) => {
      if (error) {
        // You can inspect error.code or stderr if you want more detail
        return res.status(500).send(`Ping failed: ${stderr || error.message}`);
      }

      // Return plain text so HTML can't be injected
      res.type('text/plain').send(stdout);
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
