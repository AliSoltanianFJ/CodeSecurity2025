require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const mysql = require('mysql');
const crypto = require('crypto');
const { query, validationResult } = require('express-validator');

const app = express();

// 1. Defense-in-depth headers
app.use(helmet());
app.use(helmet.frameguard({ action: 'deny' }));
app.use(helmet.hsts({ maxAge: 63072000, includeSubDomains: true, preload: true }));

// 2. Rate limiting to slow enumeration
app.use(rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
}));

// 3. Least-privilege DB pool
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT, 10) || 3306,
  user: process.env.DB_USER,           // restricted user: only SELECT/UPDATE on users(subscribed)
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: 'utf8mb4',
  timezone: 'Z',
});

// 4. Unsubscribe endpoint (note: GET for demo only; POST+CSRF recommended)
app.get(
  '/unsubscribe',
  // 4a. Validate & canonicalize email query parameter
  query('email').isEmail().normalizeEmail({ gmail_remove_dots: false }),
  async (req, res) => {
    // 4b. Reject bad input
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ error: 'Invalid request.' });
    }
    const email = req.query.email;

    try {
      // 4c. Parameterized SELECT
      pool.query(
        'SELECT id FROM users WHERE email = ? LIMIT 1',
        [email],
        (selectErr, results) => {
          if (selectErr) {
            console.error('DB SELECT error:', selectErr);
            return res.status(500).json({ error: 'Internal server error.' });
          }

          // 4d. Idempotent “not found” response
          if (results.length === 0) {
            return res.status(200).json({
              message: 'If the email existed, it has been unsubscribed.'
            });
          }

          const userId = results[0].id;
          // 4e. Parameterized UPDATE
          pool.query(
            'UPDATE users SET subscribed = ? WHERE id = ?',
            [false, userId],
            (updateErr) => {
              if (updateErr) {
                console.error('DB UPDATE error:', updateErr);
                return res.status(500).json({ error: 'Internal server error.' });
              }

              // 4f. Pseudonymized audit log
              const hash = crypto
                .createHash('sha256')
                .update(email)
                .digest('hex');
              console.info(`[UNSUBSCRIBE] email_hash=${hash} user_id=${userId}`);

              return res.status(200).json({
                message: 'You have been unsubscribed.'
              });
            }
          );
        }
      );
    } catch (fatalErr) {
      console.error('Unexpected error:', fatalErr);
      return res.status(500).json({ error: 'Internal server error.' });
    }
  }
);

const PORT = parseInt(process.env.PORT, 10) || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});