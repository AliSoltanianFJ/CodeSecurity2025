"use strict";
const express = require("express");
const mysql = require("mysql");

const app = express();
app.disable("x-powered-by");

// Basic security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Permissions-Policy", "geolocation=()");
  next();
});

// DB connection pool (do not hardcode secrets)
const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  charset: "utf8mb4",
  supportBigNumbers: true,
  multipleStatements: false
});

// Simple safe email validator
function validateEmail(raw) {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0 || trimmed.length > 254) return null;
  // RFC 5322 simplified
  const re = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,63}$/;
  if (!re.test(trimmed)) return null;
  return trimmed.toLowerCase();
}

// Generic safe JSON response
function sendOk(res) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.json({ status: "ok" });
}

/**
 * Unsubscribe endpoint: checks if email exists and marks unsubscribed.
 * Does not reveal whether email was present (privacy).
 */
app.get("/unsubscribe", (req, res) => {
  const email = validateEmail(req.query.email);

  if (!email) {
    // Return generic ok to avoid enumeration, but still reject malformed internally
    return sendOk(res);
  }

  // Defensive length cap (mitigate any atypical engine issues)
  if (email.length > 254) {
    return sendOk(res);
  }

  // Parameterized update (no SQL injection)
  const sql = "UPDATE users SET subscribed = 0, updated_at = NOW() WHERE email = ? AND subscribed = 1 LIMIT 1";

  // Use pool to avoid resource exhaustion
  pool.query(sql, [email], (err) => {
    // Do not leak details
    return sendOk(res);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Minimal log without sensitive info
  console.log("Server running");
});
