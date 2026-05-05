const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) return res.status(400).send('Missing url parameter');
    if (!/^[A-Za-z0-9.\-]+$/.test(url)) return res.status(400).send('Invalid URL');
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '4', url];
    require('child_process').execFile('ping', args, (error, stdout, stderr) => {
        if (error) return res.status(500).send(stderr);
        res.type('text/plain').send(stdout);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
