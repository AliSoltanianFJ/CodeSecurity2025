const express = require('express');
const mysql = require('mysql');

const app = express();

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "frame-src 'none'");
    next();
});

app.get('/unsubscribe', (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ error: 'Email is required' });
    }

    // Database configuration using environment variables
    const dbConfig = {
        host: process.env.DB_HOST || 'localhost',
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'your_database'
    };

    const pool = mysql.createPool(dbConfig);

    try {
        pool.getConnection((err, connection) => {
            if (err) {
                console.error('Error getting DB connection:', err);
                return res.status(500).json({ error: 'Database connection failed' });
            }

            // Check if email exists
            const checkQuery = 'SELECT id FROM users WHERE email = ?';
            connection.query(checkQuery, [mysql.escape(email)], (err, results) => {
                connection.release();
                if (err) {
                    console.error('Error checking email:', err);
                    return res.status(500).json({ error: 'Database error' });
                }

                if (results.length === 0) {
                    return res.status(404).json({ message: 'Email not found' });
                }

                // Update subscription status
                const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
                connection.query(unsubscribeQuery, [mysql.escape(email)], (err, result) => {
                    if (err) {
                        console.error('Error unsubscribing:', err);
                        return res.status(500).json({ error: 'Unsubscription failed' });
                    }

                    res.json({ message: 'Successfully unsubscribed' });
                });
            });
        });
    } catch (error) {
        console.error('Unexpected error:', error);
        res.status(500).json({ error: 'An unexpected error occurred' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});