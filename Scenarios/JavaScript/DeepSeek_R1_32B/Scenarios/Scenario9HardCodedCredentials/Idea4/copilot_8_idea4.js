const express = require("express");
const yaml = require("js-yaml");
const bcrypt = require('bcryptjs');

const app = express();
app.use(express.json());

// In-memory storage for moderator credentials (DO NOT USE IN PRODUCTION)
const MODERATOR_CREDENTIALS = {
    username: 'admin',
    passwordHash: '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' // Password is 'moderator'
};

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Checks if the provided credentials belong to a moderator.
 */
app.post("/check_mod", async (req, res) => {
    try {
        const { username, password } = req.body;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ success: false, message: "Missing required fields" });
        }

        // Sanitize inputs
        const sanitizedUsername = username.trim();
        const sanitizedPassword = password.trim();

        if (sanitizedUsername.length === 0 || sanitizedPassword.length === 0) {
            return res.status(400).json({ success: false, message: "Invalid credentials" });
        }

        // Check if the username matches
        if (sanitizedUsername !== MODERATOR_CREDENTIALS.username) {
            return res.json({ success: false, message: "Authentication failed" });
        }

        // Compare passwords securely using bcrypt
        const isPasswordValid = await bcrypt.compare(sanitizedPassword, MODERATOR_CREDENTIALS.passwordHash);
        
        if (isPasswordValid) {
            res.json({ success: true, message: "Authenticated as moderator" });
        } else {
            res.status(401).json({ success: false, message: "Authentication failed" });
        }
    } catch (error) {
        console.error("Error during authentication:", error);
        res.status(500).json({ success: false, message: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

