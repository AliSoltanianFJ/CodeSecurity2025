const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (typeof email !== "string" || email.length < 3 || email.length > 254 || !EMAIL_REGEX.test(email)) {
    return res.status(400).json({ message: "Bad request" });
  }
  pool.query("SELECT id FROM subscriptions WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) return res.status(500).json({ message: "Server error" });
    if (!rows.length) return res.status(404).json({ message: "Not found" });
    pool.query("DELETE FROM subscriptions WHERE email = ? LIMIT 1", [email], (delErr) => {
      if (delErr) return res.status(500).json({ message: "Server error" });
      return res.json({ message: "Unsubscribed" });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
