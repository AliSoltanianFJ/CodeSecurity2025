const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs"); // Import bcrypt for password hashing
const helmet = require("helmet");   // For security headers

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(helmet());  // Apply security headers

// Database connection configuration using environment variables
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    insecureAuth: false  // Prevents use of passwords without encryption
});

// Rate limiting middleware for registration endpoint
const rateLimit = require("express-rate-limit");
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use("/register", limiter);

/**
 * Register endpoint with security measures
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation and sanitization
    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required." });
    }

    // Sanitize username to allow only alphanumeric, underscore, and period
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9._]+/g, '');
    if (sanitizedUsername.length < 1) {
        return res.status(400).json({ success: false, message: "Invalid username format." });
    }

    // Password complexity check
    if (password.length < 8) {
        return res.status(400).json({ success: false, message: "Password must be at least 8 characters long." });
    }

    // Hash the password with bcrypt using a salt of 12 rounds
    bcrypt.hash(password, 12)
        .then(hashedPassword => {
            // Check if username already exists
            return pool.query("SELECT username FROM users WHERE username = ?", [sanitizedUsername]);
        })
        .then(result => {
            if (result.length > 0) {
                return res.status(409).json({ success: false, message: "Username already exists." });
            }

            // Insert new user into the database
            const query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            pool.query(query, [sanitizedUsername, hashedPassword], (error, results) => {
                if (error) {
                    console.error("Registration error:", error);
                    return res.status(500).json({ success: false, message: "An error occurred during registration." });
                }
                // Consider setting a session cookie here with secure flags
                res.status(201).json({ success: true, message: "Registration successful. Please log in." });
            });
        })
        .catch(error => {
            console.error("Error:", error);
            res.status(500).json({ success: false, message: "An internal server error occurred." });
        });
});

// Other middleware and routes here

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

