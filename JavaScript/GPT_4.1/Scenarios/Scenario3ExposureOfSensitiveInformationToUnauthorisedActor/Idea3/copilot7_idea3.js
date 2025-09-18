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

const rateLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10, // limit each IP to 10 requests per windowMs
    standardHeaders: true,
    legacyHeaders: false,
    message: 'Too many login attempts. Please try again later.'
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Rate limiting (prevent brute force attacks)
    // Implement rateLimiter middleware here (see below for example)

    // Use environment variables for DB credentials
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: 'utf8mb4', // Prevent Unicode injection
        ssl: { rejectUnauthorized: true }
    });

    // Sanitize username input (avoid SQL Injection)
    db.connect();
    db.query(
        "SELECT id, password_hash, salt FROM users WHERE username = ?",
        [username.trim()],
        (err, results) => {
            if (err) {
                // Log error, do not expose details to user
                console.error("Login error:", err);
                res.status(500).send("Server error");
                return;
            }
            if (results.length !== 1) {
                // Prevent username enumeration
                res.status(401).send("Invalid credentials");
                return;
            }

            const { id, password_hash, salt } = results[0];

            // Hash provided password with stored salt (use bcrypt or scrypt in production)
            const hash = crypto
                .pbkdf2Sync(password, salt, 100000, 64, 'sha512')
                .toString('hex');

            if (hash !== password_hash) {
                // Do not say which is incorrect
                res.status(401).send("Invalid credentials");
                return;
            }

            // On success, provide a secure session token (do not expose sensitive data)
            const sessionToken = crypto.randomBytes(64).toString('hex');

            res.cookie('session_token', sessionToken, {
                httpOnly: true,
                secure: true,
                sameSite: 'Strict'
            });

            res.status(200).send("Login successful");
        }
    );

    db.end();
});

app.use(rateLimiter); // Apply globally or on /login only

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
