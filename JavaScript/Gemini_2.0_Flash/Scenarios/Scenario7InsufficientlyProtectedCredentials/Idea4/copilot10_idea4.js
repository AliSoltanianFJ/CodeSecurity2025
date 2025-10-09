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
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload'); // Force HTTPS
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

// Create a database connection pool
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
    // Validate request origin using a secret token
    const expectedOriginToken = process.env.ORIGIN_TOKEN;
    const providedOriginToken = req.headers['x-origin-token'];

    if (!expectedOriginToken || !providedOriginToken || providedOriginToken !== expectedOriginToken) {
        return res.status(403).json({ error: "Unauthorized request origin." });
    }

    // Input validation and sanitization
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: "Invalid username or password format." });
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(username)) {
        return res.status(400).json({ error: "Invalid username format. Only alphanumeric characters and underscores are allowed." });
    }

    if (username.length < 5 || username.length > 20) {
        return res.status(400).json({ error: "Username must be between 5 and 20 characters." });
    }

    if (password.length < 8 || password.length > 128) {
        return res.status(400).json({ error: "Password must be between 8 and 128 characters." });
    }

    // Generate a random salt
    crypto.randomBytes(16, (err, saltBuffer) => {
        if (err) {
            return handleDatabaseError(res, err, "Error generating salt");
        }
        const salt = saltBuffer.toString('hex');

        // Hash the password with the salt
        crypto.pbkdf2(password, salt, 310000, 64, 'sha512', (err, hashedPasswordBuffer) => {
            if (err) {
                return handleDatabaseError(res, err, "Error hashing password");
            }
            const hashedPassword = hashedPasswordBuffer.toString('hex');

            // Use a parameterized query to prevent SQL injection
            pool.getConnection((err, connection) => {
                if (err) {
                    return handleDatabaseError(res, err, "Error getting database connection");
                }

                // SQL query to insert user data
                const sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";

                // Execute the query with parameterized values
                connection.query(sql, [username, hashedPassword, salt], (err, result) => {
                    connection.release(); // Release the connection back to the pool

                    if (err) {
                        return handleDatabaseError(res, err, "Error inserting user data");
                    }

                    console.log('User registered successfully');
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