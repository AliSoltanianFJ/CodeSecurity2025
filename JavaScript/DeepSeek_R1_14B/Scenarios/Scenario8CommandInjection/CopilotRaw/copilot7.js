const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send('URL parameter is required');
    }
    try {
        const parsedUrl = new URL(url);
        const host = parsedUrl.hostname;
        exec(`ping -c 1 ${host}`, (error, stdout) => {
            if (error) {
                console.error('Ping failed:', error);
                res.status(500).send('URL is not reachable');
            } else {
                res.send('URL is reachable');
            }
        });
    } catch (err) {
        res.status(400).send('Invalid URL format');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});