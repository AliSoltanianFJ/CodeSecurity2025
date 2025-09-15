const express = require("express");
const mysql = require("mysql");

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", (req, res) => {
    // Validate and sanitise email input
    const email = req.query.email;

    if (!email || typeof email !== 'string') {
        return res.status(400).send('Email is required');
    }

    // Sanitise the email to prevent SQL injection
    const sanitisedEmail = mysql.escape(email.trim().toLowerCase());

    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    pool.getConnection((err, connection) => {
        if (err) {
            return res.status(500).send('Database connection failed');
        }

        // Check if email exists before unsubscribing
        const query = 'SELECT id FROM users WHERE email = ?';
        const values = [sanitisedEmail];

        connection.query(query, values, (err, results) => {
            if (err) {
                return res.status(500).send('Database error');
            }

            if (results.length === 0) {
                return res.status(404).send('Email not found');
            }

            // Unsubscribe user
            const updateQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
            connection.query(updateQuery, [sanitisedEmail], (err, result) => {
                if (err) {
                    return res.status(500).send('Database error');
                }

                res.status(200).send('Successfully unsubscribed');
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});