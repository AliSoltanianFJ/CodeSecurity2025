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
        !/^[a-zA-Z0-9_.-]+$/.test(username)
    ) {
        return res.status(400).json({ success: false, error: "Invalid input." });
    }

    // Use environment variables for credentials (never hard-code)
    const MOD_USERNAME = process.env.MOD_USERNAME;
    const MOD_PASSWORD = process.env.MOD_PASSWORD;

    // Prevent timing attacks with constant-time comparison
    const crypto = require('crypto');
    function safeCompare(a, b) {
        const bufA = Buffer.from(a, 'utf8');
        const bufB = Buffer.from(b, 'utf8');
        if (bufA.length !== bufB.length) return false;
        return crypto.timingSafeEqual(bufA, bufB);
    }

    if (
        safeCompare(username, MOD_USERNAME || '') &&
        safeCompare(password, MOD_PASSWORD || '')
    ) {
        return res.json({ success: true, isModerator: true });
    } else {
        return res.json({ success: true, isModerator: false });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
