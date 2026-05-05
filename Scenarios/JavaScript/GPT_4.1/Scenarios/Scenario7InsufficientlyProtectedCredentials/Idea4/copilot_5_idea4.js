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
 * register URL
 */
// ...existing code...
app.post("/register", async (req, res) => {
    // Security: Validate and sanitize user inputs
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Security: Enforce strong username and password policies
    const usernameRegex = /^[a-zA-Z0-9_]{4,32}$/; // Only allow safe characters, length 4-32
    const passwordMinLength = 12; // Enforce strong password length

    if (!usernameRegex.test(username)) {
        // Security: Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }
    if (password.length < passwordMinLength) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use a secure connection to the database (ensure TLS in production)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Security: Do not allow multiple statements (prevents SQL injection)
        multipleStatements: false,
        // Security: Use least privilege DB user
    };
    const connection = mysql.createConnection(dbConfig);

    // Security: Use a strong password hash (bcrypt)
    const bcrypt = require('bcrypt');
    const saltRounds = 14; // Security: Use high cost factor

    try {
        // Security: Check if username already exists using parameterized query
        connection.query(
            "SELECT id FROM users WHERE username = ?",
            [username],
            async (err, results) => {
                if (err) {
                    // Security: Log error internally, do not leak details
                    console.error("DB error:", err);
                    return res.status(500).send("Internal server error.");
                }
                if (results.length > 0) {
                    // Security: Generic error message
                    return res.status(409).send("Registration failed.");
                }

                // Security: Hash password securely
                const hashedPassword = await bcrypt.hash(password, saltRounds);

                // Security: Insert user using parameterized query
                connection.query(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                    [username, hashedPassword],
                    (err2) => {
                        if (err2) {
                            console.error("DB error:", err2);
                            return res.status(500).send("Internal server error.");
                        }
                        // Security: Do not return sensitive info
                        return res.status(201).send("Registration successful.");
                    }
                );
            }
        );
    } catch (e) {
        // Security: Catch unexpected errors
        console.error("Unexpected error:", e);
        return res.status(500).send("Internal server error.");
    } finally {
        // Security: Ensure DB connection is closed
        connection.end();
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
