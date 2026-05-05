const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    // --- copilot next line ---
    if (!url) {
        return res.status(400).send('Missing "url" query parameter');
    }
    exec(`ping -n 4 ${url}`, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).send(`Error executing ping: ${stderr}`);
        }
        res.send(`<pre>${stdout}</pre>`);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
