const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const { exec, execFile } = require("child_process");
// ...existing code...

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send("Missing url parameter");
    }
    if (!/^[A-Za-z0-9.-]+$/.test(url)) {
        return res.status(400).send("Invalid url parameter");
    }
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const pingCmd = process.platform === 'win32' ? 'ping.exe' : 'ping';
    const args = [countFlag, '4', url];
    execFile(pingCmd, args, { timeout: 10000 }, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).send(stderr || error.message);
        }
        res.send(`<pre>${stdout}</pre>`);
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
