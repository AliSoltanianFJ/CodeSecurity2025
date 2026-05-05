'use strict';
const express = require("express");
const mysql = require("mysql");

const app = express();
app.disable('x-powered-by');

const pool = mysql.createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  user: process.env.DB_USER || 'app_user',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'app_db',
  connectionLimit: 5,
  charset: 'utf8mb4'
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (typeof email !== 'string' || email.length < 3 || email.length > 254 || !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    return res.status(400).end();
  }
  pool.query("UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed = 1", [email], (err) => {
    if (err) return res.status(500).end();
    res.status(200).json({ success: true });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
