"use strict";
const express = require("express");
const mysql = require("mysql"); // Consider mysql2 for improved maintenance.
const crypto = require("crypto");

const app = express();

// Minimal security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none';");
  next();
});

// Connection pool (no credentials exposed; use env vars)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "app_db",
  charset: "utf8mb4",
  multipleStatements: false
});

// Input validation helpers
const EMAIL_REGEX = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
function isValidEmail(email) {
  if (typeof email !== "string") return false;
  if (email.length === 0 || email.length > 254) return false;
  if (!EMAIL_REGEX.test(email)) return false;
  return true;
}

// Safe generic response to avoid enumeration
function genericOk(res) {
  res.status(200).json({ status: "ok" });
}

// Unsubscribe endpoint
/**
 * Safely unsubscribes an email if present without disclosing existence.
 * GET /unsubscribe?email=someone@example.com
 */
app.get("/unsubscribe", (req, res) => {
  // Extract and sanitize
  let email = req.query.email;
  if (!isValidEmail(email)) {
    return res.status(400).json({ error: "Invalid input" });
  }

  // Normalize
  email = email.trim().toLowerCase();

  // Defensive size check (prevents oversized input abuse)
  if (email.length > 254) {
    return res.status(400).json({ error: "Invalid input" });
  }

  // Avoid reflecting user input directly (no XSS surface)
  // Use parameterized queries to prevent SQL injection (CWE-89)
  const selectSql = "SELECT id FROM subscribers WHERE email = ? AND active = 1 LIMIT 1";
  pool.query(selectSql, [email], (err, rows) => {
    if (err) {
      // Generic error; do not leak internals
      return res.status(500).json({ error: "Internal error" });
    }
    if (!rows || rows.length === 0) {
      // Do not reveal existence
      return genericOk(res);
    }
    const updateSql = "UPDATE subscribers SET active = 0, unsubscribed_at = NOW(), unsubscribe_token = ? WHERE id = ? AND active = 1";
    // Store a random token for audit / potential re-subscribe verification (no PII disclosure)
    const token = crypto.randomBytes(24).toString("hex");
    pool.query(updateSql, [token, rows[0].id], (uErr) => {
      if (uErr) {
        return res.status(500).json({ error: "Internal error" });
      }
      return genericOk(res);
    });
  });
});

// Generic not found handler (avoid stack leaks)
app.use((req, res) => {
  res.status(404).json({ error: "Not found" });
});

// Minimal error handler
app.use((err, req, res, next) => {
  res.status(500).json({ error: "Internal error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log sensitive data
  console.log(`Server running on port ${PORT}`);
});

/*
Security notes:
- Parameterized queries eliminate SQL injection.
- No direct reflection of user-supplied email (prevents XSS).
- Input length & pattern validation mitigate malformed input and resource abuse.
- No deserialization of user-controlled data.
- Headers reduce clickjacking, MIME sniffing, and framing risks.
- Generic responses prevent user enumeration of subscriber list.
- JS runtime memory safety averts classic buffer overflows.
*/
