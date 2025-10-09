const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { randomBytes } = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Set HTTP security headers to prevent common attacks
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY'); // Prevent clickjacking
    res.setHeader('X-Content-Type-Options', 'nosniff'); // Prevent MIME sniffing
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin'); // Control referrer information
    res.setHeader('Permissions-Policy', 'geolocation=(), microphone=()'); // Disable features
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // mitigate XSS, only allow resources from same origin
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload'); // Force HTTPS
    res.setHeader('Cache-Control', 'no-store'); // Ensure that sensitive data is not cached
    next();
});

// Database connection configuration using environment variables
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'password',
    database: process.env.DB_DATABASE || 'database',
    connectionLimit: 10,
    multipleStatements: false, // Prevent SQL injection
};

// Create a pool of database connections
const pool = mysql.createPool(dbConfig);

// Function to handle database errors
function handleDBError(err, res, message = "Database error") {
    console.error(err);
    res.status(500).send(message);
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
    // Validate request origin to prevent CSRF
    const origin = req.get('Origin');
    if (!origin || origin !== 'https://your-app-domain.com') {
        console.warn('CSRF attempt detected');
        return res.status(400).send('Invalid origin');
    }

    // Input validation and sanitization
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send("Username and password must be strings");
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(username)) {
        return res.status(400).send("Invalid username format");
    }

    if (password.length < 8) {
        return res.status(400).send("Password must be at least 8 characters long");
    }

    // Generate a unique salt for each user
    const salt = crypto.randomBytes(16).toString('hex');

    // Hash the password with the salt using a strong hashing algorithm
    crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) {
            return handleDBError(err, res, "Error generating password hash");
        }
        const hashedPassword = derivedKey.toString('hex');

        // Use a parameterized query to prevent SQL injection
        pool.getConnection((err, connection) => {
            if (err) {
                return handleDBError(err, res, "Database connection error");
            }

            // SQL query with parameterized values
            const query = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hashedPassword, salt], (error, results) => {
                connection.release(); // Release the connection back to the pool

                if (error) {
                    return handleDBError(error, res, "Error inserting user into database");
                }

                console.log('User registered successfully');
                res.status(201).send("User registered successfully");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});