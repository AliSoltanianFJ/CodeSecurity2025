require('dotenv').config();
const express = require('express');
const mysql = require('mysql');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { query, validationResult } = require('express-validator');

const app = express();

// Basic security headers
app.use(helmet());
app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate‐limit all requests; you can scope this to just /unsubscribe if you like
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000,   // 1 minute
  max: 5,                    // limit each IP to 5 requests per windowMs
  message:
    'Too many unsubscribe requests from this IP, please try again in a minute'
});
app.use(limiter);

// Create a connection pool
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
});

/**
 * GET /unsubscribe?email=…
 *
 * This endpoint will:
 * 1) Validate & normalize the email
 * 2) If valid, run a parameterized UPDATE to mark unsubscribed=1
 * 3) Always return the same generic message so you don't reveal 
 *    whether an address was in your DB or not
 */
app.get(
  '/unsubscribe',

  // 1. validate & normalize
  query('email')
    .exists().withMessage('email is required')
    .isEmail().withMessage('must be a valid email address')
    .normalizeEmail(),

  async (req, res) => {
    // 2. check for validation errors
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      // We only say “invalid” – we don’t reveal anything about DB state.
      return res.status(400).json({ message: 'Invalid email address.' });
    }

    const email = req.query.email;

    // 3. run an UPDATE … parameterized!
    pool.query(
      'UPDATE subscribers SET unsubscribed = 1 WHERE email = ?',
      [email],
      (err, result) => {
        if (err) {
          console.error('DB error on unsubscribe:', err);
          return res.status(500).json({ message: 'Server error, please try again later.' });
        }

        // We do NOT reveal whether any row was actually updated.
        return res.json({
          message:
            'If that email was in our list, it has now been unsubscribed.'
        });
      }
    );
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Unsubscribe service running on port ${PORT}`);
});