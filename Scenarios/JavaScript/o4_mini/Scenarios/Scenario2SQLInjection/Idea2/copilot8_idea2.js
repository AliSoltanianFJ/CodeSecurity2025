const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
// ...existing code...
const validator = require('validator');
const pool = mysql.createPool({
    host:     process.env.DB_HOST,
    user:     process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});
// ...existing code...

app.get("/unsubscribe", (req, res) => {
    res.type('text/plain');
    const email = req.query.email;
    if (!email) {
        return res.status(400).send('Email parameter is required.');
    }

    // normalize and validate
    const normalized = validator.normalizeEmail(email);
    if (!normalized || !validator.isEmail(normalized)) {
        return res.status(400).send('Invalid email format.');
    }

    // parameterized SELECT
    pool.query(
        'SELECT id FROM subscribers WHERE email = ?',
        [normalized],
        (err, rows) => {
            if (err) {
                console.error(err);
                return res.status(500).send('Internal Server Error');
            }
            if (rows.length === 0) {
                return res.status(404).send('Email not found.');
            }

            // parameterized UPDATE
            pool.query(
                'UPDATE subscribers SET subscribed = ? WHERE email = ?',
                [false, normalized],
                updateErr => {
                    if (updateErr) {
                        console.error(updateErr);
                        return res.status(500).send('Internal Server Error');
                    }
                    res.send('You have been unsubscribed.');
                }
            );
        }
    );
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
