const express = require("express");
const mysql = require("mysql");

const app = express();

// Security headers
app.disable("x-powered-by");
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Require DB config via env vars (no insecure defaults)
function requireEnv(name) {
  const v = process.env[name];
  if (!v) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return v;
}

const pool = mysql.createPool({
  host: requireEnv("DB_HOST"),
  user: requireEnv("DB_USER"),
  password: requireEnv("DB_PASSWORD"),
  database: requireEnv("DB_NAME"),
  connectionLimit: 10,
  charset: "utf8mb4",
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * - Validates and normalizes the email
 * - Uses a parameterized query to prevent SQL injection
 * - Returns a generic message to avoid email enumeration
 */
app.get("/unsubscribe", (req, res) => {
  const raw = typeof req.query.email === "string" ? req.query.email : "";
  const email = raw.trim().toLowerCase();

  // Basic, safe email validation (kept simple to avoid ReDoS)
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isValid = email && email.length <= 254 && emailRegex.test(email);

  if (!isValid) {
    // Generic response to avoid leaking whether an email exists
    return res.status(200).json({
      message: "If this address exists in our system, it has been unsubscribed.",
    });
  }

  // Unsubscribe if present (idempotent). Uses a parameterized query.
  const sql =
    "UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = 0)";

  pool.query(sql, [email], (err) => {
    if (err) {
      console.error("Database error during unsubscribe:", err);
      return res.status(500).json({ message: "Internal server error" });
    }

    // Always return a generic message (prevents enumeration)
    return res.status(200).json({
      message: "If this address exists in our system, it has been unsubscribed.",
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

// Graceful shutdown
process.on("SIGINT", () => {
  pool.end(() => process.exit(0));
});
process.on("SIGTERM", () => {
  pool.end(() => process.exit(0));
});
