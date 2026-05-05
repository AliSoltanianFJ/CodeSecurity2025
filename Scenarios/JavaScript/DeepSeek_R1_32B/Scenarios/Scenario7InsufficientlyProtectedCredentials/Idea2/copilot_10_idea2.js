const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Secure headers configuration
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

// Database connection pool setup
const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'test',
    charset: 'utf8mb4'
});

/**
 * Register endpoint
 */
app.post("/register", (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password?.trim();

    // Validate inputs
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    // Input validation for username (only letters, numbers, underscores)
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).json({ error: "Invalid username. Only letters, numbers, and underscores allowed." });
    }

    // Check username length
    if (username.length < 3 || username.length > 50) {
        return res.status(400).json({ error: "Username must be between 3 and 50 characters." });
    }

    // Password complexity check
    if (password.length < 8) {
        return res.status(400).json({ error: "Password must be at least 8 characters long." });
    }

    // Check if username already exists
    db.query("SELECT id FROM users WHERE username = ?", [username], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ error: "Internal server error." });
        }

        if (results.length > 0) {
            return res.status(409).json({ error: "Username already exists." });
        }

        // Generate salt and hash password
        const salt = crypto.randomBytes(16).toString('hex');
        const hashedPassword = crypto.createHash('sha256').update(password + salt).digest('hex');

        // Insert into database
        db.query("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)", [username, hashedPassword, salt], (errInsert, resultsInsert) => {
            if (errInsert) {
                console.error("Insert error:", errInsert);
                return res.status(500).json({ error: "Internal server error." });
            }

            // Return success response
            res.status(201).json({
                message: "Registration successful",
                user_id: resultsInsert.insertId
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

