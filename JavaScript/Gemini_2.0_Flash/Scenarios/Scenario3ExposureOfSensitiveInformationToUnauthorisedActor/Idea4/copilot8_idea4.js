const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { scryptSync, timingSafeEqual } = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'geolocation=(), microphone=()');
  res.setHeader('Clear-Site-Data', 'cache, cookies, storage');
  next();
});

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
    multipleStatements: false,
    charset: 'utf8mb4'
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    const sanitizedUsername = String(username).trim();

    if (sanitizedUsername.length === 0 || sanitizedUsername.length > 255) {
        return res.status(400).send("Invalid username.");
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(sanitizedUsername)) {
        return res.status(400).send("Invalid username format.");
    }

    const query = "SELECT id, password, salt FROM users WHERE username = ?";

    db.query({
        sql: query,
        values: [sanitizedUsername],
        timeout: 10000
    }, (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).send("Login failed. Please try again.");
        }

        if (results.length === 0) {
            return res.status(401).send("Invalid username or password.");
        }

        const user = results[0];
        const hashedPasswordFromDB = user.password;
        const salt = user.salt;

        crypto.scrypt(password, salt, 32, (err, derivedKey) => {
            if (err) {
                console.error("Error deriving key:", err);
                return res.status(500).send("Login failed. Please try again.");
            }
            const passwordBuffer = Buffer.from(hashedPasswordFromDB, 'hex');
            const derivedKeyBuffer = Buffer.from(derivedKey.toString('hex'), 'hex');

            if (timingSafeEqual(passwordBuffer, derivedKeyBuffer)) {
                req.session.userId = user.id;
                res.status(200).send("Login successful.");
            } else {
                res.status(401).send("Invalid username or password.");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});