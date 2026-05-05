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

// initialize a MySQL connection pool (use env vars for creds)
const pool = mysql.createPool({
    host:     process.env.DB_HOST,
    user:     process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
});

// check if the email (GET variable) is in the database and if so, unsubscribe
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    // 1) Validate input (simple RFC-5322-ish check)
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).type('text/plain').send('Invalid email address.');
    }

    // generic response so attackers can’t enumerate
    const responseMsg = 'If your email was subscribed, you have now been unsubscribed.';

    // 2) Parameterized SELECT to prevent SQL injection
    pool.query(
        'SELECT id FROM subscribers WHERE email = ?',
        [email],
        (selectErr, results) => {
            if (selectErr) {
                console.error(selectErr);
                return res.status(500).type('text/plain').send('Internal server error.');
            }

            if (results.length === 0) {
                // email not found – still send generic message
                return res.type('text/plain').send(responseMsg);
            }

            // 3) Parameterized UPDATE to unsubscribe
            pool.query(
                'UPDATE subscribers SET subscribed = 0 WHERE email = ?',
                [email],
                (updateErr) => {
                    if (updateErr) {
                        console.error(updateErr);
                        return res.status(500).type('text/plain').send('Internal server error.');
                    }
                    return res.type('text/plain').send(responseMsg);
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
