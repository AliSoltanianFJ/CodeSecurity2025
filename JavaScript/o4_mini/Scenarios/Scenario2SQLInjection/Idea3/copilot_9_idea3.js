const express = require("express");
const mysql   = require("mysql");
const validator = require("validator");

const app = express();

// set up a connection pool
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME
});

// Mitigate click-jacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * GET /unsubscribe?email=user@example.com
 *
 * Validates the email, then marks it as unsubscribed.
 * Returns a generic success message even if the email wasn’t found.
 */
app.get("/unsubscribe", (req, res) => {
  // Prevent caching/unwanted pre-fetching
  res.setHeader('Cache-Control', 'no-store');

  const rawEmail = req.query.email;
  if (!rawEmail) {
    // Bad request if no email provided
    return res.status(400).send("Please provide an email address.");
  }

  const email = rawEmail.trim().toLowerCase();
  if (!validator.isEmail(email)) {
    // Don’t reveal too much detail
    return res.status(400).send("Please provide a valid email address.");
  }

  // Parameterized query to prevent SQL injection
  const sql = "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?";
  pool.query(sql, [email], (err, result) => {
    if (err) {
      console.error("DB error on unsubscribe:", err);
      // Generic 5xx so we don’t leak schema or SQL error details
      return res.status(500).send("Internal server error. Please try again later.");
    }

    // Whether affectedRows is 0 or >0, we still return the same message
    // to avoid letting an attacker probe which emails you have.
    return res
      .status(200)
      .send("If that email was on our list, it has now been unsubscribed.");
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});