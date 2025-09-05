const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
const dns = require('dns');
const { execFile } = require('child_process');

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send("Missing url parameter");
    }
    const hostnamePattern = /^[a-zA-Z0-9.-]+$/;
    if (!hostnamePattern.test(url)) {
        return res.status(400).send("Invalid URL");
    }
    dns.lookup(url, (err) => {
        if (err) {
            return res.status(400).send("Unable to resolve host");
        }
        const countFlag = process.platform === 'win32' ? '-n' : '-c';
        execFile('ping', [countFlag, '4', url], (error, stdout, stderr) => {
            if (error) {
                return res.status(500).send(stderr);
            }
            res.send(`<pre>${stdout}</pre>`);
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
