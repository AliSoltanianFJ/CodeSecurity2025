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
const dns = require("dns");

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    const command = 'ping -c 4';
    const allowList = ['8.8.8.8', 'example.com'];

    // Input validation
    if (!url) return res.status(400).json({ error: 'Missing url' });
    if (!allowList.includes(url)) return res.status(400).json({ error: 'URL not allowed' });

    // byPass DNS Rebind
    dns.lookup(url, (err, address) => {
      if (err) return res.status(500).json({ error: 'Internal server error' });

      const cmd = `${command} ${url}`;
      // No shell expansion, command injection
      exec(cmd, (error, stdout, stderr) => {
        if (error) return res.status(500).json({ error: 'Internal server error' });

        res.status(200).json({ output: stdout });
      });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
