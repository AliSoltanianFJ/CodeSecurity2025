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
        // Do not reveal which field failed
        return res.status(400).send("Invalid credentials.");
    }

    // Principle of least privilege: restrict DB user permissions
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // Should be a user with only SELECT privileges
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Secure defaults
        connectionLimit: 5,
        ssl: { rejectUnauthorized: true }
    };

    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error internally, do not leak details to client
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Only select necessary fields
        const query = "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?";
        connection.query(query, [username], (err, results) => {
            connection.release();
            if (err) {
                console.error("DB query error:", err);
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
                310000, // OWASP recommends >=310,000 iterations
                32,
                'sha256',
                (err, derivedKey) => {
                    if (err) {
                        console.error("PBKDF2 error:", err);
                        return res.status(500).send("Internal server error.");
                    }

                    // Constant-time comparison to prevent timing attacks (CWE-203)
                    if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), derivedKey)) {
                        return res.status(401).send("Invalid credentials.");
                    }

                    // At this point, authentication is successful
                    // Issue a secure session token (do not use JWT without proper validation)
                    // For demonstration, set a secure, httpOnly cookie (in production, use HTTPS and proper session management)
                    // Do not expose user id or sensitive info in response

                    // Example: Set session cookie (in production, use a session store)
                    res.cookie('session', crypto.randomBytes(32).toString('hex'), {
                        httpOnly: true,
                        secure: true, // Only over HTTPS
                        sameSite: 'strict',
                        maxAge: 60 * 60 * 1000 // 1 hour
                    });

                    return res.status(200).send("Login successful.");
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
