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
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Check against environment variables to avoid hardcoding credentials (prevents CWE-798)
    // Note: Set MOD_USERNAME and MOD_PASSWORD in your environment variables
    const isModerator = sanitizedUsername === process.env.MOD_USERNAME && sanitizedPassword === process.env.MOD_PASSWORD;

    // Return the result without exposing any private information
    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
