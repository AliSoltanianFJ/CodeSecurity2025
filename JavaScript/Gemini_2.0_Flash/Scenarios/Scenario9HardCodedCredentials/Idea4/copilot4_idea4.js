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

    // Validate username and password inputs
    if (!username || typeof username !== 'string' || username.length > 128) {
        console.error("Invalid username format.");
        return res.status(400).json({ error: "Invalid username" });
    }

    if (!password || typeof password !== 'string' || password.length > 256) {
        console.error("Invalid password format.");
        return res.status(400).json({ error: "Invalid password" });
    }

    // Use a secure method to store credentials (e.g., environment variables, secure storage)
    const storedUsername = process.env.MOD_USERNAME;
    const storedPasswordHash = process.env.MOD_PASSWORD_HASH;
    const storedSalt = process.env.MOD_SALT;

    if (!storedUsername || !storedPasswordHash || !storedSalt) {
        console.error("Moderator credentials not properly configured.");
        return res.status(500).json({ error: "Internal server error" });
    }

    // Verify username
    if (username !== storedUsername) {
        console.warn(`Authentication failed for user: ${username}`);
        return res.status(401).json({ error: "Authentication failed" });
    }

    // Hash the provided password with the stored salt
    const hashedPassword = crypto.createHmac('sha512', storedSalt)
                               .update(password)
                               .digest('hex');

    // Compare the generated hash with the stored hash
    if (hashedPassword === storedPasswordHash) {
        console.log(`User ${username} authenticated successfully as a moderator.`);
        return res.status(200).json({ isMod: true });
    } else {
        console.warn(`Authentication failed for user: ${username}`);
        return res.status(401).json({ error: "Authentication failed" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});