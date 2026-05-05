const express = require("express");
const helmet = require("helmet");
const mysql = require("mysql");
const rateLimit = require("express-rate-limit");
const { query, validationResult } = require("express-validator");
require("dotenv").config();

const app = express();

// 1. Secure headers (CSP, HSTS, X-Frame-Options, etc.)
app.use(helmet({
  contentSecurityPolicy: {
    directives: { defaultSrc: ["'self'"] }
  },
  frameguard: { action: "deny" },
}));

// 2. Rate limiting to mitigate abuse & enumeration
app.use("/unsubscribe", rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 5,
  message: { error: "Too many requests, please slow down." }
}));

// 3. Database pool with least-privilege credentials
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,       // dedicated unsubscribe-only DB user
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
});

// 4. Unsubscribe endpoint with validation & parameterized queries
app.get("/unsubscribe",
  query("email")
    .isEmail().withMessage("Invalid email format.")
    .normalizeEmail(),
  (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: errors.array()[0].msg });
    }

    const email = req.query.email;  // already canonicalized

    pool.getConnection((connErr, conn) => {
      if (connErr) {
        console.error("DB connection error"); 
        return res.status(500).json({ error: "Internal server error." });
      }

      // Lookup subscriber by email
      conn.query(
        "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
        [email],
        (selectErr, results) => {
          if (selectErr) {
            conn.release();
            console.error("DB select error");
            return res.status(500).json({ error: "Internal server error." });
          }
          if (results.length === 0) {
            conn.release();
            return res.status(404).json({ message: "Email not found." });
          }

          // Perform secure unsubscribe (defense-in-depth)
          conn.query(
            "UPDATE subscribers SET subscribed = 0 WHERE id = ?",
            [results[0].id],
            (updateErr) => {
              conn.release();
              if (updateErr) {
                console.error("DB update error");
                return res.status(500).json({ error: "Internal server error." });
              }
              res.json({ message: "Successfully unsubscribed." });
            }
          );
        }
      );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});