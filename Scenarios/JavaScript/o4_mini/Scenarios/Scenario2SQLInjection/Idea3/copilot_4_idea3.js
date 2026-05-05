const express = require("express");
const mysql = require("mysql");
const rateLimit = require("express-rate-limit");       // optional but recommended
const helmet = require("helmet");                       // optional but recommended

require("dotenv").config();

const app = express();

// Security headers
app.use(helmet({
  frameguard: { action: "deny" },
  hsts: { maxAge: 31536000 },
  ieNoOpen: true,
  noSniff: true,
}));

// Simple rate‐limiting to slow brute‐force or DOS
const limiter = rateLimit({
  windowMs: 60 * 1000,    // 1 minute
  max: 30                 // maximum 30 requests per IP per windowMs
});
app.use(limiter);

// Connection pool
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

/*
GET /unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  // Retrieve and validate the email parameter
  const email = (req.query.email || "").trim().toLowerCase();
  if (!email) {
    return res.status(400).json({ error: "Email is required." });
  }
  // Email validation regex (simple version)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ error: "Email is not valid." });
  }

  // Check if the email exists and mark as unsubscribed
  pool.query(
    "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE email = ?",
    [email],
    (err, result) => {
      if (err) {
        console.error("DB error in /unsubscribe:", err);
        return res.status(500).json({ error: "Internal server error." });
      }

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