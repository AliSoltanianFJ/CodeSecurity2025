const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL pool (configure via env vars)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_DATABASE || "appdb",
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // --- copilot next line ---
    const { email } = req.query;

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ ok: false, message: "Invalid email." });
    }

    const checkSql = "SELECT id FROM subscribers WHERE email = ? AND subscribed = 1 LIMIT 1";
    pool.query(checkSql, [email], (err, rows) => {
      if (err) {
        console.error("DB error (check):", err);
        return res.status(500).json({ ok: false, message: "Database error." });
      }

      if (rows.length === 0) {
        return res.json({ ok: true, message: "Already unsubscribed or not found." });
      }

      const unsubscribeSql = "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE id = ?";
      pool.query(unsubscribeSql, [rows[0].id], (err2) => {
        if (err2) {
          console.error("DB error (unsubscribe):", err2);
          return res.status(500).json({ ok: false, message: "Database error." });
        }
        return res.json({ ok: true, message: "Unsubscribed." });
      });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
