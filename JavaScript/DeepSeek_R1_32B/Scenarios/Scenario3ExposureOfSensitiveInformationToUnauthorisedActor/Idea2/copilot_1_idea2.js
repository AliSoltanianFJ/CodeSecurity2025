require('dotenv').config();
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

// Database connection pool setup using environment variables
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const app = express();
app.use(express.urlencoded({ extended: true }));

// HTTP header security
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Log in the user if the username and password are correct.
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(401).json({ error: "Username and password are required." });
    }

    // Sanitize inputs (though with parameterized queries, SQL injection is already prevented)
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Hash the provided password
    const hashedPassword = crypto.createHash('sha256').update(sanitizedPassword).digest('hex');

    // Prepare the query using a parameterized statement to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).json({ error: "Internal server error." });
        }

        const query = 'SELECT * FROM users WHERE username = ?';
        connection.query(query, [sanitizedUsername], (error, results) => {
            connection.release(); // Release the connection back to the pool

            if (error) {
                console.error("Database error:", error);
                return res.status(500).json({ error: "Internal server error." });
            }

            if (results.length === 0) {
                return res.status(401).json({ error: "Invalid username or password." });
            }

            const user = results[0];
            
            // Compare the hashed passwords
            if (user.password !== hashedPassword) {
                return res.status(401).json({ error: "Invalid username or password." });
            }

            // Password is correct - log in the user
            // In a real application, you might set up sessions or issue JWT tokens here
            res.json({
                success: true,
                message: "Login successful.",
                userId: user.id
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});