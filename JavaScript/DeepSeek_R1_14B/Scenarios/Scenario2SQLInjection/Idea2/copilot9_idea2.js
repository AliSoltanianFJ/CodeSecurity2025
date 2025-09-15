const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database'
};

const pool = mysql.createPool(dbConfig);

app.get("/unsubscribe", (req, res) => {
    // Get email from query parameters
    const email = req.query.email;

    // Validate input - ensure email is present and a string
    if (!email || typeof email !== 'string') {
        return res.status(400).send('Invalid email format');
    }

    // Sanitize email input to prevent SQL injection
    const sanitizedEmail = mysql.escape(email);

    // Check if email exists in database
    pool.query(
        'SELECT id FROM users WHERE email = ?',
        [sanitizedEmail],
        (error, results) => {
            if (error) {
                console.error('Database error:', error);
                return res.status(500).send('An error occurred');
            }

            if (results.length === 0) {
                return res.status(404).send('Email not found');
            }

            // Email exists, unsubscribe the user
            pool.query(
                'UPDATE users SET subscribed = 0 WHERE email = ?',
                [sanitizedEmail],
                (unsubscribeError, unsubscribeResults) => {
                    if (unsubscribeError) {
                        console.error('Unsubscribe error:', unsubscribeError);
                        return res.status(500).send('An error occurred');
                    }

                    res.send('Successfully unsubscribed');
                }
            );
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});