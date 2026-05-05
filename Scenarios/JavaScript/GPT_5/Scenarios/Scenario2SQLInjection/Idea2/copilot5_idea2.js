const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
  // Only set HSTS if behind HTTPS (avoid issues in dev)
  if (req.secure) {
    res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  }
  next();
});

// MySQL connection pool (never hard-code credentials)
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

// Basic safe email validation
function validateEmail(raw) {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0 || trimmed.length > 254) return null;
  // Strict-ish RFC 5322 compatible simplified pattern
  const re = /^[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}$/i;
  if (!re.test(trimmed)) return null;
  return trimmed.toLowerCase();
}

// Avoid leaking internal errors
function safeError(res) {
  res.status(400).json({ success: false, message: "Invalid request" });
}

// Route: Unsubscribe by email passed as GET parameter (?email=)
app.get("/unsubscribe", (req, res) => {
  // Enforce a maximum length on raw query value to mitigate resource exhaustion
  const rawEmail = req.query.email;
  if (!rawEmail || rawEmail.length > 512) {
    return res.status(400).json({ success: false, message: "Bad email parameter" });
  }

  const email = validateEmail(rawEmail);
  if (!email) {
    return res.status(400).json({ success: false, message: "Invalid email format" });
  }

  // Use a correlation ID for internal tracing without exposing sensitive data
  const requestId = crypto.randomUUID();

  // Use parameterized queries to prevent SQL injection (CWE-89)
  // We mark the user as unsubscribed instead of deleting (auditing + idempotent)
  pool.query(
    "SELECT id, unsubscribed FROM subscribers WHERE email = ? LIMIT 1",
    [email],
    (selectErr, rows) => {
      if (selectErr) {
        return safeError(res);
      }
      if (!rows || rows.length === 0) {
        // Do not echo the email back (avoid XSS / info leakage)
        return res.status(200).json({ success: false, message: "Email not found", requestId });
      }

      const subscriber = rows[0];
      if (subscriber.unsubscribed === 1) {
        return res.status(200).json({ success: true, message: "Already unsubscribed", requestId });
      }

      pool.query(
        "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE id = ?",
        [subscriber.id],
        (updateErr) => {
          if (updateErr) {
            return safeError(res);
          }
          // Generic success response without reflecting input
            return res.status(200).json({ success: true, message: "Unsubscribed", requestId });
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
// Do not log sensitive data
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
