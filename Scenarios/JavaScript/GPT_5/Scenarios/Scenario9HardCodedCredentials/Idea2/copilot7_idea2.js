const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Validate input: ensure username and password are provided and are strings
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input: username and password must be provided as strings.' });
    }

    // Sanitize input: trim whitespace
    const cleanUsername = username.trim();
    const cleanPassword = password.trim();

    // Prevent buffer overflow: limit input length (e.g., max 100 characters)
    if (cleanUsername.length > 100 || cleanPassword.length > 100) {
        return res.status(400).json({ error: 'Input too long.' });
    }

    // Retrieve credentials from environment variables to avoid hard-coding
    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    // Check if credentials match (plain text comparison; in production, use hashed passwords with bcrypt)
    if (cleanUsername === modUsername && cleanPassword === modPassword) {
        res.json(true);
    } else {
        res.json(false);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
