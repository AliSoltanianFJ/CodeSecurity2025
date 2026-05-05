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

    // Username: allow only alphanumeric, 3-32 chars
    if (!/^[a-zA-Z0-9]{3,32}$/.test(username)) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid credentials.");
    }
    // Password: minimum length check (do not reveal requirements)
    if (password.length < 8 || password.length > 128) {
        return res.status(400).send("Invalid credentials.");
    }

    // Use environment variables for DB credentials (never hardcode secrets)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Principle of least privilege: restrict user permissions in DB
        multipleStatements: false,
        ssl: { rejectUnauthorized: true } // Use SSL for DB connection if supported
    };

    const connection = mysql.createConnection(dbConfig);

    // Use parameterized query to prevent SQL injection (CWE-89)
    const query = "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?";
    connection.query(query, [username], (err, results) => {
        // Proper error handling: do not leak internal errors
        if (err) {
            // Log error securely (not shown here), do not expose details to user
            return res.status(500).send("An error occurred.");
        }
        if (results.length !== 1) {
            // Do not reveal whether username exists
            return res.status(401).send("Invalid credentials.");
        }

        const user = results[0];
        // Use a strong password hash algorithm (e.g., PBKDF2, bcrypt, argon2)
        // Here, assuming password_hash and password_salt are stored securely
        crypto.pbkdf2(password, user.password_salt, 310000, 32, 'sha256', (err, hashedPassword) => {
            if (err) {
                return res.status(500).send("An error occurred.");
            }
            // Use constant-time comparison to prevent timing attacks (CWE-203)
            if (!crypto.timingSafeEqual(Buffer.from(user.password_hash, 'hex'), hashedPassword)) {
                return res.status(401).send("Invalid credentials.");
            }

            // At this point, authentication is successful
            // Issue a secure session token (do not use JWT with sensitive data unless signed/encrypted)
            // Here, set a secure, httpOnly cookie as session identifier (session management not shown)
            // Always use HTTPS in production (enforce via config/proxy)
            res.cookie('session_id', crypto.randomBytes(32).toString('hex'), {
                httpOnly: true,
                secure: true, // Only send over HTTPS
                sameSite: 'Strict',
                maxAge: 60 * 60 * 1000 // 1 hour
            });
            return res.status(200).send("Login successful.");
        });
    });

    // Ensure connection is closed after query
    connection.on('error', () => { /* handle error silently */ });
    connection.end();
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
