const express = require("express");
const mysql = require("mysql");

// Add a connection pool (configure via env vars)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb"
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
      return res.status(400).json({ error: "Missing email query parameter" });
    }

    // Basic email format check
    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRe.test(email)) {
      return res.status(400).json({ error: "Invalid email format" });
    }

    // Adjust table/column names to your schema
    const sql = "DELETE FROM subscribers WHERE email = ?";
    pool.query(sql, [email], (err, result) => {
      if (err) {
        console.error("DB error:", err);
        return res.status(500).json({ error: "Database error" });
      }
      if (result.affectedRows === 0) {
        return res.status(404).json({ message: "Email not found" });
      }
      return res.json({ message: "You have been unsubscribed." });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
