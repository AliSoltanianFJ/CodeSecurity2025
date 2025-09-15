const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');
const session = require('express-session');
const rateLimit = require('express-rate-limit');

// Initialize app and middleware
const app = express();

app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// Rate limiting to prevent brute force attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Session configuration
app.use(session({
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: true,
  cookie: {
    secure: true, // Only transmit over HTTPS in production
    httpOnly: true,
    sameSite: 'Lax'
  },
  store: new mysql.PoolStore({
    pool: require('../db') // Database connection pool configuration
  })
}));

// Body-parser for JSON request bodies
app.use(express.json());

// Initialize database connection pool
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  multipleStatements: true
});

// Hash a plaintext password securely using bcrypt
async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('utf8');
  const hashedPassword = await new Promise((resolve, reject) => {
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
      if (err) reject(err);
      resolve(salt + ':' + derivedKey.toString('hex'));
    });
  });
  return hashedPassword;
}

// Verify password against stored hash
async function checkPassword(plaintext, storedHash) {
  const [salt, stored] = storedHash.split(':');
  const plaintextDerived = await new Promise((resolve, reject) => {
    crypto.pbkdf2(plaintext, salt, 100000, 64, 'sha512', (err, derivedKey) => {
      if (err) reject(err);
      resolve(derivedKey.toString('hex'));
    });
  });
  return stored === plaintextDerived;
}

// Authentication middleware
async function authenticateUser(req, res, next) {
  const username = req.body.username;
  const password = req.body.password;

  try {
    // Verify user existence and get password hash
    const [rows] = await db.promise().query(
      'SELECT id, password_hash FROM users WHERE username = ?',
      [username]
    );

    if (rows.length === 0) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    const user = rows[0];
    const isMatch = await checkPassword(password, user.password_hash);

    if (!isMatch) {
      return res.status(401).json({ message: 'Invalid password' });
    }

    // Create session
    req.session.user = { id: user.id, username: username };
    req.session.cookie.maxAge = 3600000; // Session timeout in milliseconds

    next();
  } catch (err) {
    console.error('Authentication error:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
}

// Login route
app.post('/login', authenticateUser, (req, res) => {
  res.json({
    success: true,
    message: 'Login successful',
    user: {
      id: req.session.user.id,
      username: req.session.user.username
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
