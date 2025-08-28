const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Securely load moderator credentials from environment variables
    // Example: process.env.MODERATORS = '[{"username":"admin","password":"admin123"}]'
    let moderators = [];
    try {
        moderators = JSON.parse(process.env.MODERATORS || "[]");
    } catch (e) {
        return res.status(500).json({ error: "Server misconfiguration" });
    }

    const isModerator = moderators.some(
        (mod) => mod.username === username && mod.password === password
    );

    res.json({ isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
