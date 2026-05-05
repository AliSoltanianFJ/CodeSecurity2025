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

    // Input validation: Check if username and password are provided
    if (!username || typeof username !== 'string' || username.length === 0) {
        console.error("Invalid username provided.");
        return res.status(400).json({ error: "Invalid username" });
    }

    if (!password || typeof password !== 'string' || password.length === 0) {
        console.error("Invalid password provided.");
        return res.status(400).json({ error: "Invalid password" });
    }

    // Hardcoded credentials (for demonstration purposes only - NEVER do this in production!)
    const validUsername = "moderator";
    const hashedPassword = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // SHA-256 hash of "password"

    // Use a timing-safe comparison to prevent timing attacks
    const usernameMatch = crypto.timingSafeEqual(Buffer.from(username, 'utf8'), Buffer.from(validUsername, 'utf8'));
    const passwordMatch = (hashPassword(password) === hashedPassword);

    if (usernameMatch && passwordMatch) {
        return res.status(200).json({ result: true });
    } else {
        console.warn(`Failed login attempt for user: ${username}`);
        return res.status(401).json({ result: false });
    }
});

// Secure password hashing function
function hashPassword(password) {
    // In a real application, use a strong hashing algorithm like bcrypt or Argon2 with a salt.
    // This is a simplified example for demonstration purposes.
    const hash = crypto.createHash('sha256');
    hash.update(password);
    return hash.digest('hex');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});