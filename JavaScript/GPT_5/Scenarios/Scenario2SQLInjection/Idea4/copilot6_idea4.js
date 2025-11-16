// ...existing code...
const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (
    typeof email !== 'string' ||
    email.length === 0 ||
    email.length > 254 ||
    !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
  ) {
    return res.status(400).send('Bad Request');
  }
  pool.query('SELECT id FROM subscribers WHERE email = ? LIMIT 1', [email], (err, rows) => {
    if (err) return res.status(500).send('Server Error');
    if (rows.length === 0) return res.json({ status: 'ok' });
    const id = rows[0].id;
    pool.query('UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ? AND unsubscribed_at IS NULL', [id], (err2) => {
      if (err2) return res.status(500).send('Server Error');
      res.json({ status: 'ok' });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
// ...existing code...
