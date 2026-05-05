const express = require("express");
const mysql   = require("mysql");
const validator = require("validator");

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  next();
});

// Create a pool of connections (adjust config to your environment)
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST     || 'localhost',
  user            : process.env.DB_USER     || 'your_db_user',
  password        : process.env.DB_PASSWORD || 'your_db_pass',
  database        : process.env.DB_NAME     || 'your_db_name',
  charset         : 'utf8mb4'
});

/**
 * GET /unsubscribe?email=foo@bar.com
 *
 * If the email exists in the `subscribers` table, sets `is_subscribed = 0`.
 */
app.get("/unsubscribe", (req, res) => {
  const email = (req.query.email || "").trim();

  // 1) Basic validation
  if (!email) {
    return res.status(400).send({ error: "Missing email address" });
  }
  if (!validator.isEmail(email)) {
    return res.status(400).send({ error: "Invalid email format" });
  }

  // 2) Check if email exists
  const selectSql = "SELECT id, is_subscribed FROM subscribers WHERE email = ? LIMIT 1";
  pool.query(selectSql, [email], (selectErr, rows) => {
    if (selectErr) {
      console.error("DB SELECT error:", selectErr);
      return res.status(500).send({ error: "Internal server error" });
    }

    if (rows.length === 0) {
      // 3a) Not found
      return res.status(404).send({ error: "Email not found" });
    }

    const subscriber = rows[0];
    if (subscriber.is_subscribed === 0) {
      // Already unsubscribed
      return res.status(200).send({ message: "You are already unsubscribed." });
    }

    // 3b) Email found and currently subscribed → unsubscribe
    const updateSql = "UPDATE subscribers SET is_subscribed = 0, updated_at = NOW() WHERE id = ?";
    pool.query(updateSql, [subscriber.id], (updateErr, result) => {
      if (updateErr) {
        console.error("DB UPDATE error:", updateErr);
        return res.status(500).send({ error: "Internal server error" });
      }
      if (result.affectedRows === 0) {
        // This should never happen—defensive check
        return res.status(500).send({ error: "Failed to unsubscribe. Please try again later." });
      }

      // 4) Success
      return res.status(200).send({ message: "You have been successfully unsubscribed." });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});