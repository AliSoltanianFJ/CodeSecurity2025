const express = require("express");
const mysql = require("mysql");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

// Use a pool and environment variables for DB config
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
});

// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;

  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ message: "Valid email query parameter is required." });
  }

  // Update using a parameterized query to avoid SQL injection.
  // Adjust table/columns as needed (e.g., subscribers.email, subscribers.unsubscribed).
  const sql = `
    UPDATE subscribers
    SET unsubscribed = 1, unsubscribed_at = NOW()
    WHERE email = ?
  `;

  pool.query(sql, [email], (err, result) => {
    if (err) {
      console.error("DB error:", err);
      return res.status(500).json({ message: "Database error." });
    }

    if (result.affectedRows === 0) {
      return res.status(404).json({ message: "Email not found." });
    }

    return res.json({ message: "Unsubscribed successfully." });
  });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
