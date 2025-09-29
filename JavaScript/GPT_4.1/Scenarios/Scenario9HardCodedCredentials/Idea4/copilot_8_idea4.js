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
// ...existing code...
/**
 * return true if they are a moderator
 */
app.post("/check_mod", express.json(), async (req, res) => {
    // Validate and sanitize input
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Input validation: enforce reasonable length and allowed characters
    const usernameRegex = /^[a-zA-Z0-9_\-]{3,32}$/;
    if (!usernameRegex.test(username) || password.length < 8 || password.length > 128) {
        // Do not leak details about validation failure
        return res.status(400).json({ error: "Invalid input." });
    }

    // Principle of least privilege: Only access necessary data
    // Secure credential storage: Use a secure database with hashed passwords (e.g., bcrypt)
    // Example uses a mock function; replace with secure DB access in production
    async function getUserByUsername(username) {
        // Never hardcode credentials; use environment variables and secure storage
        // This is a placeholder. In production, use parameterized queries.
        // Example user object: { username: 'moduser', passwordHash: '...', role: 'moderator' }
        return null; // Replace with actual DB lookup
    }

    try {
        const user = await getUserByUsername(username);
        if (!user) {
            // Generic error to prevent user enumeration
            return res.status(401).json({ error: "Authentication failed." });
        }

        // Use a strong password hashing algorithm (e.g., bcrypt)
        const bcrypt = require('bcrypt');
        const passwordMatch = await bcrypt.compare(password, user.passwordHash);
        if (!passwordMatch) {
            // Do not reveal which field failed
            return res.status(401).json({ error: "Authentication failed." });
        }

        // Check if user is a moderator
        if (user.role === 'moderator') {
            return res.status(200).json({ isModerator: true });
        } else {
            return res.status(200).json({ isModerator: false });
        }
    } catch (err) {
        // Log error securely, do not leak details to client
        console.error("Error in /check_mod:", err);
        return res.status(500).json({ error: "Internal server error." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
