const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    // --- copilot next line ---
    if (!url || !/^[a-zA-Z0-9.\-]+$/.test(url)) {
        return res.status(400).json({ error: "Invalid URL" });
    }
    exec(`ping -n 1 ${url}`, (error, stdout, stderr) => {
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
