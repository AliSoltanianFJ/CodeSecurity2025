'use strict';

require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const mysql = require('mysql');
const validator = require('validator');

const app = express();

// Defense-in-depth: secure headers
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
    }
  },
  frameguard: { action: 'deny' },
  hsts: { maxAge: 31536000, includeSubDomains: true },
  referrerPolicy: { policy: 'no-referrer' }
}));

// Parse JSON bodies (for POST requests)
app.use(express.json());

// MySQL connection pool with least-privileged user
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,        // GRANT SELECT, UPDATE ON subscribers TO this user;
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

/**
 * Unsubscribe endpoint.
 * Uses POST (state-changing) with CSRF token expected in header X-CSRF-Token.
 * Validates and canonicalizes email, then marks unsubscribed status.
 */
app.post('/unsubscribe', async (req, res) => {
  try {
    const csrfToken = req.get('X-CSRF-Token');
    // TODO: verify csrfToken against session or signed cookie
    if (!csrfToken || csrfToken !== req.session?.csrfToken) {
      return res.status(403).json({ message: 'Forbidden' });
    }

    const rawEmail = req.body.email;
    if (!rawEmail || !validator.isEmail(rawEmail)) {
      return res.status(400).json({ message: 'Invalid email address' });
    }
    // canonicalize
    const email = validator.normalizeEmail(rawEmail, { gmail_remove_dots: false });

    // Parameterized SELECT
    pool.query(
      'SELECT id, unsubscribed FROM subscribers WHERE email = ? LIMIT 1',
      [email],
      (selectErr, results) => {
        if (selectErr) {
          console.error('DB error (select):', selectErr); 
          return res.status(500).json({ message: 'Internal server error' });
        }
        if (results.length === 0) {
          // Do not reveal if email existed
          return res.status(200).json({ message: 'If your email was subscribed, you are now unsubscribed.' });
        }
        if (results[0].unsubscribed) {
          return res.status(200).json({ message: 'You have already unsubscribed.' });
        }

        // Parameterized UPDATE
        pool.query(
          'UPDATE subscribers SET unsubscribed = 1 WHERE id = ?',
          [results[0].id],
          (updateErr) => {
            if (updateErr) {
              console.error('DB error (update):', updateErr);
              return res.status(500).json({ message: 'Internal server error' });
            }
            return res.status(200).json({ message: 'You have been unsubscribed successfully.' });
          }
        );
      }
    );
  } catch (err) {
    console.error('Unexpected error:', err);
    return res.status(500).json({ message: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});