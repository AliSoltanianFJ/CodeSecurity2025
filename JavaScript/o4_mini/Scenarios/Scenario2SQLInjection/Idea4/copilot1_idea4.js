require('dotenv').config();
const express = require('express');
const mysql = require('mysql2/promise');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const validator = require('validator');

const app = express();

// Defense-in-depth HTTP headers
app.use(helmet({
  contentSecurityPolicy: {
    directives: { defaultSrc: ["'self'"] }
  },
  frameguard: { action: 'deny' },
  hsts: { maxAge: 63072000, preload: true },
  noSniff: true
}));

// Rate limiting to mitigate enumeration & brute force
const unsubscribeLimiter = rateLimit({
  windowMs: 60 * 1000,  // 1 minute window
  max: 30,              // limit each IP to 30 requests per window
  message: 'Too many unsubscribe attempts, please try again later'
});
app.use('/unsubscribe', unsubscribeLimiter);

// Database pool with least privilege credentials
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,       // user with only SELECT & UPDATE on subscribers
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 5,
  queueLimit: 0
});

/**
 * GET /unsubscribe?email=<email>
 *  • Validates & normalizes email input
 *  • Uses parameterized queries to prevent injection
 *  • Returns generic success message to avoid email-existence leaks
 */
app.get('/unsubscribe', async (req, res) => {
  const rawEmail = req.query.email;
  if (!rawEmail || !validator.isEmail(rawEmail)) {
    return res.status(400).json({ error: 'Invalid email format' });
  }
  const email = validator.normalizeEmail(rawEmail);

  try {
    // Check subscription status
    const [rows] = await pool.execute(
      'SELECT id FROM subscribers WHERE email = ? LIMIT 1',
      [email]
    );

    // Always return generic message
    if (rows.length > 0) {
      await pool.execute(
        'UPDATE subscribers SET subscribed = 0 WHERE email = ?',
        [email]
      );
    }
    return res.status(200).json({
      message: 'If the email is subscribed, it has been unsubscribed.'
    });
  } catch (err) {
    // Log minimal error details without sensitive info
    console.error('Unsubscribe failure:', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});