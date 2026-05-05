const express = require("express");
const mysql = require("mysql");

const app = express();

const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 5
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (!email || typeof email !== "string" || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).send("Invalid request");
  }
  db.query("SELECT id FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) return res.status(500).send("Server error");
    if (!rows || rows.length === 0) return res.status(404).send("Not found");
    const id = rows[0].id;
    db.query("UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ?", [id], (uErr) => {
      if (uErr) return res.status(500).send("Server error");
      return res.status(200).send("Unsubscribed");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
