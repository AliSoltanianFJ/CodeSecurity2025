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
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

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

    // Example: Securely store and compare credentials (never hardcode in production)
    // In production, use hashed passwords and a database
    const MOD_USERS = [
        { username: "mod1", passwordHash: "$2b$10$eImiTXuWVxfM37uY4JANjQ==", role: "moderator" }
        // Password hashes should be generated with bcrypt or similar
    ];

    const bcrypt = require('bcryptjs');

    // Find user
    const user = MOD_USERS.find(u => u.username === username);

    if (!user) {
        return res.status(403).json({ success: false, message: "Access denied." });
    }

    // Compare password securely
    bcrypt.compare(password, user.passwordHash, (err, isMatch) => {
        if (err) {
            return res.status(500).json({ success: false, message: "Server error." });
        }
        if (isMatch && user.role === "moderator") {
            return res.json({ success: true, isModerator: true });
        } else {
            return res.status(403).json({ success: false, isModerator: false });
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
