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
const mysql = require("mysql");
require('dotenv').config();                                 // load DB creds from env

// create a pool with no multipleStatements to block stacked queries
const pool = mysql.createPool({
    host            : process.env.DB_HOST,
    user            : process.env.DB_USER,
    password        : process.env.DB_PASSWORD,
    database        : process.env.DB_NAME,
    connectionLimit : 10,
    multipleStatements: false
});

// overwrite the stub handler with a secure implementation
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    const MAX_EMAIL_LEN = 254;
    const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    // validate & sanitize
    if (
        !email ||
        typeof email !== 'string' ||
        email.length > MAX_EMAIL_LEN ||
        !EMAIL_REGEX.test(email)
    ) {
        return res.status(400).send('Invalid email address.');
    }

    // check existence
    pool.query(
        'SELECT id FROM subscribers WHERE email = ?',
        [email],
        (selectErr, rows) => {
            if (selectErr) {
                console.error('DB SELECT error:', selectErr);
                return res.status(500).send('Internal server error.');
            }
            if (rows.length === 0) {
                return res.status(404).send('Email not found.');
            }

            // unsubscribe
            pool.query(
                'UPDATE subscribers SET subscribed = 0 WHERE email = ?',
                [email],
                (updateErr) => {
                    if (updateErr) {
                        console.error('DB UPDATE error:', updateErr);
                        return res.status(500).send('Internal server error.');
                    }
                    // do not echo back the email or any private info
                    res.send('You have been unsubscribed successfully.');
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
