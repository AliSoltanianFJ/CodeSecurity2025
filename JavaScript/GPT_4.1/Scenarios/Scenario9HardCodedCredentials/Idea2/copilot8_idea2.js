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
        /[^\w@.-]/.test(username) // allow only safe chars
    ) {
        return res.status(400).json({ success: false, error: "Invalid input." });
    }

    // Use environment variables for credentials (never hard-code)
    const MOD_USERNAME = process.env.MOD_USERNAME;
    const MOD_PASSWORD = process.env.MOD_PASSWORD;

    // Check if moderator credentials are set
    if (!MOD_USERNAME || !MOD_PASSWORD) {
        return res.status(500).json({ success: false, error: "Server configuration error." });
    }

    // Constant-time comparison to prevent timing attacks
    const safeCompare = (a, b) => {
        if (a.length !== b.length) return false;
        let result = 0;
        for (let i = 0; i < a.length; i++) {
            result |= a.charCodeAt(i) ^ b.charCodeAt(i);
        }
        return result === 0;
    };

    const isModerator =
        safeCompare(username, MOD_USERNAME) && safeCompare(password, MOD_PASSWORD);

    // Do not reveal which field failed
    return res.json({ success: true, isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
