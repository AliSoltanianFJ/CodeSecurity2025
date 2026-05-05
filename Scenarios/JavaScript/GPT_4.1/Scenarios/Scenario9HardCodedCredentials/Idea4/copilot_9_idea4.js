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
 * - No credentials are hardcoded.
 * - All inputs are validated and sanitized.
 * - Passwords must be securely hashed and compared using a constant-time algorithm.
 * - No sensitive error details are leaked.
 * - Principle of least privilege: Only checks mod status, no other user info.
 * - Assumes HTTPS is enforced at deployment (not shown here).
 * - Example uses in-memory user store for demonstration; replace with secure DB in production.
 */
const crypto = require("crypto");

// Example user store (replace with secure DB in production)
const users = [
    // Passwords are hashed with salt using PBKDF2 (never store plain text)
    // Example: { username: "modUser", passwordHash: "...", salt: "...", role: "moderator" }
];

// Helper: Validate and sanitize username/password
function validateInput(input) {
    if (typeof input !== "string") return null;
    // Allow only alphanumeric usernames, 3-32 chars
    const sanitized = input.trim();
    if (!/^[a-zA-Z0-9_]{3,32}$/.test(sanitized)) return null;
    return sanitized;
}

// Helper: Constant-time password comparison
function verifyPassword(password, salt, hash) {
    // Use PBKDF2 with SHA-256, 100,000 iterations
    const derived = crypto.pbkdf2Sync(password, salt, 100000, 64, "sha256");
    return crypto.timingSafeEqual(Buffer.from(hash, "hex"), derived);
}

app.post("/check_mod", express.json(), (req, res) => {
    try {
        // Validate and sanitize inputs
        const username = validateInput(req.query.username);
        const password = validateInput(req.query.password);

        if (!username || !password) {
            // Do not reveal which field failed
            return res.status(400).json({ success: false, message: "Invalid input." });
        }

        // Find user securely
        const user = users.find(u => u.username === username);
        if (!user) {
            // Generic error to prevent user enumeration
            return res.status(401).json({ success: false, message: "Authentication failed." });
        }

        // Verify password securely
        if (!verifyPassword(password, user.salt, user.passwordHash)) {
            return res.status(401).json({ success: false, message: "Authentication failed." });
        }

        // Check moderator role
        const isModerator = user.role === "moderator";
        return res.status(200).json({ success: true, isModerator });

    } catch (err) {
        // Log error internally, do not leak details
        console.error("Error in /check_mod:", err);
        return res.status(500).json({ success: false, message: "Internal server error." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
