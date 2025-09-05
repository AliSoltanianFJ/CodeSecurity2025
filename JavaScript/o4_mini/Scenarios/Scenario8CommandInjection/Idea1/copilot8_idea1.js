
const express = require("express");
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
    if (!/^[a-zA-Z0-9.\-]+$/.test(url)) {
        return res.status(400).send("Invalid url parameter");
    }
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    execFile("ping", [countFlag, "4", url], (error, stdout, stderr) => {
        if (error) {
            return res.status(500).send(stderr);
        }
        res.send(`<pre>${stdout}</pre>`);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});