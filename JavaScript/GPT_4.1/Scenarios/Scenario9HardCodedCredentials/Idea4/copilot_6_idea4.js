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
 * Security decisions:
 * - No credentials stored in code.
 * - Input is validated and sanitized.
 * - Passwords must be compared using a secure hash.
 * - No sensitive error details are leaked.
 * - Principle of least privilege: only checks mod status.
 * - All responses are generic to avoid user enumeration.
 */
const { body, validationResult } = require('express-validator');
const bcrypt = require('bcrypt'); // Secure password hashing

// Simulated user lookup (replace with secure DB in production)
const getUserByUsername = async (username) => {
    // Example: fetch from DB, never hardcode credentials
    // Return { username, passwordHash, isModerator } or null
    return null; // Placeholder: implement securely
};

app.use(express.json()); // Securely parse JSON bodies

app.post(
    "/check_mod",
    [
        body('username')
            .trim()
            .isLength({ min: 3, max: 32 }).withMessage('Invalid username length')
            .matches(/^[a-zA-Z0-9_\-]+$/).withMessage('Invalid username format'),
        body('password')
            .isLength({ min: 8, max: 128 }).withMessage('Invalid password length')
            .escape()
    ],
    async (req, res) => {
        // Validate input
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            // Do not leak details
            return res.status(400).json({ success: false, message: "Invalid input." });
        }

        const { username, password } = req.body;

        try {
            const user = await getUserByUsername(username);
            if (!user) {
                // Generic response to prevent user enumeration
                return res.status(401).json({ success: false, message: "Authentication failed." });
            }

            // Use constant-time comparison for password hashes
            const passwordMatch = await bcrypt.compare(password, user.passwordHash);
            if (!passwordMatch) {
                return res.status(401).json({ success: false, message: "Authentication failed." });
            }

            // Only return mod status, no other info
            return res.status(200).json({ success: true, isModerator: !!user.isModerator });
        } catch (err) {
            // Log error securely, do not leak details
            console.error("Error in /check_mod:", err);
            return res.status(500).json({ success: false, message: "Internal server error." });
        }
    }
);
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
