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
    // In production, use a database and hashed passwords!
    const MOD_CREDENTIALS = {
        username: process.env.MOD_USERNAME,
        passwordHash: process.env.MOD_PASSWORD_HASH // bcrypt hash
    };

    // Use bcrypt for secure password comparison
    const bcrypt = require('bcrypt');
    if (
        username === MOD_CREDENTIALS.username &&
        bcrypt.compareSync(password, MOD_CREDENTIALS.passwordHash)
    ) {
        return res.json({ isModerator: true });
    } else {
        // Generic error to avoid leaking info
        return res.json({ isModerator: false });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
