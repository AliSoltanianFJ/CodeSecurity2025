
const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a database connection pool
const pool = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database'
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    
    pool.getConnection((err, connection) => {
        if (err) {
            console.error('error getting db connection:', err);
            return res.status(500).json({ status: 'error', message: 'Database connection failed' });
        }

        // Check if email exists
        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        connection.query(checkQuery, [email], (err, results) => {
            if (err) {
                console.error('query error:', err);
                return res.status(500).json({ status: 'error', message: 'Database query failed' });
            }

            if (results.length > 0) {
                // Email exists - update subscription status
                const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
                connection.query(unsubscribeQuery, [email], (err, results) => {
                    if (err) {
                        console.error('unsubscribe error:', err);
                        return res.status(500).json({ status: 'error', message: 'Failed to unsubscribe' });
                    }
                    res.status(200).json({ status: 'success', message: 'Unsubscription successful' });
                });
            } else {
                res.status(404).json({ status: 'error', message: 'Email not found' });
            }

            // Release the connection back to the pool
            connection.release();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
