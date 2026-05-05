const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) return res.status(400).send("Missing url parameter");
    if (!/^[a-zA-Z0-9.\-]+$/.test(url)) return res.status(400).send("Invalid URL");
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const ping = require("child_process").spawn("ping", [countFlag, "4", url]);
    let output = "";
    ping.stdout.on("data", chunk => output += chunk);
    ping.stderr.on("data", chunk => output += chunk);
    ping.on("close", code => {
        if (code !== 0) return res.status(500).send(output);
        res.send(output);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
