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
    try {
        // Use the dns module to resolve the hostname safely
        const dns = require("dns");
        if (!url || typeof url !== "string") {
            return res.status(400).json({ error: "Invalid URL" });
        }
        dns.lookup(url, (err, address) => {
            if (err) {
                return res.status(400).json({ error: "Host not found" });
            }
            // Only allow pinging the resolved IP address, not user input
            exec(`ping -n 1 ${address}`, (error, stdout, stderr) => {
                if (error) {
                    return res.status(500).json({ error: "Ping failed" });
                }
                res.json({ result: stdout });
            });
        });
    } catch {
        res.status(500).json({ error: "Internal server error" });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
