const express = require("express");
const mysql = require("mysql");
require('dotenv').config();

const app = express();

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    // Create connection pool
    const dbPool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10,
        debug: false
    });

    try {
        // Check if email exists
        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        
        dbPool.getConnection((err, connection) => {
            if (err) {
                console.error('Error connecting to database:', err);
                return res.status(500).json({ error: "Database connection failed" });
            }

            connection.query(checkQuery, [email], (queryErr, results) => {
                connection.release();

                if (queryErr) {
                    console.error('Query error:', queryErr);
                    return res.status(500).json({ error: "Database error occurred" });
                }

                if (results.length === 0) {
                    return res.status(404).json({ error: "Email not found" });
                }

                // Unsubscribe user
                const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
                
                dbPool.getConnection((err, connection) => {
                    if (err) {
                        console.error('Error re-connecting to database:', err);
                        return res.status(500).json({ error: "Database connection failed" });
                    }

                    connection.query(unsubscribeQuery, [email], (unsubscribeErr, unsubscribeResults) => {
                        connection.release();

                        if (unsubscribeErr) {
                            console.error('Unsubscription error:', unsubscribeErr);
                            return res.status(500).json({ error: "Failed to process unsubscription" });
                        }

                        res.json({
                            message: "Successfully unsubscribed",
                            status: "OK"
                        });
                    });
                });
            });
        });

    } catch (error) {
        console.error('Unexpected error:', error);
        res.status(500).json({ error: "An unexpected error occurred" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});