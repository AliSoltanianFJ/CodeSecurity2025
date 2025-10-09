const express = require("express");
const crypto = require('crypto');

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

    // Input Validation: Check if username and password are provided
    if (!username || !password) {
        console.error("Missing username or password. Request:", req.query);
        return res.status(400).json({ error: "Username and password are required." });
    }

    // Input Sanitization: Basic sanitization to prevent trivial injection attempts
    const sanitizedUsername = String(username).trim();
    const sanitizedPassword = String(password).trim();

    // Centralized Secret Management: Load credentials from environment variables or a secure vault
    const moderatorUsername = process.env.MODERATOR_USERNAME;
    const moderatorPassword = process.env.MODERATOR_PASSWORD;

    if (!moderatorUsername || !moderatorPassword) {
        console.error("Moderator credentials not configured.");
        return res.status(500).json({ error: "Internal server error." });
    }

    // Constant-time comparison to prevent timing attacks
    const usernameMatch = crypto.timingSafeEqual(Buffer.from(sanitizedUsername, 'utf-8'), Buffer.from(moderatorUsername, 'utf-8'));
    const passwordMatch = crypto.timingSafeEqual(Buffer.from(sanitizedPassword, 'utf-8'), Buffer.from(moderatorPassword, 'utf-8'));

    // Authentication Logic
    if (usernameMatch && passwordMatch) {
        // Secure Logging (without sensitive data)
        console.log("Moderator access granted for user");
        return res.status(200).json({ isModerator: true });
    } else {
        // Secure Logging (without sensitive data)
        console.log("Authentication failed for user");
        return res.status(401).json({ isModerator: false, error: "Invalid credentials." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});