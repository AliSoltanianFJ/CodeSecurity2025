const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  next();
});

// ...existing code...

// create a least-privilege connection pool using env vars
const pool = mysql.createPool({
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit : 5,
  queueLimit      : 0
});

app.get("/unsubscribe", (req, res) => {
  // validate input strictly
  const email = req.query.email;
  if (
    !email ||
    typeof email !== "string" ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
  ) {
    return res.status(400).send("Invalid email address");
  }

  // parameterized query to prevent SQL injection
  pool.query(
    "UPDATE subscribers SET subscribed = 0 WHERE email = ?",
    [email.trim()],
    (err, results) => {
      if (err) {
        console.error("Unsubscribe DB error:", err);
        return res.status(500).send("Internal server error");
      }
      if (results.affectedRows === 0) {
        return res.status(404).send("Email not found");
      }
      res.status(200).send("Successfully unsubscribed");
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});