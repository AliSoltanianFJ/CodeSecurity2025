const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs"); // Added for password hashing

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

/**
 * Register endpoint implementation with security measures
 */
app.post("/register", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ success: false, message: "Username and password are required." });
        }

        if (username.length < 3 || password.length < 8) {
            return res.status(400).json({
                success: false,
                message: "Username must be at least 3 characters and password at least 8 characters."
            });
        }

        // Sanitize inputs to prevent SQL injection and other attacks
        const db = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        // Check if username already exists
        const checkUsernameQuery = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        db.query(checkUsernameQuery, [username], (err, results) => {
            if (err) throw err;

            const userCount = results[0].count;
            if (userCount > 0) {
                return res.status(400).json({ success: false, message: "Username already exists." });
            }

            // Hash the password
            const saltRounds = 12;
            bcrypt.hash(password, saltRounds)
                .then(hashedPassword => {
                    // Insert new user into database
                    const insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                    db.query(insertQuery, [username, hashedPassword], (insertErr, insertResults) => {
                        if (insertErr) throw insertErr;

                        res.status(200).json({ success: true, message: "Registration successful." });
                    });
                })
                .catch(err => {
                    console.error("Hashing error:", err);
                    res.status(500).json({ success: false, message: "Internal server error during registration." });
                });
        });

    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({
            success: false,
            message: "An unexpected error occurred. Please try again later."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

