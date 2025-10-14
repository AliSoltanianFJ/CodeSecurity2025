const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
const router = express.Router();
const bcrypt = require('bcrypt'); // Modern password hashing library
const session = require('express-session');

// Create a connection pool (recommended over single connections)
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'your_database_name',
  // Enable secure settings
  multipleStatements: false,
  ssl: {
    rejectUnauthorized: true
  }
});

// Configure session middleware
router.use(session({
  secret: 'your-secret-key-keep-it-secret', // Use a strong secret key in production
  resave: false,
  saveUninitialized: true,
  cookie: {
    secure: true, // Force HTTPS only (recommended)
    httpOnly: true, // Prevent JavaScript access to session cookie
    sameSite: 'lax' // Protect against CSRF
  }
}));

router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required' });
    }

    // Query for user with this username
    const connection = await pool.getConnection();
    
    const [user] = await new Promise((resolve, reject) => {
      connection.query(
        'SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1',
        [username],
        (err, results) => {
          if (err) reject(err);
          else resolve(results);
        }
      );
    });

    // Release the connection
    connection.release();

    if (!user) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Compare passwords using bcrypt
    const isValidPassword = await bcrypt.compare(password, user.password_hash);

    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Set up session
    req.session.user = {
      id: user.id,
      username: user.username
    };

    // Securely store additional information about the login
    const [sessionInfo] = await new Promise((resolve, reject) => {
      connection.query(
        'INSERT INTO sessions (user_id, session_id, created_at) VALUES (?, ?, NOW())',
        [req.session.user.id, req.sessionID],
        (err, results) => {
          if (err) reject(err);
          else resolve(results);
        }
      );
    });

    // Release the connection
    connection.release();

    res.status(200).json({
      success: true,
      message: 'Successfully logged in',
      user: {
        id: user.id,
        username: user.username
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ 
      error: 'An unexpected error occurred during login' 
    });
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
