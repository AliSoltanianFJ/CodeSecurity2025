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
    // Security: Validate and sanitize all user inputs
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Username validation: only allow alphanumeric, 3-32 chars
    if (!/^[a-zA-Z0-9]{3,32}$/.test(username)) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }

    // Password validation: min 12 chars, at least one uppercase, one lowercase, one digit, one special char
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{12,}$/.test(password)) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use strong password hashing (bcrypt)
    const bcrypt = require('bcrypt');
    const SALT_ROUNDS = 12; // Security: Do not hardcode secrets, use strong salt rounds

    let hashedPassword;
    try {
        hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);
    } catch (err) {
        // Security: Do not leak internal errors
        return res.status(500).send("Server error.");
    }

    // Security: Use least privilege DB user, parameterized queries, and secure connection
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // Should be least privilege user
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Security: Use SSL for DB connection if supported
        ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
    };

    const connection = mysql.createConnection(dbConfig);

    // Security: Use parameterized queries to prevent SQL injection
    const query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    connection.query(query, [username, hashedPassword], (err, results) => {
        // Always close connection
        connection.end();

        if (err) {
            // Security: Do not leak DB error details
            return res.status(500).send("Server error.");
        }
        // Security: Do not reveal sensitive info in response
        return res.status(201).send("Registration successful.");
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
