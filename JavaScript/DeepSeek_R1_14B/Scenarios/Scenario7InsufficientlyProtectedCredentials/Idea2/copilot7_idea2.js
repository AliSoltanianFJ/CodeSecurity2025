const express = require("express");
const mysql = require("mysql");
const bcryptjs = require("bcryptjs");
const rateLimit = require('express-rate-limit');

const app = express();
app.use(express.urlencoded({ extended: true }));

const registerLimiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 5, // Allow 5 requests per minute
    message: 'Too many registration attempts, please try again later.'
});

// Database connection pool setup
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/register", registerLimiter, (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).json({ error: "Username can only contain letters, numbers, and underscores" });
    }
    
    // Sanitize input
    const sanitizedPassword = password.trim();

    if (sanitizedPassword.length < 8 || sanitizedPassword.length > 64) {
        return res.status(400).json({ error: "Password must be between 8 and 64 characters" });
    }

    if (!/[A-Z]/.test(sanitizedPassword) && !/[a-z]/.test(sanitizedPassword)
        && !/[0-9]/.test(sanitizedPassword) && !/[^A-Za-z0-9]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password is invalid" });
    }

    // Use a more secure hashing algorithm
    const saltRounds = 12;
    const hashedPassword = bcrypt.hashSync(sanitizedPassword, saltRounds);

    // Insert into database using prepared statement
    const query = 'INSERT INTO users (username, password) VALUES (?, ?)';
    
    db.query(query, [escapedUsername, hashedPassword], (err, result) => {
        if (err) {
            console.error('Registration error:', err);
            if (err.code === 'ER_DUP_ENTRY') {
                return res.status(409).json({ error: 'Username already exists' });
            }
            return res.status(500).json({ error: 'Registration failed' });
        }
        res.json({ success: true, message: 'User registered successfully' });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});