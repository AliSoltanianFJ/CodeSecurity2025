const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { randomBytes } = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Securely configure headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
    next();
});

// Centralized error handling function
function handleRegistrationError(res, error, sensitiveData = null) {
    console.error("Registration Error:", error);
    if (sensitiveData) {
        // Securely log the error without exposing sensitive information
        console.error("Error details (without sensitive data):", error.message);
    }
    res.status(500).send("Registration failed. Please try again.");
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation: check for null, undefined, and empty strings
    if (!username || !password) {
        return handleRegistrationError(res, new Error("Username and password are required."));
    }

    // Input validation: check for maximum lengths
    if (username.length > 64 || password.length > 128) {
        return handleRegistrationError(res, new Error("Username or password too long."));
    }

    // Input validation: restrict allowed characters (example: alphanumeric and some special characters)
    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(username)) {
        return handleRegistrationError(res, new Error("Invalid username format."));
    }

    // Generate a secure random salt
    crypto.randomBytes(16, (err, saltBuffer) => {
        if (err) {
            return handleRegistrationError(res, err);
        }
        const salt = saltBuffer.toString('hex');

        // Hash the password using a strong hashing algorithm (e.g., SHA512) and the salt
        crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, hashedPasswordBuffer) => {
            if (err) {
                return handleRegistrationError(res, err);
            }
            const hashedPassword = hashedPasswordBuffer.toString('hex');

            // Database connection configuration (read from environment variables)
            const dbConfig = {
                host: process.env.DB_HOST,
                user: process.env.DB_USER,
                password: process.env.DB_PASSWORD,
                database: process.env.DB_NAME,
                connectionLimit: 10,
                multipleStatements: false, // Prevent SQL injection
                connectTimeout: 60 * 60 * 1000,
                acquireTimeout: 60 * 60 * 1000,
                timeout: 60 * 60 * 1000,
            };

            // Create a database connection pool
            const pool = mysql.createPool(dbConfig);

            // Acquire a connection from the pool
            pool.getConnection((err, connection) => {
                if (err) {
                    return handleRegistrationError(res, err);
                }

                // Use a parameterized query to prevent SQL injection
                const query = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
                connection.query(query, [username, hashedPassword, salt], (err, result) => {
                    // Release the connection back to the pool
                    connection.release();

                    if (err) {
                        return handleRegistrationError(res, err, { username: username });
                    }

                    console.log("User registered successfully");
                    res.status(201).send("User registered successfully");
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});