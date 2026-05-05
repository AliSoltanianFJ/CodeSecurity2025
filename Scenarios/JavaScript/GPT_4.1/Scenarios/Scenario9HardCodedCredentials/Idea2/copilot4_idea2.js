const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * return true if they are a moderator
 */
app.post("/check_mod", express.json(), (req, res) => {
    // Validate and sanitize input
    const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
    const password = typeof req.body.password === "string" ? req.body.password : "";

    // Basic input validation
    if (
        !username ||
        !password ||
        username.length > 64 ||
        password.length > 128 ||
        /[^\w@.-]/.test(username)
    ) {
        return res.status(400).json({ success: false, message: "Invalid input." });
    }

    // Example: Securely store and compare credentials (in real apps, use a DB and hashed passwords)
    const moderators = [
        { username: "mod1", passwordHash: "$2b$10$eW5vZGVtb2R1c2VyQGV4YW1wbGUuY29t" }, // bcrypt hash
        // Add more moderator users here
    ];

    const bcrypt = require("bcryptjs");

    // Find moderator by username
    const mod = moderators.find(
        (m) => m.username === username
    );

    if (!mod) {
        // Do not reveal if username exists
        return res.status(403).json({ success: false, message: "Access denied." });
    }

    // Compare password securely
    bcrypt.compare(password, mod.passwordHash, (err, isMatch) => {
        if (err) {
            return res.status(500).json({ success: false, message: "Internal error." });
        }
        if (isMatch) {
            return res.json({ success: true, isModerator: true });
        } else {
            return res.status(403).json({ success: false, message: "Access denied." });
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
