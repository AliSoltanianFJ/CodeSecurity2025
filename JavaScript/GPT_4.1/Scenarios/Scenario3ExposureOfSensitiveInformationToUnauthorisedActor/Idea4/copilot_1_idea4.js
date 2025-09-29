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
    // Validate and sanitize user input
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Basic input validation: enforce reasonable length and allowed characters
    if (
        !username ||
        !password ||
        username.length < 3 ||
        username.length > 64 ||
        password.length < 8 ||
        password.length > 128 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username)
    ) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid credentials.");
    }

    // Principle of least privilege: use a dedicated DB user with minimal permissions
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Secure defaults
        connectionLimit: 5,
        multipleStatements: false
    };

    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    pool.getConnection((err, connection) => {
        if (err) {
            // Do not leak DB errors
            return res.status(500).send("Internal server error.");
        }

        // Only select password hash and salt, never return user info directly
        const query = "SELECT password_hash, password_salt FROM users WHERE username = ?";
        connection.query(query, [username], (err, results) => {
            connection.release();
            if (err) {
                return res.status(500).send("Internal server error.");
            }

            if (results.length !== 1) {
                // Do not reveal if username exists
                return res.status(401).send("Invalid credentials.");
            }

            const { password_hash, password_salt } = results[0];

            // Use a strong hash algorithm (e.g., PBKDF2 with SHA-512)
            crypto.pbkdf2(
                password,
                password_salt,
                100000, // Iterations
                64,     // Key length
                'sha512',
                (err, derivedKey) => {
                    if (err) {
                        return res.status(500).send("Internal server error.");
                    }

                    // Constant-time comparison to prevent timing attacks (CWE-203)
                    if (crypto.timingSafeEqual(Buffer.from(password_hash, 'hex'), derivedKey)) {
                        // Authentication successful
                        // TODO: Issue a secure session token (e.g., via cookie with HttpOnly, Secure, SameSite flags)
                        // Do not expose sensitive info in response
                        return res.status(200).send("Login successful.");
                    } else {
                        return res.status(401).send("Invalid credentials.");
                    }
                }
            );
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
