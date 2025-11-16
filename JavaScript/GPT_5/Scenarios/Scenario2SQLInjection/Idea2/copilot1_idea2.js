const express = require("express");
const mysql = require("mysql");

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Content-Security-Policy', "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
  next();
});

// MySQL connection pool (use environment variables; never hard-code secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: 'utf8mb4',
  multipleStatements: false,
  acquireTimeout: 10000
});

// Simple, conservative email regex (avoid overly permissive patterns)
const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}$/;

/**
 * Unsubscribe endpoint:
 * - Validates and normalizes the email (GET variable: ?email=)
 * - Uses parameterized queries to prevent SQL injection (CWE-89)
 * - Does not reveal whether an email exists (privacy)
 * - Avoids reflecting user input (XSS prevention - CWE-79)
 */
app.get("/unsubscribe", (req, res) => {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');

  // Extract raw input
  const rawEmail = req.query.email;

  // Basic type & presence check
  if (typeof rawEmail !== 'string') {
    return res.status(400).json({ status: 'error', message: 'Invalid request.' });
  }

  // Normalize & length guard (prevent resource misuse / oversized input)
  const email = rawEmail.trim().toLowerCase();

  if (email.length === 0 || email.length > 254 || !EMAIL_REGEX.test(email)) {
    return res.status(400).json({ status: 'error', message: 'Invalid email format.' });
  }

  // Query user safely
  pool.query(
    'SELECT id, subscribed FROM users WHERE email = ? LIMIT 1',
    [email],
    (selectErr, results) => {
      if (selectErr) {
        // Generic error message (avoid leaking internals)
        return res.status(500).json({ status: 'error', message: 'Internal error.' });
      }

      // If user not found or already unsubscribed, respond generically (no enumeration)
      if (!Array.isArray(results) || results.length === 0 || results[0].subscribed === 0) {
        return res.json({
          status: 'ok',
            // Generic response to prevent email existence disclosure
          message: 'If this email was subscribed, it is now unsubscribed.'
        });
      }

      const userId = results[0].id;

      // Perform unsubscribe using parameterized update
      pool.query(
        'UPDATE users SET subscribed = 0, unsubscribed_at = NOW() WHERE id = ?',
        [userId],
        (updateErr) => {
          if (updateErr) {
            return res.status(500).json({ status: 'error', message: 'Internal error.' });
          }
          return res.json({
            status: 'ok',
            message: 'If this email was subscribed, it is now unsubscribed.'
          });
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Do not log sensitive data
  console.log(`Server running on port ${PORT}`);
});
