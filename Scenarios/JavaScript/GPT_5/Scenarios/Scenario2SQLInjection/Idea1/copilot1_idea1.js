'use strict';

const express = require('express');
const mysql = require('mysql2/promise');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Require DB config from env; avoid insecure defaults
function requiredEnv(name) {
  if (!process.env[name]) throw new Error(`Missing required env var: ${name}`);
  return process.env[name];
}

const pool = mysql.createPool({
  host: requiredEnv('DB_HOST'),
  user: requiredEnv('DB_USER'),
  password: requiredEnv('DB_PASSWORD'),
  database: requiredEnv('DB_NAME'),
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
});

function isValidEmail(email) {
  if (typeof email !== 'string') return false;
  const e = email.trim().toLowerCase();
  if (e.length < 6 || e.length > 254) return false;
  const basic = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return basic.test(e);
}

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * Uses parameterized queries and a generic response to avoid user enumeration.
 */
app.get("/unsubscribe", async (req, res) => {
  const email = typeof req.query.email === 'string' ? req.query.email.trim().toLowerCase() : '';

  // Always respond generically to avoid revealing whether the email exists
  const genericResponse = () =>
    res.status(200).json({ message: "If this email is subscribed, it has been unsubscribed." });

  if (!isValidEmail(email)) {
    return genericResponse();
  }

  let conn;
  try {
    conn = await pool.getConnection();
    await conn.beginTransaction();

    // Check if the email exists
    const [rows] = await conn.execute(
      'SELECT id, unsubscribed_at FROM subscribers WHERE email = ? LIMIT 1',
      [email]
    );

    // If exists and not yet unsubscribed, mark it unsubscribed
    if (rows.length === 1) {
      const { id, unsubscribed_at } = rows[0];
      if (!unsubscribed_at) {
        await conn.execute(
          'UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ?',
          [id]
        );
      }
    }

    await conn.commit();
    return genericResponse();
  } catch (err) {
    if (conn) {
      try { await conn.rollback(); } catch {}
    }
    // Do not leak sensitive error details
    console.error('Unsubscribe error:', err?.message || err);
    return genericResponse();
  } finally {
    if (conn) conn.release();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
