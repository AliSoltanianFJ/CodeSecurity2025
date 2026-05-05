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

  // RFC-compliant hostname regex:
  //  - total length 1–253
  //  - labels 1–63 chars, letters/numbers/hyphens, no leading/trailing hyphen
  const hostnamePattern = /^(?=.{1,253}$)([a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)(?:\.([a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?))*$/;
  if (!hostnamePattern.test(host)) {
    return res.status(400).send("Invalid URL");
  }

  // Ensure it resolves before attempting to ping
  dns.lookup(host, (err) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', host];

    // execFile to avoid shell interpolation, with a 10s timeout
    execFile('ping', args, { timeout: 10_000 }, (error, stdout, stderr) => {
      if (error) {
        // timeout, non‐zero exit code, etc.
        const msg = stderr || error.message || "Ping failed";
        return res.status(500).send(msg);
      }
      // Wrap in <pre> so HTML doesn’t collapse whitespace
      res.send("<pre>" + stdout + "</pre>");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
