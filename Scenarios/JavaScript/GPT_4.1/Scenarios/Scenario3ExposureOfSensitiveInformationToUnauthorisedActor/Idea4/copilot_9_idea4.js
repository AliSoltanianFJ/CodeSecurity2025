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
    // Input validation and sanitization
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Validate username and password format
    if (
        !username ||
        !password ||
        username.length < 3 ||
        username.length > 50 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username) // allow only safe characters
    ) {
        // Do not reveal which field failed
        return res.status(400).send("Invalid credentials.");
    }

    // Principle of least privilege: use a dedicated DB user with minimal rights
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // should be set via environment variables
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 5, // limit connections
        ssl: { rejectUnauthorized: true } // require SSL for DB connection
    };

    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    pool.getConnection((err, connection) => {
        if (err) {
            // Log generic error, do not leak details
            return res.status(500).send("Internal server error.");
        }

        // Query for user by username
        const query = "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?";
        connection.query(query, [username], (err, results) => {
            connection.release();
            if (err) {
                return res.status(500).send("Internal server error.");
            }

            if (results.length !== 1) {
                // Do not reveal if username exists
                return res.status(401).send("Invalid credentials.");
            }

            const user = results[0];

            // Secure password verification using PBKDF2 (avoid insecure hashes like MD5/SHA1)
            crypto.pbkdf2(
                password,
                user.password_salt,
                310000, // recommended iterations
                32,     // key length
                'sha256', // strong hash algorithm
                (err, derivedKey) => {
                    if (err) {
                        return res.status(500).send("Internal server error.");
                    }
                    if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), derivedKey)) {
                        // Use timingSafeEqual to prevent timing attacks (CWE-203)
                        return res.status(401).send("Invalid credentials.");
                    }

                    // At this point, authentication is successful
                    // Do not expose sensitive info in response

                    // TODO: Issue a secure session token (e.g., JWT with secure claims, or session cookie with httpOnly, secure, SameSite=strict)
                    // For demonstration, send a generic success message
                    res.status(200).send("Login successful.");
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
