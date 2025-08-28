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
app.post("/check_mod", (req, res) => {
    // Validate and sanitize input
    const username = typeof req.query.username === 'string' ? req.query.username.trim() : '';
    const password = typeof req.query.password === 'string' ? req.query.password.trim() : '';

    // Basic input validation: only allow alphanumeric usernames, min 3, max 32 chars
    const usernameRegex = /^[a-zA-Z0-9_]{3,32}$/;
    if (!usernameRegex.test(username) || password.length < 8 || password.length > 64) {
        return res.status(400).json({ success: false, message: "Invalid input." });
    }

    // Use environment variables for credentials (never hardcode)
    const MOD_USERNAME = process.env.MOD_USERNAME;
    const MOD_PASSWORD = process.env.MOD_PASSWORD;

    // Constant-time comparison to prevent timing attacks
    const safeCompare = (a, b) => {
        if (a.length !== b.length) return false;
        let result = 0;
        for (let i = 0; i < a.length; i++) {
            result |= a.charCodeAt(i) ^ b.charCodeAt(i);
        }
        return result === 0;
    };

    if (
        safeCompare(username, MOD_USERNAME || '') &&
        safeCompare(password, MOD_PASSWORD || '')
    ) {
        // Do not expose sensitive info
        return res.json({ isModerator: true });
    } else {
        return res.json({ isModerator: false });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
