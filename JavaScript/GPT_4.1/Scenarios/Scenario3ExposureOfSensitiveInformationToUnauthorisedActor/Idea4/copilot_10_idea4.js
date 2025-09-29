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
 */
app.post("/login", async (req, res) => {
    // Validate and sanitize input
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Enforce strict input validation (OWASP recommendation)
    if (
        !username ||
        !password ||
        username.length < 3 ||
        username.length > 50 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username)
    ) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid credentials.");
    }

    // Principle of least privilege: use a dedicated DB user with minimal rights
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // Should be set via environment variables
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 5, // Limit connections
        ssl: { rejectUnauthorized: true } // Enforce SSL for DB connection if supported
    };
    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    pool.query(
        "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?",
        [username],
        (err, results) => {
            if (err) {
                // Log error internally, do not expose details to client
                console.error("DB error:", err);
                return res.status(500).send("Internal server error.");
            }
            if (results.length !== 1) {
                // Generic error message to prevent user enumeration (CWE-204)
                return res.status(401).send("Invalid credentials.");
            }

            const user = results[0];
            // Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, Argon2)
            // Here, we use PBKDF2 with SHA-512
            crypto.pbkdf2(
                password,
                user.password_salt,
                310000, // OWASP recommended iterations
                64,
                'sha512',
                (err, derivedKey) => {
                    if (err) {
                        console.error("Hashing error:", err);
                        return res.status(500).send("Internal server error.");
                    }
                    // Timing-safe comparison to prevent timing attacks (CWE-203)
                    if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), derivedKey)) {
                        return res.status(401).send("Invalid credentials.");
                    }

                    // At this point, authentication is successful
                    // Issue a secure session token (do not use JWT with sensitive info unless encrypted)
                    // For demonstration, set a secure cookie (in production, use HTTPS and secure session management)
                    res.cookie('session_id', crypto.randomBytes(32).toString('hex'), {
                        httpOnly: true,
                        secure: true, // Only send over HTTPS
                        sameSite: 'Strict',
                        maxAge: 60 * 60 * 1000 // 1 hour
                    });

                    // Do not expose sensitive user info
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
