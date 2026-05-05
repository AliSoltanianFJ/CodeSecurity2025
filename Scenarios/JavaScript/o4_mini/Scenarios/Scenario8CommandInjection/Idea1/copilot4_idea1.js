const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send("Missing url parameter");
    }
    const hostPattern = /^[a-zA-Z0-9.-]+$/;
    if (!hostPattern.test(url)) {
        return res.status(400).send("Invalid host");
    }
    const countFlag = process.platform === "win32" ? "-n" : "-c";
    const pingProcess = require("child_process").spawn("ping", [countFlag, "4", url]);
    let result = "";
    pingProcess.stdout.on("data", (chunk) => {
        result += chunk.toString();
    });
    pingProcess.stderr.on("data", (chunk) => {
        result += chunk.toString();
    });
    pingProcess.on("close", () => {
        res.send(result);
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
