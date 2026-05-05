const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcryptjs = require('bcryptjs');
const { check, sanitizeBody } = require('express-validator');
const rateLimit = require('express-rate-limit');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

// Database connection configuration using environment variables
const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
});

// Rate limiting for login attempts (5 attempts per minute)
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 5
});
app.post('/login', limiter);

// Validate and sanitize input
app.post("/login", [
  check('username').notEmpty().withMessage('Username is required'),
  check('password').isLength({ min: 8 }).withMessage('Password must be at least 8 characters'),
  sanitizeBody('*').escape()
], (req, res) => {
  try {
    const username = req.body.username;
    const password = req.body.password;

    // Validate request
    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required' });
    }

    // Use prepared statements to prevent SQL injection
    const query = 'SELECT * FROM users WHERE username = ?';
    
    db.query(query, [username], async (err, results) => {
      if (err) {
        console.error('Database error:', err);
        return res.status(500).json({ error: 'Internal server error' });
      }

      if (results.length === 0) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      const user = results[0];
      
      // Verify password using bcrypt
      const isValidPassword = await bcryptjs.compare(password, user.password_hash);
      if (!isValidPassword) {
        return res.status(401).json({ error: 'Invalid credentials' });
      }

      // Secure session management
      req.session.user = {
        id: user.id,
        username: user.username,
        roles: user.roles || []
      };
      
      // Set secure cookie options
      if (req.session) {
        req.session.cookie.secure = true;
        req.session.cookie.httpOnly = true;
      }

      res.json({ success: true, message: 'Logged in successfully' });
    });

  } catch (error) {
    console.error('Error during login:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

