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

// Add rate‐limiting to prevent abuse (adjust as needed)
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000,   // 1 minute
  max: 5,                    // Set max requests per IP per windowMs
  message:
    'Too many unsubscribe requests from this IP, please try again later'
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
 */
app.get(
  '/unsubscribe',

  // Validate & normalize the email query parameter
  query('email')
    .exists().withMessage('email is required')
    .isEmail().withMessage('must be a valid email address')
    .normalizeEmail(),

  async (req, res) => {
    // Extract validation errors
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ message: 'Invalid email address.' });
    }

    const email = req.query.email;

    // Run a parameterized query to mark the email as unsubscribed
    pool.query(
      'UPDATE subscribers SET unsubscribed = 1 WHERE email = ?',
      [email],
      (err, result) => {
        if (err) {
          console.error('DB error on unsubscribe:', err);
          return res.status(500).json({ message: 'Server error, please try again later.' });
        }

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