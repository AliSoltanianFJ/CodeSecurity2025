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
// add these two requires near the top of your file
const dns = require('dns');
const { execFile } = require('child_process');

app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  if (!url) {
    return res.status(400).send("Missing url parameter");
  }

  // very simple whitelist for hostnames: letters, digits, dots and dashes
  const hostnamePattern = /^[a-zA-Z0-9.-]+$/;
  if (!hostnamePattern.test(url)) {
    return res.status(400).send("Invalid URL");
  }

  // Make sure it actually resolves before pinging
  dns.lookup(url, (err /*, address, family */) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    // turn off shell expansion entirely
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    execFile('ping', [countFlag, '4', url], { timeout: 10_000 }, (error, stdout, stderr) => {
      if (error) {
        // you may choose to expose less of stderr in prod
        return res.status(500).send(stderr || error.message);
      }
      // wrap in <pre> so HTML browsers don’t reformat it
      res.send(`<pre>${stdout}</pre>`);
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
