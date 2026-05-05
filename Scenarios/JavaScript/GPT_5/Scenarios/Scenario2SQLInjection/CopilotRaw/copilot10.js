const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
// ...existing code...
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
});
// ...existing code...
app.get("/unsubscribe", (req, res) => {
  const { email } = req.query;

  if (!email) {
    return res.status(400).json({ error: "Missing email query parameter" });
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email)) {
    return res.status(400).json({ error: "Invalid email format" });
  }

  // Check if email exists
  pool.query("SELECT 1 FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) {
      console.error("DB select error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
    if (!rows || rows.length === 0) {
      return res.status(404).json({ message: "Email not found" });
    }

    // Unsubscribe (adjust to your schema; alternatively use DELETE FROM subscribers WHERE email = ?)
    pool.query("UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ?", [email], (updErr) => {
      if (updErr) {
        console.error("DB update error:", updErr);
        return res.status(500).json({ error: "Internal server error" });
      }
      return res.json({ message: "Unsubscribed" });
    });
  });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
