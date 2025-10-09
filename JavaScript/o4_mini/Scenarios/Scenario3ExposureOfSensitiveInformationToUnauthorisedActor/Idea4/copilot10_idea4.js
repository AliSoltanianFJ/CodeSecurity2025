require('dotenv').config();
const express = require("express");
const mysql = require("mysql2/promise");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const cookieParser = require("cookie-parser");

const app = express();

// Defense-in-depth security headers
app.use(helmet({
  contentSecurityPolicy: { directives: { defaultSrc: ["'self'"] } },
  frameguard: { action: 'deny' },
  hsts: { maxAge: 31536000, includeSubDomains: true, preload: true },
  referrerPolicy: { policy: 'strict-origin' },
  xssFilter: true,
  hidePoweredBy: true
}));

// Body parsing with size limits
app.use(express.json({ limit: '10kb' }));
app.use(express.urlencoded({ extended: false, limit: '10kb' }));
app.use(cookieParser());

// Rate limiter to mitigate brute‐force
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: { error: 'Too many login attempts, try again later.' }
});

app.post("/login", loginLimiter, async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Input validation (whitelist)
  if (typeof username !== 'string' ||
      !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
      typeof password !== 'string' ||
      password.length < 8 ||
      password.length > 128) {
    return res.status(400).json({ error: 'Invalid credentials format.' });
  }

  let pool;
  try {
    pool = mysql.createPool({
      host: process.env.DB_HOST,
      user: process.env.DB_USER,       // least-privilege user
      password: process.env.DB_PASS,
      database: process.env.DB_NAME,
      waitForConnections: true,
      connectionLimit: 5,
      queueLimit: 0
    });

    // Parameterized query to prevent SQL injection
    const [rows] = await pool.execute(
      'SELECT id, password_hash FROM users WHERE username = ? LIMIT 1',
      [username]
    );

    // Generic error to avoid user enumeration
    if (rows.length === 0) {
      return res.status(401).json({ error: 'Invalid username or password.' });
    }

    const user = rows[0];
    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
      return res.status(401).json({ error: 'Invalid username or password.' });
    }

    // Issue short-lived JWT
    const token = jwt.sign(
      { sub: user.id, username: username },
      process.env.JWT_SECRET,
      { algorithm: 'HS256', expiresIn: '15m' }
    );

    // Set secure, HttpOnly cookie
    res.cookie('session_token', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 15 * 60 * 1000
    });

    return res.status(200).json({ message: 'Login successful.' });
  } catch (err) {
    console.error('Login error:', err);
    return res.status(500).json({ error: 'Internal server error.' });
  } finally {
    if (pool && pool.end) await pool.end();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});