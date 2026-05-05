// ...existing code...
const express = require("express");
const helmet = require("helmet");
const mysql = require("mysql2/promise");
const validator = require("validator");
require("dotenv").config();
// ...existing code...

const app = express();

// Security hardening
app.use(helmet({
  frameguard: { action: 'deny' },
  xssFilter: true,
  noSniff: true,
  hidePoweredBy: true,
}));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  next();
});

// Create a pooled DB connection (env vars must be set; no hard‑coded secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT ? Number(process.env.DB_PORT) : 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  waitForConnections: true,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
});

// Basic health endpoint (optional minimal exposure)
app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

/**
 * Unsubscribe endpoint (GET ?email=)
 * Safely validates and processes an unsubscribe without revealing subscription existence.
 */
app.get("/unsubscribe", async (req, res) => {
  try {
    // Extract and strictly validate input
    const rawEmail = req.query.email;

    if (typeof rawEmail !== "string") {
      return res.status(400).json({ status: "error", message: "Invalid request." });
    }

    // Trim & normalize
    const trimmed = rawEmail.trim();

    // Enforce length bounds (RFC 5321 max 254)
    if (trimmed.length === 0 || trimmed.length > 254) {
      return res.status(400).json({ status: "error", message: "Invalid email." });
    }

    // Validate structure
    if (!validator.isEmail(trimmed, { allow_utf8_local_part: false })) {
      return res.status(400).json({ status: "error", message: "Invalid email." });
    }

    const email = validator.normalizeEmail(trimmed);
    if (!email) {
      return res.status(400).json({ status: "error", message: "Invalid email." });
    }

    // Use a transaction for consistency
    const conn = await pool.getConnection();
    try {
      await conn.beginTransaction();

      // Parameterized query prevents SQL injection (CWE-89)
      const [rows] = await conn.execute(
        "SELECT id FROM subscribers WHERE email = ? AND unsubscribed = 0 LIMIT 1",
        [email]
      );

      if (rows.length > 0) {
        // Soft unsubscribe to retain audit (avoid insecure deletion practices)
        await conn.execute(
          "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = CURRENT_TIMESTAMP WHERE id = ?",
          [rows[0].id]
        );
      }

      await conn.commit();
      // Generic response (avoid user enumeration / info leakage)
      return res.status(200).json({ status: "ok", message: "If subscribed, the address will be unsubscribed." });
    } catch (e) {
      await conn.rollback();
      // Do not leak internal error details
      return res.status(500).json({ status: "error", message: "Internal error." });
    } finally {
      conn.release();
    }
  } catch {
    return res.status(500).json({ status: "error", message: "Internal error." });
  }
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Non-sensitive log
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
