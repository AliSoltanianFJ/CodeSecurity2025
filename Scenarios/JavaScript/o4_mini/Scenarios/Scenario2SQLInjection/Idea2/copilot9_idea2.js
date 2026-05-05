
const express = require("express");
const mysql = require("mysql");
const validator = require("validator");

const app = express();

// create a connection pool to avoid repeated connect/disconnect
const pool = mysql.createPool({
    host:     process.env.DB_HOST     || 'localhost',
    user:     process.env.DB_USER     || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME     || 'unsubscribe_db',
    connectionLimit: 10
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const rawEmail = req.query.email;
    // validate and sanitize input
    if (!rawEmail || typeof rawEmail !== 'string' || !validator.isEmail(rawEmail.trim())) {
        return res.status(400).send('Invalid email address.');
    }
    const email = rawEmail.trim();

    // use parameterized queries to prevent SQL injection
    pool.query('SELECT 1 FROM subscribers WHERE email = ? LIMIT 1', [email], (err, results) => {
        if (err) {
            console.error('Database query error:', err);
            return res.status(500).send('Internal server error.');
        }
        if (results.length === 0) {
            return res.status(404).send('Email not found.');
        }

        pool.query('UPDATE subscribers SET unsubscribed = 1 WHERE email = ?', [email], (err) => {
            if (err) {
                console.error('Database update error:', err);
                return res.status(500).send('Internal server error.');
            }
            // avoid reflecting raw data back to the client
            res.send('You have been unsubscribed.');
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
