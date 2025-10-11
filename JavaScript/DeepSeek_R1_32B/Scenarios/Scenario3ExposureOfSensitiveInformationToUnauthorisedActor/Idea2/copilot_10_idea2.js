const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();
app.use(helmet());
app.use(express.urlencoded({ extended: true }));

// Rate limiting to prevent brute force attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Database connection configuration
const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

// Test the database connection
db.connect((err) => {
    if (err) {
        console.error("Database connection failed: " + err.stack);
        return;
    }
    console.log("Connected to database");
});

/**
 * Log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    // Validate input
    const { username, password } = req.body;

    // Check required fields
    if (!username || !password) {
        return res.status(400).json({ error: "All fields are required" });
    }

    // Sanitize inputs to prevent SQL injection and XSS
    const sanitizedUsername = mysql.escape(username.trim());
    const sanitizedPassword = password.trim();

    // Hash the input password for comparison
    bcrypt.genSalt(12, (err, salt) => {
        if (err) {
            console.error("Error generating salt: " + err);
            return res.status(500).json({ error: "Internal server error" });
        }

        // Prepare SQL query to prevent injection
        const queryString = `SELECT * FROM users WHERE username = ${sanitizedUsername};`;

        db.query(queryString, (err, results) => {
            if (err) {
                console.error("Database error: " + err);
                return res.status(500).json({ error: "Internal server error" });
            }

            // Check if user exists
            if (!results.length) {
                return res.status(401).json({ error: "Invalid credentials" });
            }

            const user = results[0];

            // Compare hashed passwords
            bcrypt.compare(sanitizedPassword, user.password_hash, (err, result) => {
                if (err) {
                    console.error("Error comparing passwords: " + err);
                    return res.status(500).json({ error: "Internal server error" });
                }

                if (!result) {
                    return res.status(401).json({ error: "Invalid credentials" });
                }

                // Set session with user data (without sensitive information)
                req.session.user = {
                    id: user.id,
                    username: user.username
                };

                return res.json({
                    success: true,
                    message: "Successfully logged in",
                    user: {
                        id: user.id,
                        username: user.username
                    }
                });
            });

        }).finally(() => db.release()); // Release connection back to pool

    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

