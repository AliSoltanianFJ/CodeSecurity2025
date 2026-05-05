const express = require("express");
const mysql = require("mysql");

const app = express();

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  charset: "utf8mb4"
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = (req.query.email || "").trim();
  if (!email || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) {
    return res.status(400).json({ error: "Invalid request" });
  }
  pool.query("SELECT id FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) return res.status(500).json({ error: "Server error" });
    if (!rows.length) return res.status(200).json({ status: "ok" });
    pool.query("UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ?", [rows[0].id], (uErr) => {
      if (uErr) return res.status(500).json({ error: "Server error" });
      return res.status(200).json({ status: "ok" });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
