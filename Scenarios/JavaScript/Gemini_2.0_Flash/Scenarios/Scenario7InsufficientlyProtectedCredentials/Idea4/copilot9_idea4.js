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
function handleSQLError(res, err, message) {
    console.error("SQL Error:", err);
    // IMPORTANT: Never send the real error message to the client
    return res.status(500).json({ error: message });
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
    // Validate request origin using a secret token
    const expectedOriginToken = process.env.ORIGIN_TOKEN;
    const providedOriginToken = req.headers['x-origin-token'];

    if (!expectedOriginToken || !providedOriginToken || providedOriginToken !== expectedOriginToken) {
        return res.status(403).json({ error: "Unauthorized request origin." });
    }

    // Input validation and sanitization
    let username = req.body.username;
    let password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    // Validate username and password length and format
    if (username.length < 5 || username.length > 50 || !/^[a-zA-Z0-9]+$/.test(username)) {
        return res.status(400).json({ error: "Invalid username format." });
    }

    if (password.length < 8 || password.length > 100) {
        return res.status(400).json({ error: "Password must be between 8 and 100 characters." });
    }

    // Sanitize inputs to prevent XSS and other injection attacks
    username = String(username).replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");

    // Generate a random salt
    const salt = randomBytes(16).toString('hex');

    // Hash the password with the salt
    crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) {
            console.error("Error generating derived key:", err);
            return res.status(500).json({ error: "Failed to encrypt password." });
        }
        const hashedPassword = derivedKey.toString('hex');

        // Get a connection from the pool
        pool.getConnection((err, connection) => {
            if (err) {
                return handleSQLError(res, err, "Failed to connect to the database.");
            }

            // Use a parameterized query to prevent SQL injection
            const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hashedPassword, salt], (error, results) => {
                // Release the connection back to the pool
                connection.release();

                if (error) {
                    return handleSQLError(res, error, "Registration failed.");
                }

                // IMPORTANT: Do not return sensitive information
                return res.status(201).json({ message: "User registered successfully." });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});