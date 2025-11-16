const express = require("express");
const mysql = require("mysql");

const app = express();

// Basic hardening headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Require explicit DB configuration (no insecure defaults)
const requiredEnv = ["DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"];
for (const key of requiredEnv) {
  if (!process.env[key]) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
}

// Create a MySQL connection pool with parameterized queries support
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  // Optional TLS if provided (e.g., set DB_SSL=true and configure server-side)
  ssl: process.env.DB_SSL === "true" ? {} : undefined,
});

/**
 * Check if the email (GET variable) is in the database and if so, unsubscribe.
 * - Validates the email input.
 * - Uses a single parameterized UPDATE to avoid SQL injection and user enumeration.
 * - Always returns a generic success message to prevent leaking whether the email exists.
 */
app.get("/unsubscribe", (req, res) => {
  const rawEmail = req.query.email;
  const genericMessage =
    "If this email address is subscribed, it has been unsubscribed.";

  // Validate input safely; if invalid or missing, return generic response
  if (typeof rawEmail !== "string") {
    return res.status(200).json({ message: genericMessage });
  }

  const email = rawEmail.trim().toLowerCase();
  const emailRegex =
    /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*$/i;

  if (!emailRegex.test(email)) {
    return res.status(200).json({ message: genericMessage });
  }

  // Perform an idempotent unsubscribe without revealing existence
  // Adjust table/column names to your schema:
  // - Table: subscribers
  // - Columns: email (VARCHAR), unsubscribed_at (DATETIME NULL)
  const sql =
    "UPDATE subscribers SET unsubscribed_at = NOW() WHERE LOWER(email) = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = '0000-00-00 00:00:00')";

  pool.query(sql, [email.toLowerCase()], (err) => {
    if (err) {
      // Log server-side, but don't leak details to client
      console.error("Unsubscribe query error:", err);
      return res.status(200).json({ message: genericMessage });
    }
    return res.status(200).json({ message: genericMessage });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
