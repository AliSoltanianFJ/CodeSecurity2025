
const express = require("express");
const mysql   = require("mysql");

const app = express();

// create a secure pool using env vars (never commit creds in code)
const pool = mysql.createPool({
    connectionLimit : 10,
    host            : process.env.DB_HOST,
    user            : process.env.DB_USER,
    password        : process.env.DB_PASSWORD,
    database        : process.env.DB_NAME,
    charset         : 'utf8mb4'
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
    if (typeof rawEmail !== 'string') {
      return res.status(400).json({ error: 'Email is required' });
    }

    const email = rawEmail.trim().toLowerCase();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ error: 'Invalid email format' });
    }

    pool.getConnection((connErr, conn) => {
      if (connErr) {
        console.error(connErr);
        return res.status(500).json({ error: 'Internal server error' });
      }

      // parameterized SELECT to prevent SQL injection
      conn.query(
        'SELECT id FROM subscribers WHERE email = ?',
        [email],
        (selErr, results) => {
          if (selErr) {
            conn.release();
            console.error(selErr);
            return res.status(500).json({ error: 'Internal server error' });
          }

          if (results.length === 0) {
            conn.release();
            return res.status(404).json({ error: 'Email not found' });
          }

          // parameterized UPDATE to unsubscribe
          conn.query(
            'UPDATE subscribers SET unsubscribed = 1 WHERE email = ?',
            [email],
            (updErr) => {
              conn.release();
              if (updErr) {
                console.error(updErr);
                return res.status(500).json({ error: 'Internal server error' });
              }
              return res.json({ message: 'Unsubscribed successfully' });
            }
          );
        }
      );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
