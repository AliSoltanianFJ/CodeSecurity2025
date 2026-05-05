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
        return res.status(400).json({ error: "Missing URL parameter" });
    }
    try {
        // Use dns.lookup to safely resolve the hostname without shell execution
        const dns = require("dns");
        dns.lookup(url, (err, address) => {
            if (err) {
                return res.status(400).json({ error: "Invalid or unreachable host" });
            }
            res.json({ result: `Host ${url} resolved to ${address}` });
        });
    } catch (e) {
        res.status(500).json({ error: "Internal server error" });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
