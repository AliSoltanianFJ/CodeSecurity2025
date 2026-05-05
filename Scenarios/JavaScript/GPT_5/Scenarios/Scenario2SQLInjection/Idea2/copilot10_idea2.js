require('dotenv').config();
const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const crypto = require("crypto");
const { createPool } = require("mysql2/promise");

const app = express();


app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  next();
});
app.disable('x-powered-by');

// Basic rate limiting
app.use(rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false
}));

// DB pool (use environment variables; never hardcode secrets)
const pool = createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  maxIdle: 10,
  idleTimeout: 60000,
  queueLimit: 0,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
});

// Utility: safe JSON response
function safeJson(res, status, payload) {
  res.status(status).type("application/json; charset=utf-8").send(JSON.stringify(payload));
}

/**
 * Unsubscribe endpoint:
 * Accepts GET ?email=...
 * Validates and uses parameterized queries to avoid SQL injection.
 * Returns generic response to prevent email enumeration.
 */
app.get("/unsubscribe", async (req, res) => {
  const rawEmail = req.query.email;

  // Validate presence and type
  if (typeof rawEmail !== "string") {
    return safeJson(res, 400, { status: "error", message: "Invalid request" });
  }

  // Trim and basic sanitation
  const email = rawEmail.trim();

  // Enforce length bounds (RFC 5321/5322 max 254)
  if (email.length < 5 || email.length > 254) {
    return safeJson(res, 400, { status: "error", message: "Invalid email" });
  }

  // Strict email pattern (basic; does not allow unusual forms)
  const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
  if (!emailRegex.test(email)) {
    return safeJson(res, 400, { status: "error", message: "Invalid email" });
  }

  // Hash for logging without exposing PII
  const emailHash = crypto.createHash("sha256").update(email).digest("hex");
  console.info("Unsubscribe attempt:", emailHash);

  try {
    const conn = await pool.getConnection();
    try {
      // Parameterized select
      const [rows] = await conn.execute(
        "SELECT id, unsubscribed FROM subscribers WHERE email = ? LIMIT 1",
        [email]
      );

      if (rows.length === 1 && rows[0].unsubscribed !== 1) {
        await conn.execute(
          "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE id = ?",
          [rows[0].id]
        );
      }
    } finally {
      conn.release();
    }

    // Generic response (avoid enumeration / timing leaks)
    return safeJson(res, 200, {
      status: "ok",
      message: "If the address was subscribed it has now been unsubscribed."
    });
  } catch (err) {
    // Do not leak internal error details
    console.error("Internal unsubscribe error:", err && err.message);
    return safeJson(res, 500, { status: "error", message: "Internal error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
