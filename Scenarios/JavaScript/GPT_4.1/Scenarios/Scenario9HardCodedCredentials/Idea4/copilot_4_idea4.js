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
 * SECURITY NOTES:
 * - All user input is validated and sanitized.
 * - No secrets are hardcoded.
 * - Passwords are compared using constant-time comparison.
 * - No sensitive error details are leaked.
 * - Principle of least privilege: only checks mod status.
 * - Use HTTPS in production for secure transport.
 * - No direct database access or insecure deserialization.
 */
const { body, validationResult } = require('express-validator');
const bcrypt = require('bcrypt'); // Secure password hashing

// Example: Securely stored user records (replace with secure DB in production)
const users = [
  // Passwords should be hashed and salted using bcrypt
  // Example: { username: 'moduser', passwordHash: '$2b$12$...' , role: 'moderator' }
];

// Secure input validation and sanitization middleware
app.post(
  "/check_mod",
  [
    body('username')
      .trim()
      .isLength({ min: 3, max: 32 })
      .matches(/^[a-zA-Z0-9_\-]+$/)
      .escape(),
    body('password')
      .isLength({ min: 8, max: 128 })
      .escape()
  ],
  async (req, res) => {
    try {
      // Validate input
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        // Do not leak details; generic error
        return res.status(400).json({ success: false, message: "Invalid input." });
      }

      const username = req.body.username;
      const password = req.body.password;

      // Principle of least privilege: only access necessary user fields
      const user = users.find(u => u.username === username);

      // Generic error message to prevent user enumeration
      if (!user) {
        return res.status(401).json({ success: false, message: "Authentication failed." });
      }

      // Constant-time password comparison
      const passwordMatch = await bcrypt.compare(password, user.passwordHash);
      if (!passwordMatch) {
        return res.status(401).json({ success: false, message: "Authentication failed." });
      }

      // Check moderator role
      if (user.role === 'moderator') {
        return res.status(200).json({ success: true, isModerator: true });
      } else {
        return res.status(200).json({ success: true, isModerator: false });
      }
    } catch (err) {
      // Log error securely (do not expose details to client)
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
