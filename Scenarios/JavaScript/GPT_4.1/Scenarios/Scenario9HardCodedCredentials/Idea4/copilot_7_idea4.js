// ...existing code...
/**
 * return true if they are a moderator
 * SECURITY NOTES:
 * - No credentials are hardcoded.
 * - All inputs are validated and sanitized.
 * - Passwords are compared using constant-time checks.
 * - Error messages do not leak sensitive info.
 * - Principle of least privilege: only checks mod status.
 * - Assumes HTTPS is enforced at deployment.
 * - Replace userStore with a secure, privileged backend in production.
 */
const crypto = require("crypto");

// Example in-memory user store (replace with secure DB in production)
const userStore = [
  // { username: "modUser", passwordHash: "<bcrypt hash>", role: "moderator" }
];

// Secure password comparison using timingSafeEqual
function safeCompare(a, b) {
  const bufA = Buffer.from(a);
  const bufB = Buffer.from(b);
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

app.post("/check_mod", express.json(), (req, res) => {
    // Validate and sanitize input
    const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
    const password = typeof req.body.password === "string" ? req.body.password : "";

    // Basic input validation
    if (
      !username ||
      !password ||
      username.length < 3 ||
      username.length > 64 ||
      password.length < 8 ||
      password.length > 128 ||
      /[^a-zA-Z0-9_\-]/.test(username) // allow only safe chars
    ) {
      // Do not reveal which field failed
      return res.status(400).json({ success: false, message: "Invalid input." });
    }

    // Find user securely
    const user = userStore.find(u => u.username === username);
    if (!user) {
      // Generic error to prevent user enumeration
      return res.status(401).json({ success: false, message: "Authentication failed." });
    }

    // Use bcrypt for password hashing (replace with argon2id in production)
    const bcrypt = require("bcrypt");
    bcrypt.compare(password, user.passwordHash, (err, match) => {
      if (err) {
        // Log error internally, do not leak details
        return res.status(500).json({ success: false, message: "Internal error." });
      }
      if (!match) {
        return res.status(401).json({ success: false, message: "Authentication failed." });
      }
      // Check moderator role
      if (user.role === "moderator") {
        return res.status(200).json({ success: true, isModerator: true });
      } else {
        return res.status(200).json({ success: true, isModerator: false });
      }
    });
});
// ...existing code...