const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Securely retrieve database credentials from environment variables
const dbHost = process.env.DB_HOST || 'localhost';
const dbUser = process.env.DB_USER || 'root';
const dbPassword = process.env.DB_PASSWORD || '';
const dbName = process.env.DB_NAME || 'mydb';

// Create a secure database connection pool
const pool = mysql.createPool({
    host: dbHost,
    user: dbUser,
    password: dbPassword,
    database: dbName,
    connectionLimit: 10,
    multipleStatements: false, // Prevent multiple SQL statements in one query
    charset: 'utf8mb4',       // Use a character set that supports a wide range of characters
    ssl: {                   // Enable SSL/TLS encryption for the connection
        rejectUnauthorized: true // Verify the server's certificate
    }
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
    next();
});

// Email validation regex (OWASP recommended)
const emailRegex = new RegExp(/^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$/);

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Input validation: Check if email is provided
    if (!email) {
        return res.status(400).send("Email is required");
    }

    // Input validation: Validate email format
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format");
    }

    // Sanitize the email input to prevent SQL injection
    const sanitizedEmail = String(email).trim().toLowerCase();

    // Generate a unique salt for each email
    const salt = crypto.randomBytes(16).toString('hex');
    const saltedEmail = salt + sanitizedEmail;

    // Hash the salted email using SHA-256 for secure comparison
    const hashedEmail = crypto.createHash('sha256').update(saltedEmail).digest('hex');

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).send("Database connection error");
        }

        // SQL query with parameterized values
        const query = "UPDATE users SET subscribed = 0 WHERE hashed_email = ?";

        connection.query(query, [hashedEmail], (error, results) => {
            connection.release(); // Release the connection back to the pool

            if (error) {
                console.error("Error unsubscribing email");
                return res.status(500).send("Failed to unsubscribe");
            }

            if (results.affectedRows > 0) {
                return res.status(200).send("Successfully unsubscribed");
            } else {
                return res.status(404).send("Email not found");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});