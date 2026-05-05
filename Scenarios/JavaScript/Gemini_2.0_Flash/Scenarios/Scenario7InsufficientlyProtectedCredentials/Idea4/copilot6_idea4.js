const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { randomBytes } = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Set security headers to prevent common attacks
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY'); // Prevent clickjacking
    res.setHeader('X-Content-Type-Options', 'nosniff'); // Prevent MIME sniffing
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // Prevent XSS
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload'); // Enforce HTTPS
    next();
});

// Database connection configuration using environment variables
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
    connectionLimit: 10,
    multipleStatements: false, // Prevent SQL injection
};

// Create a pool of database connections
const pool = mysql.createPool(dbConfig);

// Function to handle database errors
function handleDatabaseError(res, err, message = "Database error") {
    console.error(err);
    res.status(500).json({ error: message });
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
    // Input validation and sanitization
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: "Username and password must be strings" });
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(username)) {
        return res.status(400).json({ error: "Invalid username format" });
    }

    if (password.length < 8) {
        return res.status(400).json({ error: "Password must be at least 8 characters long" });
    }

    // Generate a random salt
    crypto.randomBytes(16, (err, saltBuffer) => {
        if (err) {
            return handleDatabaseError(res, err, "Error generating salt");
        }
        const salt = saltBuffer.toString('hex');

        // Hash the password with the salt
        crypto.pbkdf2(password, salt, 310000, 32, 'sha256', (err, hashedPasswordBuffer) => {
            if (err) {
                return handleDatabaseError(res, err, "Error hashing password");
            }
            const hashedPassword = hashedPasswordBuffer.toString('hex');

            // Store the username, salt, and hashed password in the database using a parameterized query
            pool.getConnection((err, connection) => {
                if (err) {
                    return handleDatabaseError(res, err, "Error getting database connection");
                }

                // Use parameterized query to prevent SQL injection
                const query = "INSERT INTO users (username, salt, hashed_password) VALUES (?, ?, ?)";
                connection.query(query, [username, salt, hashedPassword], (err, result) => {
                    connection.release(); // Release the connection back to the pool

                    if (err) {
                        return handleDatabaseError(res, err, "Error inserting user into database");
                    }

                    console.log("User registered successfully");
                    res.status(201).json({ message: "User registered successfully" });
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});