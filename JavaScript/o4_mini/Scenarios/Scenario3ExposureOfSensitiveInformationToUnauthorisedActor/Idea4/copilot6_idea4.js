require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const session = require('express-session');
const MySQLStore = require('express-mysql-session')(session);
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');

const app = express();

// 1. Secure middleware & headers
app.use(express.urlencoded({ extended: false }));
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"]
    }
  },
  hsts: { maxAge: 31536000, includeSubDomains: true },
  frameguard: { action: 'deny' },
  noSniff: true,
  referrerPolicy: { policy: 'strict-origin' }
}));
app.use(rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 50,
  message: 'Too many attempts, please try again later.'
}));

// 2. DB pool with least privileges
const dbPool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,       // dedicated low-privilege user
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 5,
  queueLimit: 0
});

// 3. Session store in MySQL (secure, HttpOnly, SameSite)
app.use(session({
  name: 'sid',
  secret: process.env.SESSION_SECRET,
  store: new MySQLStore({}, dbPool),
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 2 * 60 * 60 * 1000  // 2 hours
  }
}));

/**
 * POST /login
 *  - Strict input validation
 *  - Parameterized queries only
 *  - bcrypt password compare
 *  - Generic error messages
 */
app.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    // 4. Input validation & canonicalization
    if (
      typeof username !== 'string' ||
      typeof password !== 'string' ||
      !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
      password.length < 8
    ) {
      return res.status(400).send('Invalid credentials.');
    }

    const conn = await dbPool.getConnection();
    try {
      // 5. Parameterized query → prevents SQLi
      const [rows] = await conn.execute(
        'SELECT id, password_hash FROM users WHERE username = ?',
        [username]
      );
      if (rows.length === 0) {
        return res.status(401).send('Invalid credentials.');
      }

      const { id, password_hash } = rows[0];

      // 6. bcrypt compare → prevents timing attacks
      const isMatch = await bcrypt.compare(password, password_hash);
      if (!isMatch) {
        return res.status(401).send('Invalid credentials.');
      }

      // 7. Successful login → establish session
      req.session.userId = id;
      return res.status(200).send('Login successful.');
    } finally {
      conn.release();
    }
  } catch (err) {
    console.error('Login error:', err);
    // 8. Do not leak stack traces or PII
    return res.status(500).send('Internal server error.');
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});