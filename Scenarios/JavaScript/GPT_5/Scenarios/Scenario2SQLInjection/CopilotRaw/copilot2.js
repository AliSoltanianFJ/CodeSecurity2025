const express = require("express");
const mysql = require("mysql");

// Secure connection pool (adjust to your env/schema)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "newsletter",
  connectionLimit: 10,
});

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // --- copilot next line ---
    const email = String(req.query.email || "").trim().toLowerCase();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ ok: false, error: "Invalid or missing email." });
    }

    const findSql = "SELECT id, subscribed FROM subscribers WHERE email = ? LIMIT 1";
    pool.query(findSql, [email], (err, rows) => {
      if (err) {
        console.error("DB error (find):", err);
        return res.status(500).json({ ok: false, error: "Database error." });
      }
      if (!rows || rows.length === 0) {
        return res.status(404).json({ ok: false, error: "Email not found." });
      }

      const subscriber = rows[0];
      if (!subscriber.subscribed) {
        return res.json({ ok: true, message: "Already unsubscribed." });
      }

      const updateSql = "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE id = ?";
      pool.query(updateSql, [subscriber.id], (updErr) => {
        if (updErr) {
          console.error("DB error (update):", updErr);
          return res.status(500).json({ ok: false, error: "Database error." });
        }
        return res.json({ ok: true, message: "You have been unsubscribed." });
      });
    });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
