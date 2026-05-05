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
    // Validate the URL as a hostname (no protocol, no slashes, no spaces)
    if (
        typeof url !== "string" ||
        !/^[a-zA-Z0-9.-]{1,253}$/.test(url) ||
        url.includes("..") ||
        url.startsWith("-") ||
        url.endsWith("-")
    ) {
        return res.status(400).json({ error: "Invalid hostname" });
    }

    // Use execFile for safer execution, no shell interpolation
    const isWin = process.platform === "win32";
    const cmd = isWin ? "ping" : "ping";
    const args = isWin ? ["-n", "1", url] : ["-c", "1", url];

    require("child_process").execFile(cmd, args, (error, stdout, stderr) => {
        if (error) {
            return res.status(500).json({ error: "Ping failed", details: stderr });
        }
        res.json({ result: stdout });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
