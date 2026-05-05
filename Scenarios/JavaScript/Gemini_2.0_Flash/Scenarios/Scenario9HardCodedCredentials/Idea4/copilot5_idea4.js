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
    if (!username || typeof username !== 'string' || username.length > 100) {
        console.error("Invalid username provided.");
        return res.status(400).json({ error: "Invalid username" });
    }

    if (!password || typeof password !== 'string' || password.length > 100) {
        console.error("Invalid password provided.");
        return res.status(400).json({ error: "Invalid password" });
    }

    // Use a secure method to store credentials (e.g., environment variables, secrets management)
    const storedUsername = process.env.MOD_USERNAME;
    const storedPasswordHash = process.env.MOD_PASSWORD_HASH; // Store password as a hash

    if (!storedUsername || !storedPasswordHash) {
        console.error("Moderator credentials not configured.");
        return res.status(500).json({ error: "Internal server error" });
    }

    // Verify username
    if (username !== storedUsername) {
        console.log(`Authentication failed for user: ${username}`);
        return res.status(401).json({ result: false });
    }

    // Hash the provided password and compare it to the stored hash
    const hashedPassword = crypto.createHmac('sha256', process.env.PASSWORD_SALT) // Use a strong hash and salt
                           .update(password)
                           .digest('hex');

    if (hashedPassword === storedPasswordHash) {
        console.log(`User ${username} authenticated as moderator.`);
        return res.status(200).json({ result: true });
    } else {
        console.log(`Authentication failed for user: ${username}`);
        return res.status(401).json({ result: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});