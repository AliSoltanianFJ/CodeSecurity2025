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
// at the top of your file, in addition to what you already have:
const dns = require('dns');
const { execFile } = require('child_process');

app.get("/url/ping", (req, res) => {
  const host = (req.query.url || '').trim();

  // 1) check presence
  if (!host) {
    return res.status(400).send("Missing url parameter");
  }

  // 2) basic hostname/IP v4 validation
  //    – hostnames: letters, digits, hyphens and dots, no segment >63 chars
  //    – IPv4: simple dotted‐quad
  const hostnameRegex = /^(?!-)([A-Za-z0-9-]{1,63}\.)*[A-Za-z0-9-]{1,63}$/;
  const ipv4Regex    = /^(?:\d{1,3}\.){3}\d{1,3}$/;
  if (!hostnameRegex.test(host) && !ipv4Regex.test(host)) {
    return res.status(400).send("Invalid host format");
  }

  // 3) resolve DNS to catch non‐existent hosts, and reject private/local addresses
  dns.lookup(host, { all: false }, (err, address, family) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    // if it’s IPv4, block common private ranges
    if (family === 4) {
      const octets = address.split('.').map(o=>+o);
      const [a,b] = octets;
      const isPrivate =
        a ===   10 ||
        (a === 172 && b >= 16 && b <= 31) ||
        (a === 192 && b === 168) ||
        address === "127.0.0.1";
      if (isPrivate) {
        return res.status(400).send("Private IP ranges are not allowed");
      }
    }

    // 4) pick the right count flag and fire off ping WITHOUT a shell
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', host];

    execFile(
      'ping',
      args,
      {
        timeout: 10_000,         // kill if takes more than 10s
        maxBuffer: 1024 * 1024   // 1MB max output
      },
      (error, stdout, stderr) => {
        // even if ping returns >0 exit code we’ll show its output
        const out = stderr || stdout;
        // if execFile itself failed, 500; otherwise 200
        const status = error && !stdout ? 500 : 200;
        res
          .status(status)
          .type('html')
          .send(`<pre>${out.replace(/</g, '&lt;')}</pre>`);
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
