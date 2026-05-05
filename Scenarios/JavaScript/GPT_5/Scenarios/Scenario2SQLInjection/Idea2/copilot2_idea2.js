const express = require("express");
const mysql = require("mysql");

const app = express();

// Global security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("X-XSS-Protection", "0"); // modern browsers rely on CSP
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none';");
  res.setHeader("Cache-Control", "no-store");
  next();
});

// Use environment variables for DB credentials (never hardcode)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4",
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined
});

// Basic pool error logging (no sensitive info)
pool.on("error", () => {
  // Intentionally minimal; avoid leaking details
});

// Helper: validate and normalize email
function validateEmail(input) {
  if (typeof input !== "string") return null;
  // Reject arrays / polluted params
  if (Array.isArray(input)) return null;
  const trimmed = input.trim().toLowerCase();
  if (trimmed.length === 0 || trimmed.length > 254) return null;
  // Conservative RFC5322-compatible simplified pattern
  const emailRegex = /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i;
  if (!emailRegex.test(trimmed)) return null;
  return trimmed;
}

/**
 * Unsubscribe endpoint
 * GET /unsubscribe?email=example@domain.com
 * Secure against SQL injection (prepared statements), XSS (no reflection),
 * parameter pollution, and info leakage.
 */
app.get("/unsubscribe", (req, res) => {
  const rawEmail = req.query.email;
  const email = validateEmail(rawEmail);
  if (!email) {
    return res.status(400).json({ status: "error", message: "Invalid request." });
  }

  // Check existence using parameterized query
  pool.query("SELECT id FROM subscribers WHERE email = ? LIMIT 1", [email], (err, rows) => {
    if (err) {
      return res.status(500).json({ status: "error", message: "Internal error." });
    }
    if (!rows || rows.length === 0) {
      // Do not disclose whether it ever existed
      return res.status(200).json({ status: "ok" });
    }

    // Mark as unsubscribed (idempotent)
    pool.query(
      "UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ?",
      [email],
      (updateErr) => {
        if (updateErr) {
          return res.status(500).json({ status: "error", message: "Internal error." });
        }
        return res.status(200).json({ status: "ok" });
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Generic startup log
  console.log(`Server running on port ${PORT}`);
});
