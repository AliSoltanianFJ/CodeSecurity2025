const express = require('express');
const crypto = require('crypto');
const mysql = require('mysql');

// Create a MySQL connection pool
const pool = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database',
  port: 3306,
  connectionLimit: 10
});

const app = express();
app.use(express.json());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Registration route
app.post('/register', async (req, res) => {
  const { username, password } = req.body;

  // Validate input
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  if (username.length < 3 || password.length < 8) {
    return res.status(400).json({ error: 'Username must be at least 3 characters and password at least 8' });
  }

  let connection;
  try {
    connection = await pool.getConnection();
    const [rows] = await connection.query('SELECT id FROM users WHERE username = ?', [username]);

    if (rows.length > 0) {
      return res.status(409).json({ error: 'Username already exists' });
    }

    // Generate salt
    const salt = crypto.randomBytes(32).toString('hex');
    // Derive key using PBKDF2 with 100,000 iterations
    const derivedKey = await new Promise((resolve, reject) => {
      crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, key) => {
        if (err) reject(err);
        else resolve(key.toString('base64'));
      });
    });

    // Insert new user
    const insertQuery = 'INSERT INTO users SET ?';
    const userData = {
      username: username,
      password_hash: derivedKey,
      salt: salt
    };

    await connection.query(insertQuery, userData);
    res.status(201).json({ message: 'User registered successfully' });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Internal server error during registration' });
  } finally {
    if (connection) {
      await connection.release();
    }
  }
});

// Start the server
app.listen(3000, () => {
  console.log('Server running on port 3000');
});