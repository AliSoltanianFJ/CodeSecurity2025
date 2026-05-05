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
        !/^[A-Za-z0-9_.-]+$/.test(username)
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
        port: 3306,
        ssl: { rejectUnauthorized: true }
    };

    // Never hardcode secrets; use environment variables
    const connection = mysql.createConnection(dbConfig);

    // Use parameterized queries to prevent SQL injection (CWE-89)
    const query = "SELECT id, username, password_hash, password_salt FROM users WHERE username = ?";
    connection.query(query, [username], (err, results) => {
        if (err) {
            // Log error internally, do not leak details to client
            console.error("DB error:", err);
            return res.status(500).send("Internal server error.");
        }

        if (results.length !== 1) {
            // Do not reveal whether username exists
            return res.status(401).send("Invalid credentials.");
        }

        const user = results[0];

        // Use a strong password hashing algorithm (e.g., PBKDF2, bcrypt, argon2)
        // Here, using PBKDF2 via crypto (Node.js built-in)
        const hash = crypto.pbkdf2Sync(
            password,
            user.password_salt,
            310000, // OWASP recommends >=310,000 iterations
            32,
            'sha256'
        ).toString('hex');

        if (crypto.timingSafeEqual(Buffer.from(hash, 'hex'), Buffer.from(user.password_hash, 'hex'))) {
            // Authentication successful
            // Issue a secure session token (do not use JWT without proper validation)
            // For demo, set a secure cookie (in production, use HTTPS and secure session management)
            // Do not expose user details in response
            res.cookie('session', crypto.randomBytes(32).toString('hex'), {
                httpOnly: true,
                secure: true, // Ensure HTTPS
                sameSite: 'strict',
                maxAge: 60 * 60 * 1000 // 1 hour
            });
            return res.status(200).send("Login successful.");
        } else {
            // Do not reveal whether password or username was incorrect
            return res.status(401).send("Invalid credentials.");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
