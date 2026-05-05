const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
require('dotenv').config(); // Load environment variables from .env file

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database configuration using environment variables
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'mydb',
    port: process.env.DB_PORT || 3306,
    connectionLimit: 10,
    multipleStatements: false, // Prevent multiple SQL statements in one query
    charset: 'utf8mb4' // Use a character set that supports a wide range of characters
};

// Create a pool of connections
const pool = mysql.createPool(dbConfig);

// Validate email format
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Sanitize email to prevent CRLF injection and other attacks
function sanitizeEmail(email) {
    // Remove any characters that could be used for injection
    return email.replace(/[\r\n%'"`;<>=\\]/g, '');
}

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).send("Email is required");
    }

    if (!isValidEmail(email)) {
        return res.status(400).send("Invalid email format");
    }

    const sanitizedEmail = sanitizeEmail(email);

    // Use a secure method to generate a unique salt
    const salt = crypto.randomBytes(16).toString('hex');

    // Hash the email with the salt
    const emailHash = crypto.createHash('sha256').update(sanitizedEmail + salt).digest('hex');

    // Use a pool connection to handle database operations
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database");
            return res.status(500).send("Database connection error");
        }

        // Use a parameterized query to prevent SQL injection
        const query = "SELECT * FROM users WHERE email_hash = ? AND salt = ?";
        connection.query(query, [emailHash, salt], (error, results) => {
            if (error) {
                console.error("Error querying the database");
                connection.release(); // Release the connection back to the pool
                return res.status(500).send("Error querying the database");
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).send("Email not found");
            }

            // Unsubscribe the user if the email is found
            const unsubscribeQuery = "UPDATE users SET subscribed = 0 WHERE email_hash = ? AND salt = ?";
            connection.query(unsubscribeQuery, [emailHash, salt], (unsubscribeError) => {
                connection.release(); // Always release the connection

                if (unsubscribeError) {
                    console.error("Error unsubscribing user");
                    return res.status(500).send("Error unsubscribing user");
                }

                return res.status(200).send("Successfully unsubscribed");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});