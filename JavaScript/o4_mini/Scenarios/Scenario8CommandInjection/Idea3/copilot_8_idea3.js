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
  // 1) required
  if (!host) {
    return res.status(400).send("Missing url parameter");
  }

  // 2) simple hostname / IPv4 / IPv6 pattern
  //    - allows letters, digits, dots and hyphens (for hostnames)
  //    - allows IPv4 dotted form
  //    - allows IPv6 in bracketed form [::1]
  //    - rejects anything else (no ports, no paths, no shell metacharacters)
  const hostPattern = /^(\[?[A-Fa-f0-9:.]+\]?|[a-zA-Z0-9.-]+)$/;
  if (!hostPattern.test(host)) {
    return res.status(400).send("Invalid hostname");
  }

  // 3) ensure it really resolves
  dns.lookup(host.replace(/^\[|\]$/g, ''), (dnsErr) => {
    if (dnsErr) {
      return res.status(400).send("Unable to resolve host");
    }

    // 4) choose the right flag for count
    const countFlag = process.platform === 'win32' ? '-n' : '-c';

    // 5) execFile (no shell!), limit to 4 pings, 10s timeout max
    execFile(
      'ping',
      [countFlag, '4', host],
      { timeout: 10_000, maxBuffer: 1024 * 1024 },
      (err, stdout, stderr) => {
        if (err) {
          // ping returns non-zero on unreachable host,
          // so stderr may be informative
          const msg = stderr || err.message;
          return res
            .status(502)
            .send(`<pre>${escapeHtml(msg)}</pre>`);
        }
        // 6) send raw output
        res.send(`<pre>${escapeHtml(stdout)}</pre>`);
      }
    );
  });
});

// simple HTML escaper
function escapeHtml(s) {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
