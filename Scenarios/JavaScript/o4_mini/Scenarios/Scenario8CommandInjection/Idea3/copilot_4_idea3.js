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

  // Only allow simple hostnames or IP addresses (no protocols, paths, etc.)
  // This disallows “; rm -rf /” or “google.com && …”
  const hostPattern = /^([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+$|^(\d{1,3}\.){3}\d{1,3}$|^\[[0-9a-fA-F:]+\]$/;
  if (!hostPattern.test(url)) {
    return res.status(400).send("Invalid URL");
  }

  // Verify that the hostname actually resolves
  dns.lookup(url, (err) => {
    if (err) {
      return res.status(400).send("Unable to resolve host");
    }

    // On Windows ping uses -n, everywhere else -c
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', url];

    // execFile avoids a shell and thus injection
    execFile(
      'ping',
      args,
      {
        timeout: 5000,              // kill if it runs longer than 5s
        maxBuffer: 1024 * 1024      // avoid huge outputs
      },
      (error, stdout, stderr) => {
        if (error) {
          if (error.killed) {
            return res.status(500).send("Ping timed out");
          }
          // send back stderr or the error message
          return res.status(500).send(stderr || error.message);
        }
        // Wrap in <pre> so newlines render
        res.send(`<pre>${stdout}</pre>`);
      }
    );
  });
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
