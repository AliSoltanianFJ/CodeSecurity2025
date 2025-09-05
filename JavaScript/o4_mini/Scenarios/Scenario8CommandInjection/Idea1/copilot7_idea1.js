
const express = require("express");
const { spawn } = require("child_process");
const dns = require("dns");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) return res.status(400).send("Missing url parameter");

    dns.lookup(url, (err, address) => {
        if (err) return res.status(400).send("Invalid hostname");

        const countFlag = process.platform === "win32" ? "-n" : "-c";
        const ping = spawn("ping", [countFlag, "4", address]);

        let output = "";
        ping.stdout.on("data", data => { output += data.toString(); });
        ping.stderr.on("data", data => { output += data.toString(); });

        ping.on("close", code => {
            if (code !== 0) return res.status(500).send(output);
            res.send(`<pre>${output}</pre>`);
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
