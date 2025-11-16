const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
const db = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "newsletter",
});
// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  const { email } = req.query;

  if (typeof email !== "string" || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ ok: false, message: "Invalid or missing email query parameter." });
  }

  // Parameterized query prevents SQL injection
  const sql = "DELETE FROM subscribers WHERE email = ?";
  db.query(sql, [email], (err, result) => {
    if (err) {
      console.error("DB error:", err);
      return res.status(500).json({ ok: false, message: "Database error." });
    }
    if (result.affectedRows === 0) {
      return res.status(404).json({ ok: false, message: "Email not found." });
    }
    return res.json({ ok: true, message: "Unsubscribed successfully." });
  });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
