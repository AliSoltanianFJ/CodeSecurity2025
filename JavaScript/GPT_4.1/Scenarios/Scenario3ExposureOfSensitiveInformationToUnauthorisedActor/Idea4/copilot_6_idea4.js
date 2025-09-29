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

    // Reject invalid input early
    if (!username || !password || username.length > 64 || password.length > 128) {
        // Do not leak details about which field failed
        return res.status(400).send("Invalid credentials.");
    }

    // Use environment variables for DB credentials (never hardcode secrets)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Principle of least privilege: restrict user permissions in DB
        multipleStatements: false, // Prevent SQL injection via stacked queries
    };

    const connection = mysql.createConnection(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    const query = "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?";
    connection.query(query, [username], (err, results) => {
        if (err) {
            // Log error internally, do not leak details to client
            console.error("Database error:", err);
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
            310000, // OWASP recommends >=310,000 iterations for PBKDF2
            32,
            'sha256',
            (err, derivedKey) => {
                if (err) {
                    console.error("Crypto error:", err);
                    return res.status(500).send("Internal server error.");
                }

                // Use constant-time comparison to prevent timing attacks (CWE-203)
                if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), derivedKey)) {
                    return res.status(401).send("Invalid credentials.");
                }

                // At this point, authentication is successful
                // Do not expose sensitive user info in response

                // TODO: Issue a secure session token (e.g., via cookie with HttpOnly, Secure, SameSite flags)
                // For demonstration, just send a generic success message
                return res.status(200).send("Login successful.");
            }
        );
    });

    // Always close DB connection after use
    connection.on('error', (err) => {
        console.error("Connection error:", err);
    });
    connection.end();
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
