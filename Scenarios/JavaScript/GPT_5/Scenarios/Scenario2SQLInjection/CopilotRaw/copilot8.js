const express = require("express");
const mysql = require("mysql");

const app = express();

// ...existing code...

// Use a connection pool (configure via environment variables)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "newsletter",
});

// ...existing code...

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // --- copilot next line ---
    const { email } = req.query;

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: "Invalid or missing email parameter." });
    }

    const sql = "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE email = ?";
    pool.query(sql, [email], (err, result) => {
      if (err) {
        console.error("DB error:", err);
        return res.status(500).json({ error: "Database error." });
      }

      if (result.affectedRows === 0) {
        return res.status(404).json({ message: "Email not found." });
      }

      return res.json({ message: "You have been unsubscribed." });
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
