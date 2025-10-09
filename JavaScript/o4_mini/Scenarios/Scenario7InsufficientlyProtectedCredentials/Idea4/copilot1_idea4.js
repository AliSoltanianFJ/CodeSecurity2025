require('dotenv').config();
const express       = require('express');
const helmet        = require('helmet');
const rateLimit     = require('express-rate-limit');
const cookieParser  = require('cookie-parser');
const csurf         = require('csurf');
const mysql         = require('mysql2/promise');
const bcrypt        = require('bcrypt');
const { body, validationResult } = require('express-validator');

const app = express();

// Defense-in-depth middlewares
app.use(helmet());                                   // sets HSTS, CSP, X-Frame-Options, etc.
app.use(express.urlencoded({ extended: false }));    // strict body parsing
app.use(cookieParser());                             // needed for CSRF
app.use(rateLimit({                                  // simple brute-force protection
  windowMs: 15 * 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false
}));
const csrfProtection = csurf({ cookie: { httpOnly: true, sameSite: 'strict' } });

// Least-privilege DB pool
const pool = mysql.createPool({
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,      // limited-privilege user
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit : 5,
  queueLimit      : 0
});

/**
 * register URL
 */
app.post(
  '/register',
  csrfProtection,
  // Input validation & sanitization
  body('username')
    .trim()
    .isLength({ min: 3, max: 30 })
    .matches(/^[A-Za-z0-9_]+$/)
    .withMessage('Username must be alphanumeric (3–30 chars)'),
  body('password')
    .isLength({ min: 12 })
    .withMessage('Password ≥12 chars')
    .matches(/[A-Z]/).withMessage('Must include uppercase')
    .matches(/[a-z]/).withMessage('Must include lowercase')
    .matches(/[0-9]/).withMessage('Must include digit')
    .matches(/[@$!%*?&]/).withMessage('Must include special char'),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    const { username, password } = req.body;
    try {
      // bcrypt hashing with cost factor 12
      const passwordHash = await bcrypt.hash(password, 12);

      // parameterized INSERT
      const conn = await pool.getConnection();
      try {
        await conn.execute(
          'INSERT INTO users (username, password_hash) VALUES (?, ?)',
          [username, passwordHash]
        );
      } finally {
        conn.release();
      }

      res.status(201).json({ message: 'User registered successfully' });
    } catch (err) {
      console.error('Registration error:', err);
      // generic error to avoid info leak
      res.status(500).json({ error: 'Internal server error' });
    }
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});