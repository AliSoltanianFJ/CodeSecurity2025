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
app.post("/login", async (req, res) => {
    // Validate and sanitize input
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Basic input validation: enforce reasonable length and allowed characters
    const usernameRegex = /^[a-zA-Z0-9_.-]{3,32}$/;
    if (!usernameRegex.test(username) || password.length < 8 || password.length > 128) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid credentials.");
    }

    // Principle of least privilege: use a dedicated DB user with minimal permissions
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // Should be set via environment variables
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Secure defaults
        connectionLimit: 5,
        ssl: { rejectUnauthorized: true } // Enforce SSL/TLS for DB connection
    };
    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    pool.query(
        "SELECT id, username, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, results) => {
            if (err) {
                // Log error internally, do not leak details to client
                console.error("DB error:", err);
                return res.status(500).send("Internal server error.");
            }
            if (results.length === 0) {
                // Do not reveal whether username exists
                return res.status(401).send("Invalid credentials.");
            }

            const user = results[0];
            // Secure password verification using PBKDF2 (avoid insecure hashes like MD5/SHA1)
            crypto.pbkdf2(
                password,
                user.password_salt,
                310000, // Iterations (OWASP recommends >=310,000 for PBKDF2)
                32,     // Key length
                'sha256',
                (err, derivedKey) => {
                    if (err) {
                        console.error("Crypto error:", err);
                        return res.status(500).send("Internal server error.");
                    }
                    // Constant-time comparison to prevent timing attacks (CWE-203)
                    if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), derivedKey)) {
                        return res.status(401).send("Invalid credentials.");
                    }

                    // At this point, authentication is successful
                    // Do NOT expose sensitive info in response
                    // TODO: Issue a secure session token (e.g., JWT with proper signing, or session cookie with httpOnly, secure, SameSite=strict)
                    // For demonstration, just send a generic success message
                    return res.status(200).send("Login successful.");
                }
            );
        }
    );
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
