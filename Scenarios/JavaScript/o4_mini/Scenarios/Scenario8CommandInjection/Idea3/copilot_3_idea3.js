const express = require("express");
const dns = require("dns");
const { execFile } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  if (!url) {
    return res.status(400).send("Missing url parameter");
  }

  // Only allow bare hostnames (letters, digits, dots, hyphens). No protocol, no paths, no ports.
  const hostnamePattern = /^[a-zA-Z0-9.-]+$/;
  if (!hostnamePattern.test(url)) {
    return res.status(400).send("Invalid URL");
  }

  // Make sure it actually resolves before we ping it
  dns.lookup(url, (dnsErr) => {
    if (dnsErr) {
      return res.status(400).send("Unable to resolve host");
    }

    // Choose the correct count flag for the platform
    const countFlag = process.platform === 'win32' ? '-n' : '-c';

    // execFile runs the binary directly; no shell involved
    execFile('ping', [countFlag, '4', url], { timeout: 10_000 }, (pingErr, stdout, stderr) => {
      if (pingErr) {
        // If ping itself returns a non‐zero exit code, still show the stderr if any
        const errorMessage = stderr && stderr.trim() ? stderr : pingErr.message;
        return res.status(500).send(`<pre>${errorMessage}</pre>`);
      }
      // Wrap the output in <pre> so it formats nicely in the browser
      res.send(`<pre>${stdout}</pre>`);
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});