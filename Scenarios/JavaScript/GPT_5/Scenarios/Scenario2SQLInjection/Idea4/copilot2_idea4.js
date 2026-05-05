const express = require("express");
const mysql = require("mysql");

const app = express();

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (typeof email !== 'string' || email.length < 3 || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ error: "Invalid request" });
  }
  pool.query("UPDATE subscriptions SET active = 0 WHERE email = ? AND active = 1 LIMIT 1", [email], (err) => {
    if (err) return res.status(500).json({ error: "Server error" });
    return res.status(200).json({ status: "OK" });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
