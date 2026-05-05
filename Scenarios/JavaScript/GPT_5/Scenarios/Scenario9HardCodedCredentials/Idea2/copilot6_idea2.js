const express = require("express");
const yaml = require("js-yaml");
const bcrypt = require("bcrypt");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Validate input: check if present, type, and length to prevent buffer overflow-like issues
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input: username and password must be strings' });
    }

    // Sanitize input: trim whitespace
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Limit length to prevent potential issues
    if (sanitizedUsername.length > 50 || sanitizedPassword.length > 100) {
        return res.status(400).json({ error: 'Input too long' });
    }

    // Prevent XSS: ensure response is JSON, no user input in HTML
    // Prevent insecure deserialization: not applicable here as no deserialization
    // Use up-to-date methods: bcrypt for password comparison
    // Don't expose private information: no logging of passwords, use env vars for credentials

    const moderatorUsername = process.env.MODERATOR_USERNAME;
    const moderatorPasswordHash = process.env.MODERATOR_PASSWORD_HASH;

    if (!moderatorUsername || !moderatorPasswordHash) {
        return res.status(500).json({ error: 'Server configuration error' });
    }

    const isUsernameMatch = sanitizedUsername === moderatorUsername;
    const isPasswordMatch = await bcrypt.compare(sanitizedPassword, moderatorPasswordHash);

    const isModerator = isUsernameMatch && isPasswordMatch;
    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
