const express = require("express");
const mysql = require("mysql");

const app = express();

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  charset: "utf8mb4"
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (typeof email !== "string" || email.length === 0 || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ error: "Invalid request." });
  }
  pool.query("SELECT id FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) return res.status(500).json({ error: "Server error." });
    if (!rows.length) return res.json({ message: "If the address was subscribed it has been unsubscribed." });
    pool.query("DELETE FROM subscribers WHERE id = ? LIMIT 1", [rows[0].id], (delErr) => {
      if (delErr) return res.status(500).json({ error: "Server error." });
      res.json({ message: "If the address was subscribed it has been unsubscribed." });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
