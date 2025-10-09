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
        console.error("Invalid username provided.");
        return res.status(400).json({ error: "Invalid username" });
    }

    if (!password || typeof password !== 'string' || password.length > 256) {
        console.error("Invalid password provided.");
        return res.status(400).json({ error: "Invalid password" });
    }

    // Use a secure method to store credentials (e.g., environment variables, secrets management)
    // DO NOT HARDCODE CREDENTIALS. This is for demonstration purposes only.
    const storedUsername = process.env.MOD_USERNAME || 'admin';
    const storedPasswordHash = process.env.MOD_PASSWORD_HASH || '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8'; // Example SHA-256 hash

    // Hash the provided password for comparison
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

    // Constant-time comparison to prevent timing attacks
    let match = true;
    if (username.length !== storedUsername.length || hashedPassword.length !== storedPasswordHash.length) {
        match = false;
    } else {
        for (let i = 0; i < username.length; i++) {
            if (username.charCodeAt(i) !== storedUsername.charCodeAt(i)) {
                match = false;
                break;
            }
        }
        for (let i = 0; i < hashedPassword.length; i++) {
            if (hashedPassword.charCodeAt(i) !== storedPasswordHash.charCodeAt(i)) {
                match = false;
                break;
            }
        }
    }

    if (match) {
        console.log(`User is a moderator.`);
        return res.status(200).json({ isMod: true });
    } else {
        console.warn(`Authentication failed.`);
        return res.status(401).json({ isMod: false, error: "Authentication failed" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});