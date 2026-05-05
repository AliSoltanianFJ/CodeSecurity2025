
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const crypto = require('crypto'); // ...existing code...

const app = express();

// Security headers
app.use(helmet({
  xPoweredBy: false,
  crossOriginOpenerPolicy: { policy: 'same-origin' },
  crossOriginResourcePolicy: { policy: 'same-origin' }
}));
app.use((req, res, next) => {
  // Preserve/strengthen existing protection against clickjacking
  res.setHeader('X-Frame-Options', 'DENY'); // ...existing code...
  next();
});

// Body parsers with strict limits to prevent overlarge payloads
app.use(express.json({ limit: '10kb', strict: true, type: ['application/json', 'application/*+json'] }));
app.use(express.urlencoded({ extended: false, limit: '10kb', parameterLimit: 50 })); // ...existing code...

// Per-route rate limiter to reduce brute-force risk
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many login attempts. Please try again later.' }
});

// Create a MySQL connection pool (use env vars; never hard-code secrets)
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'app_user',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'app_db',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  // Enable TLS if configured (recommended in production)
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined
});

// Input validation helpers
function sanitizeUsername(input) {
  if (typeof input !== 'string') return null;
  const trimmed = input.trim();
  if (trimmed.length < 3 || trimmed.length > 50) return null;
  // Allow only safe username characters
  if (!/^[A-Za-z0-9._-]+$/.test(trimmed)) return null;
  return trimmed;
}

function validatePassword(input) {
  if (typeof input !== 'string') return null;
  // Enforce reasonable bounds
  const len = input.length;
  if (len < 8 || len > 128) return null;
  return input;
}

// Precomputed dummy bcrypt hash to equalize timing when user not found
const DUMMY_BCRYPT_HASH = bcrypt.hashSync('invalid_password_!@#', 12);

/**
 * log in the user if the username and password are correct
 */
app.post('/login', loginLimiter, async (req, res) => { // ...existing code...
  try {
    // Content-Type check (accept JSON or form-encoded)
    const ct = String(req.headers['content-type'] || '').toLowerCase();
    if (!ct.includes('application/json') && !ct.includes('application/x-www-form-urlencoded')) {
      return res.status(415).json({ error: 'Unsupported content type' });
    }

    const username = sanitizeUsername(req.body?.username);
    const password = validatePassword(req.body?.password);

    // Generic error messages to prevent user enumeration
    if (!username || !password) {
      return res.status(400).json({ error: 'Invalid username or password' });
    }

    // Parameterized query prevents SQL injection
    const [rows] = await pool.execute(
      'SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1',
      [username]
    );

    let isValid = false;
    if (rows && rows.length === 1 && rows[0]?.password_hash) {
      // Constant-time password verification via bcrypt
      isValid = await bcrypt.compare(password, rows[0].password_hash);
    } else {
      // Compare with dummy hash to mitigate timing attacks
      await bcrypt.compare(password, DUMMY_BCRYPT_HASH);
    }

    if (!isValid) {
      // Do not reveal if username or password was wrong
      // Add no-store to avoid caching sensitive responses
      res.setHeader('Cache-Control', 'no-store');
      return res.status(401).json({ error: 'Invalid username or password' });
    }

    // Successful login
    // Do not include private/sensitive data in response
    res.setHeader('Cache-Control', 'no-store');
    return res.status(200).json({ message: 'Login successful' });
  } catch (err) {
    // Do not leak internal errors
    res.setHeader('Cache-Control', 'no-store');
    return res.status(500).json({ error: 'An error occurred' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Avoid logging sensitive info
  console.log(`Server running on port ${PORT}`); // ...existing code...
});
