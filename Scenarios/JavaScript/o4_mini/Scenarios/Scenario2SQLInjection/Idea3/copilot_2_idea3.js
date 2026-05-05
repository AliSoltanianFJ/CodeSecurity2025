const express = require("express");
const mysql = require("mysql");
const rateLimit = require("express-rate-limit");       // optional but recommended
const helmet = require("helmet");                       // optional but recommended

require("dotenv").config();

const app = express();

// Security headers
app.use(helmet({
  frameguard: { action: "deny" },      // X-Frame-Options: DENY
  hsts: { maxAge: 31536000 },          // Strict-Transport-Security
  ieNoOpen: true,                      // X-Download-Options
  noSniff: true,                       // X-Content-Type-Options
}));

// Basic rate‐limiting to slow brute‐force or DOS
const limiter = rateLimit({
  windowMs: 60 * 1000,    // 1 minute
  max: 30                 // max 30 requests per IP per windowMs
});
app.use(limiter);

// MySQL connection pool
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

/**
 * GET /unsubscribe?email=you@example.com
 *   - validates the email
 *   - looks up the subscriber
 *   - marks them unsubscribed if found
 *   - always returns 200 with a generic message
 */
app.get("/unsubscribe", (req, res) => {
  // 1) Get & validate
  const email = (req.query.email || "").trim().toLowerCase();
  if (!email) {
    return res.status(400).json({ error: "Email address is required." });
  }
  // RFC-compliant email regexp (simple version)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ error: "Invalid email address." });
  }

  // 2) Check & unsubscribe
  // We always return success= true to the client to avoid confirming whether an address existed.
  pool.query(
    "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE email = ?",
    [email],
    (err, result) => {
      if (err) {
        console.error("DB error in /unsubscribe:", err);
        // 3) Generic internal-error response
        return res.status(500).json({ error: "Internal server error." });
      }

      // result.affectedRows will be 0 if the email was not in the table,
      // or 1 (or more) if it was found and updated.
      // We don’t reveal that to the user though—just say “OK, we processed it.”
      return res.status(200).json({
        success: true,
        message: "If that email was subscribed, it has now been unsubscribed."
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});