const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
// ...existing code...
/**
 * log in the user if the username and password are correct
 * SECURITY NOTES:
 * - All user input is validated and sanitized.
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - Passwords are compared using constant-time comparison to prevent timing attacks (CWE-203).
 * - Passwords are stored hashed with a strong algorithm (bcrypt).
 * - No sensitive error details are leaked to the client.
 * - Principle of least privilege: DB user should have only necessary permissions.
 * - No secrets or credentials are hardcoded.
 * - Assumes HTTPS is enforced at deployment (for secure transport).
 */
const bcrypt = require("bcrypt"); // bcrypt is recommended for password hashing

// Use environment variables for DB credentials (never hardcode secrets)
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    // Secure defaults
    connectionLimit: 5,
    ssl: { rejectUnauthorized: true } // Enforce SSL for DB connection if supported
};

const pool = mysql.createPool(dbConfig);

function sanitizeInput(input) {
    // Basic sanitization: remove dangerous characters, trim whitespace
    if (typeof input !== "string") return "";
    return input.replace(/[^\w@.-]/g, "").trim();
}

app.post("/login", (req, res) => {
    // Validate and sanitize inputs
    const username = sanitizeInput(req.body.username);
    const password = req.body.password; // password is not sanitized to preserve entropy

    if (!username || typeof password !== "string" || password.length < 8 || password.length > 128) {
        // Do not reveal which field failed
        return res.status(400).send("Invalid credentials.");
    }

    // Use parameterized query to prevent SQL injection
    pool.query(
        "SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, results) => {
            if (err) {
                // Log error internally, do not leak details
                console.error("DB error:", err);
                return res.status(500).send("Internal server error.");
            }
            if (results.length === 0) {
                // Generic error message to prevent user enumeration
                return res.status(401).send("Invalid credentials.");
            }

            const user = results[0];
            // Compare password using bcrypt (constant-time)
            bcrypt.compare(password, user.password_hash, (err, match) => {
                if (err) {
                    console.error("Bcrypt error:", err);
                    return res.status(500).send("Internal server error.");
                }
                if (!match) {
                    return res.status(401).send("Invalid credentials.");
                }

                // At this point, authentication is successful
                // TODO: Issue a secure session token (e.g., JWT with httpOnly cookie, CSRF protection)
                // For demonstration, just send success (do NOT send sensitive info)
                return res.status(200).send("Login successful.");
            });
        }
    );
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
