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
 * - All user inputs are validated and sanitized.
 * - No secrets or credentials are hardcoded.
 * - Passwords are compared using a constant-time algorithm to prevent timing attacks.
 * - Error messages are generic to avoid information leakage.
 * - Principle of least privilege: only checks mod status, no other actions.
 * - Assumes HTTPS is enforced at deployment (Express does not handle HTTPS directly).
 * - No insecure deserialization or unsafe APIs.
 * - No direct database access; replace with secure DB/API in production.
 */
const crypto = require("crypto");
const { body, validationResult } = require("express-validator");

// Use express.json() for parsing JSON bodies securely
app.use(express.json());

// Example: Secure, in-memory user store (replace with secure DB in production)
const users = [
  // Passwords are hashed with a strong algorithm and unique salt
  // Example user: { username: "moduser", passwordHash: "<hash>", salt: "<salt>", role: "moderator" }
];

// Secure password hashing function (PBKDF2)
function hashPassword(password, salt) {
  return crypto.pbkdf2Sync(password, salt, 310000, 32, "sha256").toString("hex");
}

// Secure constant-time comparison
function safeCompare(a, b) {
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}

// Secure input validation and sanitization middleware
const validateCheckMod = [
  body("username")
    .trim()
    .isLength({ min: 3, max: 32 })
    .matches(/^[a-zA-Z0-9_\-]+$/)
    .escape(),
  body("password")
    .isLength({ min: 8, max: 128 })
    .escape(),
];

app.post("/check_mod", validateCheckMod, (req, res) => {
  // Validate and sanitize input
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    // Do not leak details about validation errors
    return res.status(400).json({ success: false, message: "Invalid input." });
  }

  const { username, password } = req.body;

  // Principle of least privilege: only fetch necessary fields
  const user = users.find(u => u.username === username);

  // Generic error message to prevent user enumeration
  if (!user || !user.salt || !user.passwordHash) {
    return res.status(401).json({ success: false, message: "Authentication failed." });
  }

  // Hash the provided password with the user's salt
  const passwordHash = hashPassword(password, user.salt);

  // Constant-time comparison to prevent timing attacks
  if (!safeCompare(passwordHash, user.passwordHash)) {
    return res.status(401).json({ success: false, message: "Authentication failed." });
  }

  // Check if user is a moderator
  const isModerator = user.role === "moderator";

  // Do not leak role information if not authenticated
  if (!isModerator) {
    return res.status(403).json({ success: false, message: "Access denied." });
  }

  // Success: authenticated and authorized
  return res.status(200).json({ success: true, isModerator: true });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
