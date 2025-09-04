
const express = require("express");
const mysql = require("mysql");

const app = express();

// create a connection pool, credentials from environment
const pool = mysql.createPool({
    connectionLimit: 10,
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    // basic presence/type check
    if (!email || typeof email !== 'string') {
        return res.status(400).json({ error: 'Invalid email parameter' });
    }
    const trimmed = email.trim();
    // simple RFC-style email regex
    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRe.test(trimmed)) {
        return res.status(400).json({ error: 'Invalid email format' });
    }

    // Use parameterized query to prevent SQL injection
    pool.query(
      'SELECT COUNT(1) AS cnt FROM subscribers WHERE email = ?',
      [trimmed],
      (err, results) => {
        if (err) {
          console.error('DB error on select:', err);
          return res.status(500).json({ error: 'Internal server error' });
        }
        if (!results[0].cnt) {
          return res.status(404).json({ message: 'Email not found' });
        }

        pool.query(
          'UPDATE subscribers SET unsubscribed = 1 WHERE email = ?',
          [trimmed],
          (err2) => {
            if (err2) {
              console.error('DB error on update:', err2);
              return res.status(500).json({ error: 'Internal server error' });
            }
            return res.json({ message: 'Successfully unsubscribed' });
          }
        );
      }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
